package com.mohamed.barcodelabel.fx.designer.model;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.LabelElement;
import com.mohamed.barcodelabel.model.geometry.RectMm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public final class TemplateEditSession {
    private LabelTemplate template;
    private final LinkedHashSet<UUID> selection = new LinkedHashSet<>();
    private final Deque<HistoryEntry> undo = new ArrayDeque<>();
    private final Deque<HistoryEntry> redo = new ArrayDeque<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public TemplateEditSession(LabelTemplate template) {
        this.template = Objects.requireNonNull(template, "template");
    }

    public LabelTemplate template() { return template; }
    public Set<UUID> selection() { return Set.copyOf(selection); }
    public UUID primarySelection() { return selection.isEmpty() ? null : selection.getFirst(); }
    public boolean canUndo() { return !undo.isEmpty(); }
    public boolean canRedo() { return !redo.isEmpty(); }
    public String undoDescription() { return canUndo() ? undo.peek().description() : ""; }
    public String redoDescription() { return canRedo() ? redo.peek().description() : ""; }

    public void addListener(Runnable listener) { listeners.add(Objects.requireNonNull(listener)); }
    public void removeListener(Runnable listener) { listeners.remove(listener); }

    public void select(UUID id, boolean additive) {
        if (!additive) selection.clear();
        if (id != null) {
            if (additive && selection.contains(id)) selection.remove(id); else selection.add(id);
        }
        notifyListeners();
    }

    public void clearSelection() { select(null, false); }

    public LabelElement find(UUID id) {
        return template.elements().stream().filter(element -> element.frame().id().equals(id)).findFirst().orElse(null);
    }

    public void addElement(LabelElement element) {
        apply("Add " + element.frame().name(), current -> {
            var elements = new ArrayList<>(current.elements());
            elements.add(element);
            return current.withElements(elements);
        });
        selection.clear();
        selection.add(element.frame().id());
        notifyListeners();
    }

    public void deleteSelected() {
        if (selection.isEmpty()) return;
        Set<UUID> removed = Set.copyOf(selection);
        apply("Delete elements", current -> current.withElements(current.elements().stream()
                .filter(element -> !removed.contains(element.frame().id())).toList()));
        selection.clear();
        notifyListeners();
    }

    public void duplicateSelected(double offsetMm) {
        if (selection.isEmpty()) return;
        int nextZ = template.elements().stream().mapToInt(element -> element.frame().zIndex()).max().orElse(0) + 1;
        var duplicates = new ArrayList<LabelElement>();
        for (LabelElement element : template.elements()) {
            if (!selection.contains(element.frame().id())) continue;
            RectMm old = element.frame().bounds();
            RectMm moved = fit(new RectMm(old.x() + offsetMm, old.y() + offsetMm, old.width(), old.height()));
            duplicates.add(ElementCopies.duplicate(element, moved, nextZ++));
        }
        apply("Duplicate elements", current -> {
            var elements = new ArrayList<>(current.elements());
            elements.addAll(duplicates);
            return current.withElements(elements);
        });
        selection.clear();
        duplicates.forEach(element -> selection.add(element.frame().id()));
        notifyListeners();
    }

    public void moveSelected(double deltaX, double deltaY) {
        if (selection.isEmpty() || (deltaX == 0 && deltaY == 0)) return;
        updateSelected("Move elements", element -> {
            if (element.frame().locked()) return element;
            RectMm old = element.frame().bounds();
            return ElementCopies.withBounds(element, fit(new RectMm(old.x() + deltaX, old.y() + deltaY,
                    old.width(), old.height())));
        });
    }

    public void resize(UUID id, RectMm bounds) {
        updateOne(id, "Resize element", element -> element.frame().locked()
                ? element : ElementCopies.withBounds(element, fit(bounds)));
    }

    public void setBounds(UUID id, RectMm bounds) {
        updateOne(id, "Change bounds", element -> ElementCopies.withBounds(element, fit(bounds)));
    }

    public void setContent(UUID id, String content) {
        updateOne(id, "Change content", element -> ElementCopies.withContent(element, content));
    }

    public void setLockedVisible(UUID id, boolean locked, boolean visible) {
        updateOne(id, "Change element state", element -> ElementCopies.withLockedVisible(element, locked, visible));
    }

    public void align(EditorAlignment alignment) {
        if (selection.size() < 2) return;
        List<LabelElement> selected = template.elements().stream()
                .filter(element -> selection.contains(element.frame().id())).toList();
        double minX = selected.stream().mapToDouble(e -> e.frame().bounds().x()).min().orElse(0);
        double maxX = selected.stream().mapToDouble(e -> e.frame().bounds().x() + e.frame().bounds().width()).max().orElse(0);
        double minY = selected.stream().mapToDouble(e -> e.frame().bounds().y()).min().orElse(0);
        double maxY = selected.stream().mapToDouble(e -> e.frame().bounds().y() + e.frame().bounds().height()).max().orElse(0);
        updateSelected("Align " + alignment, element -> {
            RectMm b = element.frame().bounds();
            double x = switch (alignment) {
                case LEFT -> minX;
                case HORIZONTAL_CENTER -> (minX + maxX - b.width()) / 2;
                case RIGHT -> maxX - b.width();
                default -> b.x();
            };
            double y = switch (alignment) {
                case TOP -> minY;
                case VERTICAL_CENTER -> (minY + maxY - b.height()) / 2;
                case BOTTOM -> maxY - b.height();
                default -> b.y();
            };
            return ElementCopies.withBounds(element, fit(new RectMm(x, y, b.width(), b.height())));
        });
    }

    public void undo() {
        if (undo.isEmpty()) return;
        HistoryEntry entry = undo.pop();
        redo.push(entry);
        template = entry.before();
        pruneSelection();
        notifyListeners();
    }

    public void redo() {
        if (redo.isEmpty()) return;
        HistoryEntry entry = redo.pop();
        undo.push(entry);
        template = entry.after();
        pruneSelection();
        notifyListeners();
    }

    private void updateSelected(String description, UnaryOperator<LabelElement> update) {
        Set<UUID> ids = Set.copyOf(selection);
        apply(description, current -> current.withElements(current.elements().stream()
                .map(element -> ids.contains(element.frame().id()) ? update.apply(element) : element).toList()));
    }

    private void updateOne(UUID id, String description, UnaryOperator<LabelElement> update) {
        if (id == null) return;
        apply(description, current -> current.withElements(current.elements().stream()
                .map(element -> element.frame().id().equals(id) ? update.apply(element) : element).toList()));
    }

    private void apply(String description, UnaryOperator<LabelTemplate> operation) {
        LabelTemplate before = template;
        LabelTemplate after = Objects.requireNonNull(operation.apply(before));
        if (before.equals(after)) return;
        template = after;
        undo.push(new HistoryEntry(description, before, after));
        redo.clear();
        notifyListeners();
    }

    private RectMm fit(RectMm requested) {
        var size = template.page().labelSize();
        double width = Math.min(requested.width(), size.width());
        double height = Math.min(requested.height(), size.height());
        double x = Math.max(0, Math.min(requested.x(), size.width() - width));
        double y = Math.max(0, Math.min(requested.y(), size.height() - height));
        return new RectMm(x, y, width, height);
    }

    private void pruneSelection() {
        Set<UUID> ids = template.elements().stream().map(element -> element.frame().id()).collect(Collectors.toSet());
        selection.retainAll(ids);
    }

    private void notifyListeners() { listeners.forEach(Runnable::run); }

    private record HistoryEntry(String description, LabelTemplate before, LabelTemplate after) {}
}

