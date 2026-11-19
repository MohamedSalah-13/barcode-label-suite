package com.mohamed.barcodelabel.fx.workbench;

import javafx.geometry.NodeOrientation;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

final class IconButtons {
    private IconButtons() {}

    static Button create(Feather glyph, String label) {
        FontIcon icon = new FontIcon(glyph);
        icon.setIconSize(18);
        icon.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        Button button = new Button(label, icon);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setMinSize(36, 34);
        button.setPrefSize(36, 34);
        button.setTooltip(new Tooltip(label));
        button.setAccessibleText(label);
        return button;
    }
}
