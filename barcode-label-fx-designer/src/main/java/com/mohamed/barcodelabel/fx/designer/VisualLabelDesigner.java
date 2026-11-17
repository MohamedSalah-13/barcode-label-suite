package com.mohamed.barcodelabel.fx.designer;

import com.mohamed.barcodelabel.fx.i18n.UiLanguage;
import com.mohamed.barcodelabel.model.LabelTemplate;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Public embeddable designer control. It wraps the editor pane and coordinates
 * focus so resize handles and keyboard shortcuts can work together.
 */
public final class VisualLabelDesigner extends BorderPane {
    private final LabelDesignerPane delegate;

    /** An Arabic designer for a new, empty A4 label. */
    public VisualLabelDesigner() {
        this(new LabelDesignerPane());
    }

    /** An Arabic designer. */
    public VisualLabelDesigner(LabelTemplate template) {
        this(new LabelDesignerPane(template));
    }

    /** A designer in this language: Arabic right to left, English left to right. */
    public VisualLabelDesigner(LabelTemplate template, UiLanguage language) {
        this(new LabelDesignerPane(template, language));
    }

    private VisualLabelDesigner(LabelDesignerPane delegate) {
        this.delegate = delegate;
        setCenter(delegate);
        // After a press the selected element takes the focus back, so arrow keys and shortcuts keep moving it. Not
        // after a press in a field one types into: that would take the focus from the field before the first key.
        delegate.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (isTypedInto(event.getPickResult().getIntersectedNode())) return;
            Platform.runLater(() -> {
                Node selected = delegate.lookup(".selected-element");
                if (selected != null) selected.requestFocus();
            });
        });
    }

    /** Whether {@code node} is, or is inside, a text field or a combo box, colour picker or other editable box. */
    static boolean isTypedInto(Node node) {
        for (Node current = node; current != null; current = current.getParent()) {
            if (current instanceof TextInputControl || current instanceof ComboBoxBase<?>) return true;
        }
        return false;
    }

    public LabelTemplate getTemplate() { return delegate.getTemplate(); }
    public void setTemplate(LabelTemplate value) { delegate.setTemplate(value); }
    public ReadOnlyObjectProperty<LabelTemplate> templateProperty() { return delegate.templateProperty(); }
    public void save(Path path) throws IOException { delegate.save(path); }
    public LabelDesignerPane editorPane() { return delegate; }
    public UiLanguage language() { return delegate.language(); }
}

