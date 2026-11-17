package com.mohamed.barcodelabel.fx.designer;

import com.mohamed.barcodelabel.fx.i18n.UiLanguage;
import com.mohamed.barcodelabel.model.LabelTemplate;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.Node;
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
        delegate.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> Platform.runLater(() -> {
            Node selected = delegate.lookup(".selected-element");
            if (selected != null) selected.requestFocus();
        }));
    }

    public LabelTemplate getTemplate() { return delegate.getTemplate(); }
    public void setTemplate(LabelTemplate value) { delegate.setTemplate(value); }
    public ReadOnlyObjectProperty<LabelTemplate> templateProperty() { return delegate.templateProperty(); }
    public void save(Path path) throws IOException { delegate.save(path); }
    public LabelDesignerPane editorPane() { return delegate; }
    public UiLanguage language() { return delegate.language(); }
}

