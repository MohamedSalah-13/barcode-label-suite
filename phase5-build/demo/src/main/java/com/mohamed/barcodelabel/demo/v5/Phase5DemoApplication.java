package com.mohamed.barcodelabel.demo.v5;

import com.mohamed.barcodelabel.fx.workbench.BarcodeLabelWorkbench;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.*;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.workflow.paper.PaperPresetRegistry;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;

public final class Phase5DemoApplication extends Application {
    @Override public void start(Stage stage) {
        BarcodeLabelWorkbench workbench = new BarcodeLabelWorkbench(defaultTemplate());
        stage.setScene(new Scene(workbench, 1450, 920));
        stage.setTitle("Barcode Label Suite — Workbench 0.5");
        stage.setMinWidth(1100); stage.setMinHeight(720); stage.show();
    }

    private static LabelTemplate defaultTemplate() {
        var page = new PaperPresetRegistry().find("a4-3x7-63.5x38.1").orElseThrow().page();
        var border = new ShapeElement(ElementFrame.create("Border", new RectMm(1, 1, 61.5, 36.1), 0), ShapeType.ROUNDED_RECTANGLE, "#CBD5E1", "transparent", 0.25);
        var name = new TextElement(ElementFrame.create("Product", new RectMm(4, 3, 55, 7), 1), "{{product_name}}", "Arial", 11, "#111827", true, false, TextAlignment.CENTER);
        var barcode = new BarcodeElement(ElementFrame.create("Barcode", new RectMm(4, 11, 55.5, 19), 2), "{{sku}}", Symbology.CODE_128, 0.25, 2.5, true);
        var price = new TextElement(ElementFrame.create("Price", new RectMm(10, 31, 43.5, 5), 3), "السعر: {{price}} ج.م", "Arial", 9, "#B91C1C", true, false, TextAlignment.CENTER);
        return LabelTemplate.create("قالب المنتجات", page, List.of(border, name, barcode, price));
    }

    public static void main(String[] args) { launch(args); }
}
