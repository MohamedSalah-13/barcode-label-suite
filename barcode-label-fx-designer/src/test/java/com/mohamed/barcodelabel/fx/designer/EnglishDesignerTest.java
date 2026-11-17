package com.mohamed.barcodelabel.fx.designer;

import com.mohamed.barcodelabel.fx.i18n.UiLanguage;
import com.mohamed.barcodelabel.model.element.ImageFit;
import com.mohamed.barcodelabel.model.element.TextElement;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static com.mohamed.barcodelabel.fx.designer.FxTestSupport.onFx;
import static com.mohamed.barcodelabel.fx.designer.FxTestSupport.runFx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The English designer: left to right, every text in English, and the label surface unchanged. */
class EnglishDesignerTest {
    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        FxTestSupport.startToolkit();
    }

    @AfterEach
    void closeWindows() {
        runFx(() -> List.copyOf(Window.getWindows()).forEach(Window::hide));
    }

    @Test
    void theEnglishDesignerIsLeftToRightAndHasNoArabicText() {
        LabelDesignerPane pane = shown(new VisualLabelDesigner(FxTestSupport.twoTextTemplate(), UiLanguage.ENGLISH));
        runFx(() -> {
            assertEquals(UiLanguage.ENGLISH, pane.language());
            assertEquals(NodeOrientation.LEFT_TO_RIGHT, pane.getEffectiveNodeOrientation());
            assertEquals(NodeOrientation.LEFT_TO_RIGHT, canvas(pane).getEffectiveNodeOrientation());
            button(pane, "+ Text").fire();
            button(pane, "+ Barcode").fire();
            assertEquals("Undo: Add element", button(pane, "Undo").getText());
            assertEquals("Selected: 1 element", status(pane).getText());
            assertEquals("Barcode", pane.getTemplate().elements().getLast().frame().name());
            assertNoArabic(pane);
        });
    }

    @Test
    void newElementsAndTheEmptyTemplateHaveEnglishNames() {
        LabelDesignerPane pane = onFx(() -> new LabelDesignerPane(UiLanguage.ENGLISH));
        runFx(() -> {
            assertEquals("New label", pane.getTemplate().name());
            button(pane, "+ Text").fire();
            TextElement text = assertInstanceOf(TextElement.class, pane.getTemplate().elements().getLast());
            assertEquals("Text", text.frame().name());
            assertEquals("New text", text.textTemplate());
            button(pane, "Undo").fire();
            assertEquals("Redo: Add element", button(pane, "Redo").getText());
            assertEquals("Undo", button(pane, "Undo").getText());
        });
    }

    @Test
    void theContentCaptionAndHintAreInEnglish() {
        LabelDesignerPane pane = onFx(() -> new LabelDesignerPane(UiLanguage.ENGLISH));
        runFx(() -> {
            assertEquals("Content", pane.contentCaption(null));
            button(pane, "+ Barcode").fire();
            assertTrue(all(pane).anyMatch(node -> node instanceof javafx.scene.control.Label label
                    && "Barcode data".equals(label.getText())));
            button(pane, "+ Text").fire();
            assertEquals("Text", pane.contentCaption(pane.getTemplate().elements().getLast()));
            assertTrue(all(pane).anyMatch(node -> node instanceof javafx.scene.control.TextField field
                    && field.getTooltip() != null && field.getTooltip().getText().endsWith("then press Enter")));
        });
    }

    @Test
    void imageFitsAreNamedInTheUiLanguage() {
        LabelDesignerPane english = onFx(() -> new LabelDesignerPane(UiLanguage.ENGLISH));
        LabelDesignerPane arabic = onFx(LabelDesignerPane::new);
        runFx(() -> {
            assertEquals("Fit inside", imageFit(english).getConverter().toString(ImageFit.CONTAIN));
            assertEquals("Stretch", imageFit(english).getConverter().toString(ImageFit.STRETCH));
            assertEquals("احتواء", imageFit(arabic).getConverter().toString(ImageFit.CONTAIN));
            assertEquals("", imageFit(arabic).getConverter().toString(null));
        });
    }

    @Test
    void aNonNumberIsExplainedInEnglish() {
        LabelDesignerPane pane = onFx(() -> new LabelDesignerPane(FxTestSupport.twoTextTemplate(), UiLanguage.ENGLISH));
        runFx(() -> {
            button(pane, "+ Text").fire();
            all(pane.propertiesPanel()).filter(node -> node instanceof javafx.scene.control.TextField).map(javafx.scene.control.TextField.class::cast)
                    .findFirst().orElseThrow().setText("abc");
            button(pane, "Apply properties").fire();
            assertEquals("Invalid value: position, size, rotation and font size must be numbers", status(pane).getText());
        });
    }

    /** Fails on any visible text with Arabic letters: a text that was not translated. */
    static void assertNoArabic(Node root) {
        all(root).forEach(node -> {
            String text = node instanceof Labeled labeled ? labeled.getText()
                    : node instanceof TextInputControl input ? input.getPromptText()
                    : node instanceof ComboBox<?> box ? box.getPromptText() : null;
            if (text != null) {
                assertFalse(text.codePoints().anyMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.ARABIC),
                        "Arabic text in the English UI: " + text);
            }
        });
    }

    private static LabelDesignerPane shown(VisualLabelDesigner designer) {
        return onFx(() -> {
            Stage stage = new Stage();
            stage.setScene(new Scene(designer, 1300, 700));
            stage.show();
            designer.applyCss();
            designer.layout();
            return designer.editorPane();
        });
    }

    private static Stream<Node> all(Node root) {
        Stream<Node> children = Stream.empty();
        if (root instanceof ToolBar toolBar) children = toolBar.getItems().stream();
        else if (root instanceof javafx.scene.control.SplitPane split) children = split.getItems().stream();
        else if (root instanceof ScrollPane scroll && scroll.getContent() != null) children = Stream.of(scroll.getContent());
        else if (root instanceof Parent parent) children = parent.getChildrenUnmodifiable().stream();
        return Stream.concat(Stream.of(root), children.flatMap(EnglishDesignerTest::all));
    }

    private static Button button(LabelDesignerPane pane, String text) {
        return all(pane).filter(node -> node instanceof Button button
                        && (button.getText().equals(text) || button.getText().startsWith(text + ":")))
                .map(Button.class::cast).findFirst()
                .orElseThrow(() -> new AssertionError("no button " + text));
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<ImageFit> imageFit(LabelDesignerPane pane) {
        return all(pane.propertiesPanel()).filter(node -> node instanceof ComboBox<?> box && !box.getItems().isEmpty()
                        && box.getItems().getFirst() instanceof ImageFit)
                .map(node -> (ComboBox<ImageFit>) node).findFirst().orElseThrow();
    }

    private static javafx.scene.control.Label status(LabelDesignerPane pane) {
        return (javafx.scene.control.Label) ((HBox) pane.getBottom()).getChildren().getFirst();
    }

    private static DesignerCanvas canvas(LabelDesignerPane pane) {
        return all(pane).filter(DesignerCanvas.class::isInstance).map(DesignerCanvas.class::cast).findFirst().orElseThrow();
    }
}
