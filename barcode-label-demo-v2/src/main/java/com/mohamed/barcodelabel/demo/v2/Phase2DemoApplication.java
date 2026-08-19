package com.mohamed.barcodelabel.demo.v2;

import com.mohamed.barcodelabel.fx.preview.HighQualityLabelPreview;
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
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

public final class Phase2DemoApplication extends Application {
    @Override
    public void start(Stage stage) {
        HighQualityLabelPreview preview = new HighQualityLabelPreview();
        preview.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        preview.setTemplate(createTemplate());
        preview.setData(Map.of(
                "product_name", "قهوة عربية فاخرة",
                "sku", "6221234567890",
                "price", "125.50"
        ));

        Slider dpi = new Slider(96, 300, 150);
        dpi.setShowTickLabels(true);
        dpi.setShowTickMarks(true);
        dpi.valueProperty().addListener((observable, oldValue, newValue) -> preview.setPreviewDpi(newValue.doubleValue()));
        HBox toolbar = new HBox(12, new Label("دقة المعاينة DPI:"), dpi);
        toolbar.setStyle("-fx-padding: 12; -fx-alignment: center-right; -fx-background-color: white;");

        BorderPane root = new BorderPane(preview);
        root.setTop(toolbar);
        root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        stage.setScene(new Scene(root, 1050, 760));
        stage.setTitle("Barcode Label Suite - معاينة حقيقية للباركود");
        stage.show();
    }

    private static LabelTemplate createTemplate() {
        var layout = new SheetLayout(3, 7, new DimensionsMm(63.5, 38.1), 2.5, 0);
        var page = PageDefinition.a4Sheet(layout, new InsetsMm(15.15, 7.25, 15.15, 7.25));
        var border = new ShapeElement(
                ElementFrame.create("Border", new RectMm(1, 1, 61.5, 36.1), 0),
                ShapeType.ROUNDED_RECTANGLE, "#CBD5E1", "transparent", 0.25);
        var name = new TextElement(
                ElementFrame.create("Product", new RectMm(4, 3, 55, 7), 1),
                "{{product_name}}", "Arial", 12, "#111827", true, false, TextAlignment.CENTER);
        var barcode = new BarcodeElement(
                ElementFrame.create("Barcode", new RectMm(4, 11, 55.5, 19), 2),
                "{{sku}}", Symbology.CODE_128, 0.25, 2.5, true);
        var price = new TextElement(
                ElementFrame.create("Price", new RectMm(10, 31, 43.5, 5), 3),
                "السعر: {{price}} ج.م", "Arial", 10, "#B91C1C", true, false, TextAlignment.CENTER);
        return LabelTemplate.create("ملصق منتج", page, List.of(border, name, barcode, price));
    }

    public static void main(String[] args) {
        launch(args);
    }
}

