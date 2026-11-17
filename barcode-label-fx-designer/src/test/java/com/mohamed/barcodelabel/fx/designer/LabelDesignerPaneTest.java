package com.mohamed.barcodelabel.fx.designer;

import com.mohamed.barcodelabel.data.TemplateJsonService;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.ImageElement;
import com.mohamed.barcodelabel.model.element.ImageFit;
import com.mohamed.barcodelabel.model.element.LabelElement;
import com.mohamed.barcodelabel.model.element.ShapeElement;
import com.mohamed.barcodelabel.model.element.ShapeType;
import com.mohamed.barcodelabel.model.element.Symbology;
import com.mohamed.barcodelabel.model.element.TextAlignment;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static com.mohamed.barcodelabel.fx.designer.FxTestSupport.mouse;
import static com.mohamed.barcodelabel.fx.designer.FxTestSupport.onFx;
import static com.mohamed.barcodelabel.fx.designer.FxTestSupport.runFx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabelDesignerPaneTest {
    private static final double LABEL_WIDTH = FxTestSupport.LABEL_WIDTH;
    private static final double LABEL_HEIGHT = FxTestSupport.LABEL_HEIGHT;

    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        FxTestSupport.startToolkit();
    }

    @Test
    void defaultConstructorStartsWithAnEmptyA4SheetLabel() {
        LabelDesignerPane pane = onFx(LabelDesignerPane::new);
        assertEquals("New label", pane.getTemplate().name());
        assertTrue(pane.getTemplate().elements().isEmpty());
        assertEquals(LABEL_WIDTH, pane.getTemplate().page().labelSize().width(), 0.001);
    }

    @Test
    void toolboxAddsEachElementTypeAndSelectsIt() {
        LabelDesignerPane pane = pane();
        runFx(() -> {
            button(pane, "+ نص").fire();
            assertInstanceOf(TextElement.class, last(pane));
            button(pane, "+ باركود").fire();
            BarcodeElement barcode = assertInstanceOf(BarcodeElement.class, last(pane));
            assertEquals(Symbology.CODE_128, barcode.symbology());
            button(pane, "+ QR Code").fire();
            BarcodeElement qr = assertInstanceOf(BarcodeElement.class, last(pane));
            assertEquals(Symbology.QR_CODE, qr.symbology());
            assertFalse(qr.showHumanReadableText());
            button(pane, "+ مستطيل").fire();
            ShapeElement rectangle = assertInstanceOf(ShapeElement.class, last(pane));
            assertEquals(ShapeType.ROUNDED_RECTANGLE, rectangle.shapeType());

            assertEquals(6, pane.getTemplate().elements().size());
            assertEquals(5, last(pane).frame().zIndex(), "each new element goes on top");
            assertEquals("1 عنصر محدد", status(pane).getText());
            assertEquals(last(pane).frame().name(), title(pane).getText());
        });
    }

    @Test
    void undoAndRedoButtonsFollowTheHistory() {
        LabelDesignerPane pane = pane();
        runFx(() -> {
            Button undo = button(pane, "تراجع");
            Button redo = button(pane, "إعادة");
            assertTrue(undo.isDisabled());
            assertTrue(redo.isDisabled());

            button(pane, "+ نص").fire();
            assertFalse(undo.isDisabled());
            assertEquals("تراجع: Add Text", undo.getText());

            undo.fire();
            assertEquals(2, pane.getTemplate().elements().size());
            assertEquals("إعادة: Add Text", redo.getText());
            assertEquals("تراجع", undo.getText());

            redo.fire();
            assertEquals(3, pane.getTemplate().elements().size());
            assertTrue(redo.isDisabled());
        });
    }

    @Test
    void toolbarCopiesPastesDuplicatesCutsAndDeletes() {
        LabelDesignerPane pane = pane();
        runFx(() -> {
            button(pane, "+ نص").fire();
            button(pane, "نسخ").fire();
            button(pane, "لصق").fire();
            assertEquals(4, pane.getTemplate().elements().size());
            button(pane, "تكرار").fire();
            assertEquals(5, pane.getTemplate().elements().size());
            button(pane, "قص").fire();
            assertEquals(4, pane.getTemplate().elements().size());
            button(pane, "حذف").fire();
            assertEquals(4, pane.getTemplate().elements().size(), "nothing is selected after a cut");
            button(pane, "+ نص").fire();
            button(pane, "حذف").fire();
            assertEquals(4, pane.getTemplate().elements().size());
        });
    }

    @Test
    void toolbarAlignsASingleElementToTheLabelEdgesAndCentre() {
        LabelDesignerPane pane = pane();
        runFx(() -> {
            button(pane, "+ نص").fire(); // 30 x 7 mm at (5, 5)
            button(pane, "يمين").fire();
            button(pane, "أسفل").fire();
            assertEquals(new RectMm(LABEL_WIDTH - 30, LABEL_HEIGHT - 7, 30, 7), last(pane).frame().bounds());
            button(pane, "يسار").fire();
            button(pane, "أعلى").fire();
            assertEquals(new RectMm(0, 0, 30, 7), last(pane).frame().bounds());
            button(pane, "توسيط أفقي").fire();
            button(pane, "توسيط رأسي").fire();
            RectMm centred = last(pane).frame().bounds();
            assertEquals((LABEL_WIDTH - 30) / 2, centred.x(), 0.0001);
            assertEquals((LABEL_HEIGHT - 7) / 2, centred.y(), 0.0001);
        });
    }

    @Test
    void toolbarChangesTheSelectedElementsLayer() {
        LabelDesignerPane pane = pane();
        runFx(() -> {
            button(pane, "+ نص").fire(); // z 2, above the template's z 0 and 1
            var id = last(pane).frame().id();
            button(pane, "إرسال للخلفية").fire();
            assertEquals(-1, zOf(pane, id));
            button(pane, "طبقة للأمام").fire();
            assertEquals(0, zOf(pane, id));
            button(pane, "إحضار للمقدمة").fire();
            assertEquals(2, zOf(pane, id));
            button(pane, "طبقة للخلف").fire();
            assertEquals(1, zOf(pane, id));
        });
    }

    @Test
    void propertiesPanelIsEmptyAndDisabledWithoutASelection() {
        LabelDesignerPane pane = pane();
        runFx(() -> {
            assertEquals("لا يوجد عنصر محدد", title(pane).getText());
            assertEquals("", field(pane, "X (mm)").getText());
            assertTrue(field(pane, "X (mm)").isDisabled());
            assertTrue(control(pane, "الحجم").isDisabled());
            button(pane, "تطبيق الخصائص").fire();
            assertEquals(2, pane.getTemplate().elements().size());
        });
    }

    @Test
    void propertiesPanelShowsAndAppliesTextProperties() {
        LabelDesignerPane pane = pane();
        runFx(() -> {
            button(pane, "+ نص").fire();
            assertEquals("5.00", field(pane, "X (mm)").getText());
            assertEquals("نص جديد", field(pane, "النص").getText());
            assertTrue(toggle(pane, "توسيط").isSelected());

            field(pane, "X (mm)").setText("10");
            field(pane, "Y (mm)").setText("8");
            field(pane, "العرض").setText("25");
            field(pane, "الارتفاع").setText("6");
            field(pane, "الدوران (°)").setText("15");
            field(pane, "النص").setText("Hello");
            field(pane, "الحجم").setText("14");
            ((ColorPicker) control(pane, "اللون")).setValue(Color.RED);
            toggle(pane, "B").setSelected(true);
            toggle(pane, "I").setSelected(true);
            toggle(pane, "يمين").setSelected(true);
            checkBox(pane, "مقفل").setSelected(true);
            button(pane, "تطبيق الخصائص").fire();

            TextElement text = assertInstanceOf(TextElement.class, last(pane));
            assertEquals(new RectMm(10, 8, 25, 6), text.frame().bounds());
            assertEquals(15, text.frame().rotationDegrees(), 0.0001);
            assertEquals("Hello", text.textTemplate());
            assertEquals(14, text.fontSizePoints(), 0.0001);
            assertEquals("#FF0000", text.color());
            assertTrue(text.bold());
            assertTrue(text.italic());
            assertEquals(TextAlignment.RIGHT, text.alignment());
            assertTrue(text.frame().locked());
            assertEquals("تم تطبيق الخصائص", status(pane).getText());

            toggle(pane, "يسار").setSelected(true);
            button(pane, "تطبيق الخصائص").fire();
            assertEquals(TextAlignment.LEFT, ((TextElement) last(pane)).alignment());
        });
    }

    @Test
    void anInvalidNumberIsReportedAndChangesNothing() {
        LabelDesignerPane pane = pane();
        runFx(() -> {
            button(pane, "+ نص").fire();
            LabelElement before = last(pane);
            field(pane, "X (mm)").setText("abc");
            button(pane, "تطبيق الخصائص").fire();
            assertTrue(status(pane).getText().startsWith("قيمة غير صحيحة"), status(pane).getText());
            assertEquals(before, last(pane));
        });
    }

    @Test
    void barcodeContentIsEditableButTextFormattingIsNot() {
        LabelDesignerPane pane = pane();
        runFx(() -> {
            button(pane, "+ باركود").fire();
            assertEquals("{{barcode}}", field(pane, "بيانات الباركود").getText());
            assertTrue(control(pane, "الحجم").isDisabled());
            assertTrue(control(pane, "نمط الصورة").isDisabled());

            field(pane, "بيانات الباركود").setText("{{sku}}");
            button(pane, "تطبيق الخصائص").fire();
            assertEquals("{{sku}}", ((BarcodeElement) last(pane)).dataTemplate());
        });
    }

    @Test
    void enterInAFieldAppliesItWithoutTheButton() {
        LabelDesignerPane pane = pane();
        runFx(() -> {
            button(pane, "+ باركود").fire();
            TextField data = field(pane, "بيانات الباركود");
            data.setText("6221234567890");
            data.fireEvent(new javafx.event.ActionEvent());
            assertEquals("6221234567890", ((BarcodeElement) last(pane)).dataTemplate());

            TextField x = field(pane, "X (mm)");
            x.setText("12");
            x.fireEvent(new javafx.event.ActionEvent());
            assertEquals(12, last(pane).frame().bounds().x(), 0.0001);
            assertEquals("تم تطبيق الخصائص", status(pane).getText());
        });
    }

    @Test
    void theContentCaptionNamesWhatTheFieldHolds() {
        LabelDesignerPane pane = pane();
        runFx(() -> {
            button(pane, "+ نص").fire();
            assertEquals("نص جديد", field(pane, "النص").getText());
            button(pane, "+ QR Code").fire();
            assertEquals("{{qr_value}}", field(pane, "بيانات الباركود").getText());
            pane.setTemplate(FxTestSupport.template(List.of()));
            assertTrue(field(pane, "المحتوى").isDisabled());
        });
    }

    @Test
    void imageSourceAndFitAreEditable(@TempDir Path directory) {
        String source = directory.resolve("logo.png").toUri().toString();
        var image = new ImageElement(ElementFrame.create("Logo", new RectMm(5, 5, 20, 10), 0), source, ImageFit.COVER);
        LabelDesignerPane pane = onFx(() -> new LabelDesignerPane(FxTestSupport.template(List.of(image))));
        runFx(() -> {
            mouse(elementView(pane), MouseEvent.MOUSE_PRESSED, 10, 10, false);
            assertEquals("Logo", title(pane).getText());
            assertEquals(source, field(pane, "مسار الصورة").getText());
            @SuppressWarnings("unchecked")
            ComboBox<ImageFit> fit = (ComboBox<ImageFit>) control(pane, "نمط الصورة");
            assertFalse(fit.isDisabled());
            assertEquals(ImageFit.COVER, fit.getValue());

            fit.setValue(ImageFit.STRETCH);
            button(pane, "تطبيق الخصائص").fire();
            assertEquals(ImageFit.STRETCH, ((ImageElement) last(pane)).fit());
        });
    }

    @Test
    void zoomSliderAndSnapCheckboxDriveTheCanvas() {
        LabelDesignerPane pane = pane();
        runFx(() -> {
            Slider zoom = (Slider) all(pane).filter(Slider.class::isInstance).findFirst().orElseThrow();
            zoom.setValue(2);
            assertEquals(LABEL_WIDTH * 4 * 2, canvas(pane).getPrefWidth(), 0.001);
            checkBox(pane, "التصاق بالشبكة").setSelected(false);
        });
    }

    @Test
    void saveWritesJsonThatReadsBackToTheSameTemplate(@TempDir Path directory) throws Exception {
        LabelDesignerPane pane = pane();
        Path file = directory.resolve("label.json");
        runFx(() -> {
            try {
                pane.save(file);
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
            assertEquals("تم حفظ القالب: label.json", status(pane).getText());
        });
        assertEquals(pane.getTemplate(), new TemplateJsonService().read(file));
    }

    @Test
    void visualLabelDesignerDelegatesToItsEditorPane(@TempDir Path directory) throws Exception {
        LabelTemplate initial = FxTestSupport.twoTextTemplate();
        VisualLabelDesigner designer = onFx(() -> new VisualLabelDesigner(initial));
        assertEquals(initial, designer.getTemplate());
        assertSame(designer.editorPane(), designer.getCenter());

        LabelTemplate replacement = FxTestSupport.template(List.of());
        runFx(() -> designer.setTemplate(replacement));
        assertEquals(replacement, designer.templateProperty().get());

        Path file = directory.resolve("designer.json");
        runFx(() -> {
            try {
                designer.save(file);
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        });
        assertEquals(replacement, new TemplateJsonService().read(file));

        VisualLabelDesigner empty = onFx(VisualLabelDesigner::new);
        assertTrue(empty.getTemplate().elements().isEmpty());
        runFx(() -> mouse(empty.editorPane(), MouseEvent.MOUSE_PRESSED, 1, 1, false));
    }

    private static LabelDesignerPane pane() {
        return onFx(() -> new LabelDesignerPane(FxTestSupport.twoTextTemplate()));
    }

    private static LabelElement last(LabelDesignerPane pane) {
        return pane.getTemplate().elements().getLast();
    }

    private static int zOf(LabelDesignerPane pane, java.util.UUID id) {
        return pane.getTemplate().elements().stream()
                .filter(element -> element.frame().id().equals(id))
                .findFirst().orElseThrow().frame().zIndex();
    }

    /** Every node under {@code root}, including tool bar items, which are not children until a skin exists. */
    private static Stream<Node> all(Node root) {
        Stream<Node> children = Stream.empty();
        if (root instanceof ToolBar toolBar) children = toolBar.getItems().stream();
        else if (root instanceof ScrollPane scroll && scroll.getContent() != null) children = Stream.of(scroll.getContent());
        else if (root instanceof Parent parent) children = parent.getChildrenUnmodifiable().stream();
        return Stream.concat(Stream.of(root), children.flatMap(LabelDesignerPaneTest::all));
    }

    private static Button button(LabelDesignerPane pane, String text) {
        return all(pane).filter(node -> node instanceof Button button
                        && (button.getText().equals(text) || button.getText().startsWith(text + ":")))
                .map(Button.class::cast).findFirst()
                .orElseThrow(() -> new AssertionError("no button " + text));
    }

    private static ToggleButton toggle(LabelDesignerPane pane, String text) {
        return all(pane.getRight()).filter(node -> node instanceof ToggleButton toggle && toggle.getText().equals(text))
                .map(ToggleButton.class::cast).findFirst()
                .orElseThrow(() -> new AssertionError("no toggle " + text));
    }

    private static CheckBox checkBox(LabelDesignerPane pane, String text) {
        return all(pane).filter(node -> node instanceof CheckBox box && box.getText().equals(text))
                .map(CheckBox.class::cast).findFirst()
                .orElseThrow(() -> new AssertionError("no check box " + text));
    }

    /** The control in the properties grid row labelled {@code label}. */
    private static Node control(LabelDesignerPane pane, String label) {
        Label caption = all(pane.getRight())
                .filter(node -> node instanceof Label l && l.getText().equals(label))
                .map(Label.class::cast).findFirst()
                .orElseThrow(() -> new AssertionError("no property " + label));
        GridPane grid = (GridPane) caption.getParent();
        Integer row = GridPane.getRowIndex(caption);
        Node cell = grid.getChildren().stream()
                .filter(node -> node != caption && row.equals(GridPane.getRowIndex(node)))
                .findFirst().orElseThrow();
        return cell instanceof HBox box ? box.getChildren().getFirst() : cell;
    }

    private static TextField field(LabelDesignerPane pane, String label) {
        return (TextField) control(pane, label);
    }

    private static Label status(LabelDesignerPane pane) {
        return (Label) ((HBox) pane.getBottom()).getChildren().getFirst();
    }

    /** The properties panel's second child names the selected element. */
    private static Label title(LabelDesignerPane pane) {
        return (Label) ((javafx.scene.layout.VBox) pane.getRight()).getChildren().get(1);
    }

    private static DesignerCanvas canvas(LabelDesignerPane pane) {
        return all(pane).filter(DesignerCanvas.class::isInstance).map(DesignerCanvas.class::cast)
                .findFirst().orElseThrow();
    }

    private static StackPane elementView(LabelDesignerPane pane) {
        return canvas(pane).getChildren().stream()
                .filter(node -> node.getStyleClass().contains("designer-element"))
                .map(StackPane.class::cast).findFirst().orElseThrow();
    }
}
