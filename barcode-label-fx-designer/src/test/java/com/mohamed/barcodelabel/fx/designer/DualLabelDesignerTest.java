package com.mohamed.barcodelabel.fx.designer;

import com.mohamed.barcodelabel.fx.designer.model.TemplateEditSession;
import com.mohamed.barcodelabel.fx.i18n.*;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.geometry.*;
import com.mohamed.barcodelabel.model.page.*;
import com.mohamed.barcodelabel.render.RenderRequest;
import com.mohamed.barcodelabel.render.java2d.BufferedImageLabelRenderer;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.*;
import java.util.List;
import java.util.Map;
import static com.mohamed.barcodelabel.fx.designer.FxTestSupport.runFx;
import static org.junit.jupiter.api.Assertions.*;

class DualLabelDesignerTest {
    @BeforeAll static void toolkit() throws Exception { FxTestSupport.startToolkit(); }
    @AfterEach void close() { runFx(() -> List.copyOf(Window.getWindows()).forEach(Window::hide)); }

    static LabelTemplate plain() {
        return LabelTemplate.create("Dual preview", new PageDefinition(PageMode.ROLL, new DimensionsMm(40, 25),
                InsetsMm.ZERO, PageOrientation.PORTRAIT, null), List.of());
    }

    @Test void presetCanBeEditedSavedAndUndoneAndHasPrintableContents() {
        runFx(() -> {
            var texts = UiTexts.load(LabelDesignerPane.class, "messages", UiLanguage.ARABIC);
            var dialog = new LabelLayoutDialog(plain(), texts);
            dialog.show();
            ((Button) dialog.getDialogPane().lookup("#layout-preset")).fire();
            var dual = dialog.build();
            assertTrue(dual.contentFits());
            assertEquals(new DimensionsMm(40, 25), dual.page().pageSize());
            assertEquals(3, dual.elements().size());
            assertTrue(dual.dualLayout().showGuide());
            assertEquals(2, dual.schemaVersion());
            var session = new TemplateEditSession(plain());
            session.changeLayout(dual);
            session.undo(); assertNull(session.template().dualLayout());
            session.redo(); assertEquals(dual, session.template());
            try {
                new com.mohamed.barcodelabel.data.TemplateJsonService().write(java.nio.file.Path.of("target/dual-label-40x25.label.json"), dual);
                var image = new BufferedImageLabelRenderer().render(new RenderRequest(dual, Map.of(), 203));
                javax.imageio.ImageIO.write(image, "png", new java.io.File("target/dual-label-print.png"));
                dialog.getDialogPane().applyCss(); dialog.getDialogPane().layout();
                javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(dialog.getDialogPane().snapshot(null, null), null),
                        "png", new java.io.File("target/dual-label-settings.png"));
            } catch (Exception exception) { throw new AssertionError(exception); }
            ((CheckBox) dialog.getDialogPane().lookup("#layout-sample")).setSelected(false);
            ((TextField) dialog.getDialogPane().lookup("#layout-height")).setText("30");
            ((CheckBox) dialog.getDialogPane().lookup("#layout-centred")).setSelected(false);
            ((TextField) dialog.getDialogPane().lookup("#layout-split")).setText("14");
            ((CheckBox) dialog.getDialogPane().lookup("#layout-guide")).setSelected(false);
            assertEquals(14, dialog.build().dualLayout().splitPositionMm());
            assertFalse(dialog.build().dualLayout().showGuide());
            ((TextField) dialog.getDialogPane().lookup("#layout-gap")).setText("100");
            assertThrows(IllegalArgumentException.class, dialog::build);
        });
    }

    @Test void copyTracksSourceEditsAndArabicDoesNotMirrorCoordinates() {
        runFx(() -> {
            var texts = UiTexts.load(LabelDesignerPane.class, "messages", UiLanguage.ARABIC);
            var dual = plain().withLayout(plain().page(), DualLabelLayout.standard());
            dual = dual.withElements(LabelLayoutDialog.sampleElements(dual.designSize(), texts));
            var session = new TemplateEditSession(dual);
            var canvas = new DesignerCanvas(session, texts);
            var stage = new Stage(); stage.setScene(new Scene(new StackPane(canvas), 500, 350)); stage.show();
            canvas.applyCss(); canvas.layout();
            assertEquals(javafx.geometry.NodeOrientation.LEFT_TO_RIGHT, canvas.getEffectiveNodeOrientation());
            assertEquals(3, canvas.lookupAll(".dual-label-copy").size());
            assertNotNull(canvas.lookup("#dual-label-guide"));
            var source = canvas.lookupAll(".designer-element").iterator().next();
            assertEquals(4, source.getTranslateX());
            var id = dual.elements().getFirst().frame().id();
            session.setContent(id, "Updated");
            assertEquals(2, canvas.lookupAll(".element-text").stream()
                    .filter(node -> ((javafx.scene.text.Text) node).getText().equals("Updated")).count());
            session.select(id, false); session.moveSelected(100, 100);
            var bounds = session.find(id).frame().bounds();
            assertTrue(bounds.x() + bounds.width() <= dual.designSize().width());
            assertTrue(bounds.y() + bounds.height() <= dual.designSize().height());
        });
    }

    @Test void normalAndSheetSettingsPreservePaperAndCancelDoesNotChangeTemplate() {
        runFx(() -> {
            var texts = UiTexts.load(LabelDesignerPane.class, "messages", UiLanguage.ENGLISH);
            var original = FxTestSupport.twoTextTemplate();
            var dialog = new LabelLayoutDialog(original, texts);
            dialog.show();
            assertNull(dialog.build().dualLayout());
            ((Button) dialog.getDialogPane().lookup("#layout-preset")).fire();
            var dual = dialog.build();
            assertEquals(original.page().pageSize(), dual.page().pageSize());
            assertEquals(new DimensionsMm(40, 25), dual.page().labelSize());
            assertEquals(original.page().sheetLayout().rows(), dual.page().sheetLayout().rows());
            assertNull(original.dualLayout());
            var reopened = new LabelLayoutDialog(dual, texts);
            assertEquals(dual.dualLayout(), reopened.build().dualLayout());
            assertEquals(dual.elements(), reopened.build().elements());
            dialog.close();
            assertNull(dialog.getResult());
        });
    }
}
