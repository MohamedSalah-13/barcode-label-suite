package com.mohamed.barcodelabel.fx.workbench;

import com.mohamed.barcodelabel.fx.i18n.UiLanguage;
import com.mohamed.barcodelabel.fx.i18n.UiTexts;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.TextAlignment;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.workflow.csv.CsvDataset;
import com.mohamed.barcodelabel.workflow.paper.PaperPreset;
import com.mohamed.barcodelabel.workflow.paper.PaperPresetRegistry;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.mohamed.barcodelabel.fx.workbench.FxSupport.all;
import static com.mohamed.barcodelabel.fx.workbench.FxSupport.assertNoArabic;
import static com.mohamed.barcodelabel.fx.workbench.FxSupport.await;
import static com.mohamed.barcodelabel.fx.workbench.FxSupport.onFx;
import static com.mohamed.barcodelabel.fx.workbench.FxSupport.runFx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnglishWorkbenchTest {
    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        FxSupport.startToolkit();
    }

    @AfterEach
    void closeWindows() {
        runFx(() -> List.copyOf(Window.getWindows()).forEach(Window::hide));
    }

    @Test
    void bothLanguagesTranslateEveryWorkbenchText() {
        UiTexts arabic = UiTexts.load(BarcodeLabelWorkbench.class, "messages", UiLanguage.ARABIC);
        UiTexts english = UiTexts.load(BarcodeLabelWorkbench.class, "messages", UiLanguage.ENGLISH);
        assertEquals(arabic.keys(), english.keys());
        english.keys().forEach(key -> {
            assertFalse(english.get(key).isBlank(), key);
            assertNoArabic(english.get(key));
        });
    }

    @Test
    void theEnglishWorkbenchIsLeftToRightAndHasNoArabicText() {
        BarcodeLabelWorkbench workbench = shown(new BarcodeLabelWorkbench(template(), UiLanguage.ENGLISH));
        runFx(() -> {
            assertEquals(UiLanguage.ENGLISH, workbench.language());
            assertEquals(UiLanguage.ENGLISH, workbench.designer().language());
            assertEquals(UiLanguage.ENGLISH, workbench.printPane().language());
            assertEquals(NodeOrientation.LEFT_TO_RIGHT, workbench.getEffectiveNodeOrientation());
            TabPane tabs = all(workbench).filter(TabPane.class::isInstance).map(TabPane.class::cast).findFirst().orElseThrow();
            assertEquals(List.of("Label design", "Print data", "Preview and print"),
                    tabs.getTabs().stream().map(tab -> tab.getText()).toList());
            workbench.setDataset(new CsvDataset(List.of("name"), List.of(Map.of("name", "Pen")), ','));
            assertTrue(labelShown(workbench, "1 record — 1 column"));
        });
        await(Duration.ofSeconds(15), () -> onFx(() -> labelShown(workbench, "Preview ready")));
        runFx(() -> {
            assertTrue(labelShown(workbench, "Page 1 of 1"));
            assertNoArabic(workbench);
        });
    }

    @Test
    void builtInPaperSizesAreNamedInTheUiLanguageAndOthersKeepTheirName() {
        BarcodeLabelWorkbench english = onFx(() -> new BarcodeLabelWorkbench(template(), UiLanguage.ENGLISH));
        BarcodeLabelWorkbench arabic = onFx(() -> new BarcodeLabelWorkbench(template()));
        PaperPresetRegistry registry = new PaperPresetRegistry();
        PaperPreset sheet = registry.find("a4-3x7-63.5x38.1").orElseThrow();
        PaperPreset roll = registry.find("roll-40x25").orElseThrow();
        assertEquals("A4 — 3 × 7 (63.5 × 38.1 mm)", english.presetName(sheet));
        assertEquals("Thermal roll — 40 × 25 mm", english.presetName(roll));
        registry.all().forEach(preset -> assertEquals(preset.displayName(), arabic.presetName(preset)));
        PaperPreset own = new PaperPreset("roll-40x25", "Shop roll", "Rolls", roll.page());
        assertEquals("Shop roll", english.presetName(own));
    }

    private static BarcodeLabelWorkbench shown(BarcodeLabelWorkbench workbench) {
        runFx(() -> {
            Stage stage = new Stage();
            stage.setScene(new Scene(workbench, 1400, 900));
            stage.show();
            workbench.applyCss();
            workbench.layout();
        });
        return workbench;
    }

    private static boolean labelShown(BarcodeLabelWorkbench workbench, String text) {
        return all(workbench).anyMatch(node -> node instanceof Label label && text.equals(label.getText()));
    }

    private static LabelTemplate template() {
        var page = new PaperPresetRegistry().find("a4-3x7-63.5x38.1").orElseThrow().page();
        var name = new TextElement(ElementFrame.create("Name", new RectMm(4, 4, 50, 8), 0),
                "{{name}}", "Arial", 11, "#111827", false, false, TextAlignment.CENTER);
        return LabelTemplate.create("Products", page, List.of(name));
    }
}
