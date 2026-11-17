package com.mohamed.barcodelabel.app;

import com.mohamed.barcodelabel.batch.TemplateFields;
import com.mohamed.barcodelabel.fx.i18n.UiLanguage;
import com.mohamed.barcodelabel.fx.i18n.UiTexts;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.workflow.csv.CsvDataset;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.render.RenderRequest;
import com.mohamed.barcodelabel.render.java2d.BufferedImageLabelRenderer;
import javafx.application.Platform;
import javafx.geometry.NodeOrientation;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BarcodeLabelSuiteAppTest {
    /** The first row of sample-products-quantities.csv, mapped as docs/USER_GUIDE_AR.md describes. */
    private static final Map<String, String> SAMPLE_ROW = Map.of(
            "product_name", "قلم أزرق", "sku", "SKU-000001", "price", "12.50");

    @Test
    void theStartingTemplateUsesTheFieldsTheUserGuideMaps() {
        assertEquals(List.of("product_name", "sku", "price"), new TemplateFields().scan(BarcodeLabelSuiteApp.template()));
    }

    @Test
    void everyElementOfTheStartingTemplateFitsOnTheLabel() {
        LabelTemplate template = BarcodeLabelSuiteApp.template();
        var label = template.page().labelSize();
        template.elements().forEach(element -> {
            RectMm b = element.frame().bounds();
            assertTrue(b.x() >= 0 && b.y() >= 0 && b.x() + b.width() <= label.width() && b.y() + b.height() <= label.height(),
                    element.frame().name() + " " + b + " lies outside the " + label + " label");
        });
    }

    @Test
    void theSampleProductRendersWithTheStartingTemplate() throws Exception {
        // Fails with a RenderException if, say, the barcode no longer fits its frame for real SKUs.
        BufferedImage image = new BufferedImageLabelRenderer()
                .render(new RenderRequest(BarcodeLabelSuiteApp.template(), SAMPLE_ROW, 203));
        assertEquals(Math.round(63.5 / 25.4 * 203), image.getWidth(), 1);
    }

    @Test
    void theStartingTemplateIsWordedInEachLanguageWithTheSameFields() {
        LabelTemplate english = BarcodeLabelSuiteApp.template(UiLanguage.ENGLISH);
        assertEquals("Products with quantities", english.name());
        assertEquals(new TemplateFields().scan(BarcodeLabelSuiteApp.template()), new TemplateFields().scan(english));
        assertEquals(List.of("Border", "Product", "Barcode", "Price"),
                english.elements().stream().map(element -> element.frame().name()).toList());
        assertEquals("السعر: {{price}} ج.م", ((TextElement) BarcodeLabelSuiteApp.template().elements().getLast()).textTemplate());
        assertEquals("Price: {{price}}", ((TextElement) english.elements().getLast()).textTemplate());
    }

    @Test
    void bothLanguagesTranslateEveryAppText() {
        var arabic = UiTexts.load(BarcodeLabelSuiteApp.class, "messages", UiLanguage.ARABIC);
        var english = UiTexts.load(BarcodeLabelSuiteApp.class, "messages", UiLanguage.ENGLISH);
        assertEquals(arabic.keys(), english.keys());
    }

    @Test
    void theAppOpensInTheSavedLanguageInATitledWindow() throws Exception {
        MemorySetting setting = new MemorySetting(UiLanguage.ENGLISH);
        BarcodeLabelSuiteApp app = started(setting, texts -> true);
        onFx(() -> {
            Stage stage = (Stage) app.workbench().getScene().getWindow();
            assertEquals("Barcode Label Suite", stage.getTitle());
            assertTrue(stage.isShowing());
            StackPane root = assertInstanceOf(StackPane.class, stage.getScene().getRoot());
            assertSame(app.workbench(), root.getChildren().getFirst());
            assertEquals(UiLanguage.ENGLISH, app.workbench().language());
            assertEquals(NodeOrientation.LEFT_TO_RIGHT, root.getNodeOrientation());
            assertEquals(UiLanguage.ENGLISH, app.languagePicker().getValue());
            assertEquals(List.of(UiLanguage.ARABIC, UiLanguage.ENGLISH), app.languagePicker().getItems());
            assertEquals("Products with quantities", app.workbench().getTemplate().name());
            return null;
        });
        closeWindows();
    }

    @Test
    void choosingALanguageRebuildsTheWindowAndRemembersIt() throws Exception {
        MemorySetting setting = new MemorySetting(UiLanguage.ARABIC);
        BarcodeLabelSuiteApp app = started(setting, texts -> true);
        onFx(() -> {
            assertEquals("قالب المنتجات بالكميات", app.workbench().getTemplate().name());
            app.languagePicker().setValue(UiLanguage.ENGLISH);
            assertEquals(UiLanguage.ENGLISH, app.language());
            assertEquals(UiLanguage.ENGLISH, setting.saved);
            assertEquals(UiLanguage.ENGLISH, app.workbench().language());
            assertEquals(NodeOrientation.LEFT_TO_RIGHT, app.workbench().getScene().getRoot().getNodeOrientation());
            assertEquals("Products with quantities", app.workbench().getTemplate().name(), "the untouched label follows the language");

            LabelTemplate edited = app.workbench().getTemplate().withElements(List.of());
            app.workbench().labelWorkbench().setTemplate(edited);
            app.languagePicker().setValue(UiLanguage.ARABIC);
            assertEquals(UiLanguage.ARABIC, setting.saved);
            assertEquals(NodeOrientation.RIGHT_TO_LEFT, app.workbench().getScene().getRoot().getNodeOrientation());
            assertEquals(edited.elements(), app.workbench().getTemplate().elements(), "an edited label is kept");
            assertEquals("Products with quantities", app.workbench().getTemplate().name());
            return null;
        });
        closeWindows();
    }

    @Test
    void switchingWithImportedDataAsksFirstAndCanBeDeclined() throws Exception {
        MemorySetting setting = new MemorySetting(UiLanguage.ARABIC);
        List<UiLanguage> asked = new java.util.ArrayList<>();
        boolean[] answer = {false};
        BarcodeLabelSuiteApp app = started(setting, texts -> { asked.add(texts.language()); return answer[0]; });
        onFx(() -> {
            app.workbench().batchPane().setDataset(new CsvDataset(List.of("Name"), List.of(Map.of("Name", "Pen")), ','));
            app.languagePicker().setValue(UiLanguage.ENGLISH);
            assertEquals(List.of(UiLanguage.ARABIC), asked, "asked in the current language");
            assertEquals(UiLanguage.ARABIC, app.language());
            assertEquals(UiLanguage.ARABIC, app.languagePicker().getValue(), "the menu goes back");
            assertEquals(null, setting.saved);

            answer[0] = true;
            app.languagePicker().setValue(UiLanguage.ENGLISH);
            assertEquals(UiLanguage.ENGLISH, app.language());
            assertEquals(UiLanguage.ENGLISH, setting.saved);
            return null;
        });
        closeWindows();
    }

    private static BarcodeLabelSuiteApp started(LanguageSetting setting, java.util.function.Predicate<UiTexts> confirm) throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyStarted) {
            started.countDown();
        }
        assertTrue(started.await(10, TimeUnit.SECONDS), "JavaFX toolkit did not start");
        Platform.setImplicitExit(false);
        BarcodeLabelSuiteApp app = new BarcodeLabelSuiteApp(setting, confirm);
        onFx(() -> { app.start(new Stage()); return null; });
        return app;
    }

    private static <T> T onFx(java.util.concurrent.Callable<T> action) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<T> result = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(action.call());
            } catch (Throwable error) {
                failure.set(error);
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(15, TimeUnit.SECONDS), "the FX action did not finish");
        if (failure.get() instanceof AssertionError error) throw error;
        if (failure.get() != null) throw new AssertionError("FX action failed", failure.get());
        return result.get();
    }

    private static void closeWindows() throws Exception {
        onFx(() -> { List.copyOf(Window.getWindows()).forEach(Window::hide); return null; });
    }

    /** Keeps the language in memory, so tests do not touch the user's preferences. */
    private static final class MemorySetting implements LanguageSetting {
        private final UiLanguage initial;
        private UiLanguage saved;

        MemorySetting(UiLanguage initial) { this.initial = initial; }

        @Override public UiLanguage load() { return initial; }
        @Override public void save(UiLanguage language) { saved = language; }
    }
}
