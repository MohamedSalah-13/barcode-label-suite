package com.mohamed.barcodelabel.demo.v3;

import com.mohamed.barcodelabel.fx.print.SheetPrintPane;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.ShapeElement;
import com.mohamed.barcodelabel.model.element.ShapeType;
import com.mohamed.barcodelabel.model.element.Symbology;
import com.mohamed.barcodelabel.model.element.TextAlignment;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.SheetLayout;
import javafx.application.Application;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public final class Phase3DemoApplication extends Application {
    @Override
    public void start(Stage stage) {
        SheetPrintPane printPane = new SheetPrintPane();
        printPane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        printPane.setTemplate(createTemplate());
        printPane.setRecords(createProducts(32));
        stage.setScene(new Scene(printPane, 1280, 850));
        stage.setTitle("Barcode Label Suite - معاينة وطباعة ورق A4");
        stage.show();
    }

    private static LabelTemplate createTemplate() {
        var layout = new SheetLayout(3, 7, new DimensionsMm(63.5, 38.1), 2.5, 0);
        var page = PageDefinition.a4Sheet(layout, new InsetsMm(15.15, 7.25, 15.15, 7.25));
        var border = new ShapeElement(ElementFrame.create("Border", new RectMm(1, 1, 61.5, 36.1), 0),
                ShapeType.ROUNDED_RECTANGLE, "#CBD5E1", "transparent", 0.25);
        var name = new TextElement(ElementFrame.create("Product", new RectMm(4, 3, 55, 7), 1),
                "{{product_name}}", "Arial", 11, "#111827", true, false, TextAlignment.CENTER);
        var barcode = new BarcodeElement(ElementFrame.create("Barcode", new RectMm(4, 11, 55.5, 19), 2),
                "{{sku}}", Symbology.CODE_128, 0.25, 2.5, true);
        var price = new TextElement(ElementFrame.create("Price", new RectMm(10, 31, 43.5, 5), 3),
                "السعر: {{price}} ج.م", "Arial", 9, "#B91C1C", true, false, TextAlignment.CENTER);
        return LabelTemplate.create("ورقة ملصقات المنتجات", page, List.of(border, name, barcode, price));
    }

    private static List<Map<String, ?>> createProducts(int count) {
        return IntStream.rangeClosed(1, count)
                .<Map<String, ?>>mapToObj(index -> Map.of(
                        "product_name", "منتج رقم " + index,
                        "sku", String.format("SKU-%06d", index),
                        "price", String.format("%.2f", 15.0 + index * 2.75)))
                .toList();
    }

    public static void main(String[] args) { launch(args); }
}

