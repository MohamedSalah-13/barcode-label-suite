package com.mohamed.barcodelabel.demo;

import com.mohamed.barcodelabel.data.MissingFieldPolicy;
import com.mohamed.barcodelabel.data.TemplateFieldResolver;
import com.mohamed.barcodelabel.fx.LabelDesigner;
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

public final class DemoApplication extends Application {
    @Override
    public void start(Stage stage) {
        LabelDesigner designer = new LabelDesigner();
        designer.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        designer.setTemplate(createResolvedTemplate());

        Scene scene = new Scene(designer, 1000, 700);
        stage.setTitle("مصمم ملصقات الباركود - نسخة تجريبية");
        stage.setScene(scene);
        stage.show();
    }

    private LabelTemplate createResolvedTemplate() {
        var layout = new SheetLayout(3, 7, new DimensionsMm(63.5, 38.1), 2.5, 0);
        var page = PageDefinition.a4Sheet(layout, new InsetsMm(15.15, 7.25, 15.15, 7.25));

        var border = new ShapeElement(
                ElementFrame.create("Border", new RectMm(1, 1, 61.5, 36.1), 0),
                ShapeType.ROUNDED_RECTANGLE, "#cbd5e1", "transparent", 0.25);
        var name = new TextElement(
                ElementFrame.create("Product", new RectMm(4, 4, 55, 7), 1),
                "{{product_name}}", "Arial", 14, "#111827", true, false, TextAlignment.CENTER);
        var barcode = new BarcodeElement(
                ElementFrame.create("Barcode", new RectMm(6, 14, 51.5, 17), 2),
                "{{sku}}", Symbology.CODE_128, 0.25, 2.5, true);
        var price = new TextElement(
                ElementFrame.create("Price", new RectMm(17, 34, 30, 3), 3),
                "السعر: {{price}} ج.م", "Arial", 10, "#b91c1c", true, false, TextAlignment.CENTER);

        LabelTemplate template = LabelTemplate.create("ملصق منتج", page, List.of(border, name, barcode, price));
        return new TemplateFieldResolver().resolve(template,
                Map.of("product_name", "قهوة عربية", "sku", "6221234567890", "price", "125.50"),
                MissingFieldPolicy.FAIL);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

