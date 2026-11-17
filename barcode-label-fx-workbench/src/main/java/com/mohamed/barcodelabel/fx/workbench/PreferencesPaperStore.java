package com.mohamed.barcodelabel.fx.workbench;

import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.PageMode;
import com.mohamed.barcodelabel.model.page.PageOrientation;
import com.mohamed.barcodelabel.model.page.SheetLayout;
import com.mohamed.barcodelabel.workflow.paper.PaperPreset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/** Saved paper sizes in a preferences node: child node "0", "1", ... per size, each value under its own key. */
final class PreferencesPaperStore implements CustomPaperStore {
    private final Preferences node;

    PreferencesPaperStore(Preferences node) {
        this.node = node;
    }

    @Override public List<PaperPreset> load() {
        try {
            List<PaperPreset> presets = new ArrayList<>();
            String[] children = node.childrenNames();
            Arrays.sort(children, Comparator.comparingInt(PreferencesPaperStore::index));
            for (String child : children) {
                try {
                    presets.add(read(node.node(child)));
                } catch (RuntimeException unreadable) {
                    // A size saved by hand or by another version that no longer makes a page is left out.
                }
            }
            return List.copyOf(presets);
        } catch (BackingStoreException | IllegalStateException unavailable) {
            return List.of();
        }
    }

    @Override public void save(List<PaperPreset> presets) {
        try {
            for (String child : node.childrenNames()) node.node(child).removeNode();
            for (int i = 0; i < presets.size(); i++) write(node.node(Integer.toString(i)), presets.get(i));
            node.flush();
        } catch (BackingStoreException ignored) {
            // The sizes stay in the list until the application closes; they are only not remembered.
        }
    }

    private static void write(Preferences entry, PaperPreset preset) {
        PageDefinition page = preset.page();
        entry.put("id", preset.id());
        entry.put("name", preset.displayName());
        entry.put("category", preset.category());
        entry.put("mode", page.mode().name());
        entry.put("orientation", page.orientation().name());
        entry.putDouble("width", page.pageSize().width());
        entry.putDouble("height", page.pageSize().height());
        entry.putDouble("top", page.margins().top());
        entry.putDouble("right", page.margins().right());
        entry.putDouble("bottom", page.margins().bottom());
        entry.putDouble("left", page.margins().left());
        SheetLayout layout = page.sheetLayout();
        if (layout != null) {
            entry.putInt("columns", layout.columns());
            entry.putInt("rows", layout.rows());
            entry.putDouble("labelWidth", layout.labelSize().width());
            entry.putDouble("labelHeight", layout.labelSize().height());
            entry.putDouble("horizontalGap", layout.horizontalGap());
            entry.putDouble("verticalGap", layout.verticalGap());
        }
    }

    private static PaperPreset read(Preferences entry) {
        PageMode mode = PageMode.valueOf(required(entry, "mode"));
        SheetLayout layout = entry.get("columns", null) == null ? null : new SheetLayout(
                entry.getInt("columns", 0), entry.getInt("rows", 0),
                new DimensionsMm(number(entry, "labelWidth"), number(entry, "labelHeight")),
                number(entry, "horizontalGap"), number(entry, "verticalGap"));
        PageDefinition page = new PageDefinition(mode,
                new DimensionsMm(number(entry, "width"), number(entry, "height")),
                new InsetsMm(number(entry, "top"), number(entry, "right"), number(entry, "bottom"), number(entry, "left")),
                PageOrientation.valueOf(entry.get("orientation", PageOrientation.PORTRAIT.name())),
                mode == PageMode.SHEET ? layout : null);
        return new PaperPreset(required(entry, "id"), required(entry, "name"),
                entry.get("category", CustomPaperDialog.CATEGORY), page);
    }

    private static String required(Preferences entry, String key) {
        String value = entry.get(key, null);
        if (value == null) throw new IllegalArgumentException("missing " + key);
        return value;
    }

    private static double number(Preferences entry, String key) {
        return Double.parseDouble(required(entry, key));
    }

    private static int index(String child) {
        try {
            return Integer.parseInt(child);
        } catch (NumberFormatException exception) {
            return Integer.MAX_VALUE;
        }
    }
}
