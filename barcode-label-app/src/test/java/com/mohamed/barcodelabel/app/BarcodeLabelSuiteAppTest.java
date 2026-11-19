package com.mohamed.barcodelabel.app;

import com.mohamed.barcodelabel.batch.TemplateFields;
import com.mohamed.barcodelabel.fx.i18n.UiLanguage;
import com.mohamed.barcodelabel.fx.i18n.UiTexts;
import com.mohamed.barcodelabel.fx.batch.BatchDataMappingPane;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.workflow.csv.CsvDataset;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.render.RenderRequest;
import com.mohamed.barcodelabel.render.java2d.BufferedImageLabelRenderer;
import javafx.application.Platform;
import javafx.geometry.NodeOrientation;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        BarcodeLabelSuiteApp app = started(setting);
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
        BarcodeLabelSuiteApp app = started(setting);
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
    void theUpdateButtonSitsBesideTheLanguageMenuInTheWindowLanguage() throws Exception {
        MemorySetting setting = new MemorySetting(UiLanguage.ARABIC);
        BarcodeLabelSuiteApp app = started(setting);
        onFx(() -> {
            assertEquals("التحقق من التحديثات", app.checkUpdatesButton().getAccessibleText());
            assertEquals(ContentDisplay.GRAPHIC_ONLY, app.checkUpdatesButton().getContentDisplay());
            assertInstanceOf(FontIcon.class, app.checkUpdatesButton().getGraphic());
            assertSame(app.languagePicker().getParent(), app.checkUpdatesButton().getParent());
            assertEquals("لغة الواجهة", app.languagePicker().getTooltip().getText());
            assertEquals(Feather.GLOBE,
                    assertInstanceOf(FontIcon.class, app.languagePicker().getButtonCell().getGraphic()).getIconCode());
            assertNull(app.languagePicker().getButtonCell().getText());
            app.languagePicker().applyCss();
            assertEquals(0.0, app.languagePicker().lookup(".arrow").getOpacity());
            var globe = app.languagePicker().getButtonCell().getGraphic();
            for (var type : List.of(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED)) {
                globe.fireEvent(new MouseEvent(type, 8, 8, 0, 0, MouseButton.PRIMARY, 1,
                        false, false, false, false, type == MouseEvent.MOUSE_PRESSED,
                        false, false, false, false, true, null));
            }
            assertTrue(app.languagePicker().isShowing());
            app.languagePicker().hide();
            // The tests run without the jpackage launcher, so the copy has no installed version.
            assertEquals("التحقق من التحديثات\nنسخة تطوير: التحديث يعمل في النسخة المثبتة فقط.",
                    app.checkUpdatesButton().getTooltip().getText());

            app.languagePicker().setValue(UiLanguage.ENGLISH);
            assertEquals("Interface language", app.languagePicker().getTooltip().getText());
            assertEquals("Check for updates", app.checkUpdatesButton().getAccessibleText());
            assertEquals("Check for updates\nA development copy: only an installed copy is updated.",
                    app.checkUpdatesButton().getTooltip().getText());
            return null;
        });
        closeWindows();
    }

    @Test
    void switchingLanguagesKeepsTheSampleMappingQuantitiesAndSecondPage() throws Exception {
        BarcodeLabelSuiteApp app = started(new MemorySetting(UiLanguage.ARABIC));
        try {
            onFx(() -> {
                app.workbench().batchPane().importCsv(java.nio.file.Path.of("../sample-products-quantities.csv"));
                assertTrue(app.languagePicker().isDisabled(), "wait for the file callback before rebuilding");
                app.switchLanguage(UiLanguage.ENGLISH);
                assertEquals(UiLanguage.ARABIC, app.language());
                return null;
            });
            awaitFx(() -> app.workbench().batchPane().getDataset() != null && !app.languagePicker().isDisabled());
            onFx(() -> {
                var batch = app.workbench().batchPane();
                fieldBox(batch, 0).setValue("Code"); // A deliberate manual override of the suggested Name column.
                var spinners = nodes(batch, Spinner.class);
                setSpinner(spinners.get(0), 3);
                setSpinner(spinners.get(1), 40);
                setSpinner(spinners.get(2), 500);
                button(batch, "تحقق وجهّز الطباعة").fire();
                assertTrue(app.languagePicker().isDisabled());
                return null;
            });
            awaitFx(() -> app.workbench().batchPane().getResult() != null && !app.languagePicker().isDisabled());
            var session = onFx(() -> {
                var label = app.workbench().labelWorkbench();
                ((TabPane) label.getCenter()).getSelectionModel().select(2);
                var print = label.printPane();
                var spinners = nodes(print, Spinner.class);
                setSpinner(spinners.get(0), 2);
                setSpinner(spinners.get(1), 20); // Ten labels on a partially used sheet need two pages.
                nodes(print, CheckBox.class).getFirst().setSelected(false);
                setSpinner(spinners.get(2), 2);
                setSpinner(spinners.get(3), 2);
                button(print, "التالي").fire();
                assertTrue(hasText(print, "صفحة 2 من 2"));
                assertEquals(10, app.workbench().batchPane().getResult().labelCount());
                return app.workbench().snapshotSession();
            });
            for (UiLanguage language : List.of(UiLanguage.ENGLISH, UiLanguage.ARABIC)) {
                onFx(() -> {
                    app.languagePicker().setValue(language);
                    var current = app.workbench().snapshotSession();
                    assertSame(session.batch().dataset(), current.batch().dataset());
                    assertEquals("sample-products-quantities.csv", current.batch().datasetName());
                    assertEquals(session.batch().mapping(), current.batch().mapping());
                    assertSame(session.batch().result(), current.batch().result());
                    assertSame(session.label().dataset(), current.label().dataset());
                    assertTrue(current.printingPreparedBatch());
                    assertEquals(2, current.label().selectedTab());
                    assertEquals(session.selectedTab(), current.selectedTab());
                    assertEquals(1, current.label().print().pageIndex());
                    assertEquals(20, current.label().print().startSlot());
                    assertEquals(2, current.label().print().copies());
                    assertFalse(current.label().print().allPages());
                    assertEquals(2, current.label().print().fromPage());
                    assertEquals(2, current.label().print().toPage());
                    assertTrue(hasText(app.workbench().labelWorkbench().printPane(),
                            language == UiLanguage.ENGLISH ? "Page 2 of 2" : "صفحة 2 من 2"));
                    assertTrue(hasText(app.workbench().batchPane(), language == UiLanguage.ENGLISH
                            ? "Ready: 10 labels will print from 4 rows" : "جاهز: سيُطبع 10 ملصقات من 4 صفوف"));
                    // Entering the print tab again must not reset the restored page or range.
                    var tabs = (TabPane) app.workbench().labelWorkbench().getCenter();
                    tabs.getSelectionModel().select(0);
                    tabs.getSelectionModel().select(2);
                    assertEquals(1, app.workbench().snapshotSession().label().print().pageIndex());
                    return null;
                });
            }
            onFx(() -> {
                var batch = app.workbench().batchPane();
                quantityBox(batch).getSelectionModel().selectFirst();
                fieldBox(batch, 0).setValue(null); // An unmapped field must stay unmapped, not auto-filled.
                assertNull(batch.getResult());
                assertNull(app.workbench().labelWorkbench().getDataset(), "prepared-data ownership survived the switch");
                ((TabPane) app.workbench().getCenter()).getSelectionModel().select(1);
                app.switchLanguage(UiLanguage.ENGLISH);
                assertNull(app.workbench().batchPane().getResult());
                assertNull(fieldBox(app.workbench().batchPane(), 0).getValue());
                assertNull(app.workbench().snapshotSession().batch().mapping().quantityColumn());
                assertEquals(3, app.workbench().snapshotSession().batch().mapping().defaultCopies());
                assertEquals(1, app.workbench().snapshotSession().selectedTab());
                assertTrue(hasText(app.workbench().batchPane(), "The mapping changed; click “Validate and prepare printing” again"));
                assertTrue(button(app.workbench().labelWorkbench().printPane(), "Print").isDisabled());
                return null;
            });
        } finally { closeWindows(); }
    }

    @Test
    void anInvalidBatchKeepsItsTranslatedErrorsAndPrintBlockAcrossLanguages() throws Exception {
        BarcodeLabelSuiteApp app = started(new MemorySetting(UiLanguage.ARABIC));
        try {
            onFx(() -> {
                app.workbench().batchPane().setDataset(new CsvDataset(List.of("product_name", "sku", "price"),
                        List.of(Map.of("product_name", "Pen", "sku", "", "price", "12")), ','));
                button(app.workbench().batchPane(), "تحقق وجهّز الطباعة").fire();
                return null;
            });
            awaitFx(() -> app.workbench().batchPane().getResult() != null);
            var result = onFx(() -> app.workbench().batchPane().getResult());
            assertFalse(result.ready());
            for (UiLanguage language : List.of(UiLanguage.ENGLISH, UiLanguage.ARABIC)) {
                onFx(() -> {
                    app.switchLanguage(language);
                    assertSame(result, app.workbench().batchPane().getResult());
                    assertNull(app.workbench().labelWorkbench().getDataset());
                    ListView<?> issues = nodes(app.workbench().batchPane(), ListView.class).getFirst();
                    String message = issues.getItems().getFirst().toString();
                    assertTrue(message.contains("sku"));
                    assertEquals(language == UiLanguage.ARABIC, containsArabic(message));
                    var print = app.workbench().labelWorkbench().printPane();
                    assertTrue(button(print, language == UiLanguage.ENGLISH ? "Print" : "طباعة").isDisabled());
                    assertEquals(language == UiLanguage.ARABIC,
                            containsArabic(((ListView<?>) print.getTop()).getItems().getFirst().toString()));
                    return null;
                });
            }
        } finally { closeWindows(); }
    }

    @Test
    void directlyImportedDataIsKeptSeparatelyAndRecheckedInTheNewLanguage() throws Exception {
        BarcodeLabelSuiteApp app = started(new MemorySetting(UiLanguage.ARABIC));
        CsvDataset direct = new CsvDataset(List.of("product_name", "sku", "price"),
                List.of(Map.of("product_name", "Pen", "sku", "", "price", "12")), ',');
        try {
            onFx(() -> { app.workbench().labelWorkbench().setDataset(direct); return null; });
            for (UiLanguage language : List.of(UiLanguage.ENGLISH, UiLanguage.ARABIC)) {
                onFx(() -> { app.switchLanguage(language); return null; });
                awaitFx(() -> {
                    var top = app.workbench().labelWorkbench().printPane().getTop();
                    return top instanceof ListView<?> list && list.getItems().stream().anyMatch(item -> item.toString().contains("sku"));
                });
                onFx(() -> {
                    assertSame(direct, app.workbench().labelWorkbench().getDataset());
                    assertNull(app.workbench().batchPane().getDataset());
                    assertFalse(app.workbench().snapshotSession().printingPreparedBatch());
                    var print = app.workbench().labelWorkbench().printPane();
                    assertTrue(button(print, language == UiLanguage.ENGLISH ? "Print" : "طباعة").isDisabled());
                    assertEquals(language == UiLanguage.ARABIC,
                            containsArabic(((ListView<?>) print.getTop()).getItems().getFirst().toString()));
                    return null;
                });
            }
            onFx(() -> {
                app.workbench().batchPane().setDataset(new CsvDataset(List.of("Name"), List.of(Map.of("Name", "Pending")), ','));
                app.switchLanguage(UiLanguage.ENGLISH);
                fieldBox(app.workbench().batchPane(), 0).setValue(null);
                assertSame(direct, app.workbench().labelWorkbench().getDataset(), "editing a mapping must not clear direct data");
                assertNull(app.workbench().batchPane().getResult());
                return null;
            });
        } finally { closeWindows(); }
    }

    @Test
    void theWindowHasTheAppIconInEverySize() throws Exception {
        BarcodeLabelSuiteApp app = started(new MemorySetting(UiLanguage.ARABIC));
        onFx(() -> {
            Stage stage = (Stage) app.workbench().getScene().getWindow();
            assertEquals(List.of(16.0, 24.0, 32.0, 48.0, 64.0, 128.0, 256.0),
                    stage.getIcons().stream().map(javafx.scene.image.Image::getWidth).toList());
            assertTrue(stage.getIcons().stream().noneMatch(javafx.scene.image.Image::isError));
            return null;
        });
        closeWindows();
    }

    @Test
    void theInstallerIconHoldsTheSameSizes() throws Exception {
        // jpackage takes this file (package.ps1 --icon); a Windows icon starts with 0, type 1 and its image count.
        java.nio.ByteBuffer ico = java.nio.ByteBuffer.wrap(java.nio.file.Files.readAllBytes(
                java.nio.file.Path.of("src/main/package/BarcodeLabelSuite.ico"))).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        assertEquals(0, ico.getShort());
        assertEquals(1, ico.getShort());
        assertEquals(7, ico.getShort());
    }

    @Test
    void theLabelBeingDesignedOpensAgainOnTheNextRun() throws Exception {
        LastDesign lastDesign = LastDesign.inMemory();
        BarcodeLabelSuiteApp first = started(new MemorySetting(UiLanguage.ARABIC), lastDesign);
        onFx(() -> { first.stop(); return null; });
        assertTrue(lastDesign.load().isEmpty(), "the untouched starting label is not kept");
        LabelTemplate edited = BarcodeLabelSuiteApp.template().withElements(List.of());
        onFx(() -> { first.workbench().labelWorkbench().setTemplate(edited); first.stop(); return null; });
        assertEquals(edited, lastDesign.load().orElseThrow());
        closeWindows();

        BarcodeLabelSuiteApp second = started(new MemorySetting(UiLanguage.ENGLISH), lastDesign);
        onFx(() -> {
            assertEquals(edited, second.workbench().getTemplate());
            second.languagePicker().setValue(UiLanguage.ARABIC);
            assertEquals(edited, second.workbench().getTemplate(), "a switch keeps the restored label as it is");
            return null;
        });
        closeWindows();
    }

    @Test
    void theLastDesignFileKeepsTheLabelAndIgnoresADamagedFile(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory)
            throws Exception {
        java.nio.file.Path file = directory.resolve("app").resolve("last-design.label.json");
        LastDesign store = LastDesign.file(file);
        assertTrue(store.load().isEmpty(), "nothing before the first close");

        LabelTemplate template = BarcodeLabelSuiteApp.template(UiLanguage.ENGLISH);
        store.save(template);
        assertEquals(template, LastDesign.file(file).load().orElseThrow());
        assertEquals(List.of(file), java.nio.file.Files.list(file.getParent()).toList(), "no partial file left behind");

        java.nio.file.Files.writeString(file, "{ not json");
        assertTrue(LastDesign.file(file).load().isEmpty(), "a damaged file is ignored");

        java.nio.file.Path blocked = file.resolve("inside-a-file.label.json");
        LastDesign.file(blocked).save(template);
        assertTrue(LastDesign.file(blocked).load().isEmpty(), "a label that cannot be written is only not kept");
    }

    @Test
    void theLastDesignIsInAppDataOnWindowsAndInTheHomeFolderElsewhere() {
        assertEquals(java.nio.file.Path.of("C:/Users/u/AppData/Roaming", "Barcode Label Suite", "last-design.label.json"),
                LastDesign.userFile("C:/Users/u/AppData/Roaming", "C:/Users/u"));
        assertEquals(java.nio.file.Path.of("/home/u", ".barcode-label-suite", "last-design.label.json"),
                LastDesign.userFile(null, "/home/u"));
        assertEquals(java.nio.file.Path.of("/home/u", ".barcode-label-suite", "last-design.label.json"),
                LastDesign.userFile(" ", "/home/u"));
        assertTrue(LastDesign.userFile() != null);
    }

    private static BarcodeLabelSuiteApp started(LanguageSetting setting) throws Exception {
        return started(setting, LastDesign.inMemory());
    }

    private static BarcodeLabelSuiteApp started(LanguageSetting setting,
                                                LastDesign lastDesign) throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyStarted) {
            started.countDown();
        }
        assertTrue(started.await(10, TimeUnit.SECONDS), "JavaFX toolkit did not start");
        Platform.setImplicitExit(false);
        BarcodeLabelSuiteApp app = new BarcodeLabelSuiteApp(setting,
                com.mohamed.barcodelabel.fx.workbench.CustomPaperStore.inMemory(), lastDesign);
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

    private static void awaitFx(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            if (onFx(condition::getAsBoolean)) return;
            Thread.sleep(25);
        }
        throw new AssertionError("Timed out waiting for session state");
    }

    private static boolean containsArabic(String text) {
        return text.codePoints().anyMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.ARABIC);
    }

    private static <T extends Node> List<T> nodes(Node root, Class<T> type) {
        var found = new java.util.ArrayList<T>();
        if (type.isInstance(root)) found.add(type.cast(root));
        List<? extends Node> children = root instanceof TabPane tabs ? tabs.getTabs().stream().map(Tab::getContent).toList()
                : root instanceof SplitPane split ? split.getItems()
                : root instanceof ScrollPane scroll ? (scroll.getContent() == null ? List.of() : List.of(scroll.getContent()))
                : root instanceof Parent parent ? parent.getChildrenUnmodifiable() : List.of();
        children.forEach(child -> found.addAll(nodes(child, type)));
        return found;
    }

    private static Button button(Node root, String text) {
        return nodes(root, Button.class).stream().filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
    }

    private static boolean hasText(Node root, String text) {
        return nodes(root, Label.class).stream().anyMatch(label -> text.equals(label.getText()));
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<String> fieldBox(BatchDataMappingPane pane, int index) {
        GridPane grid = nodes(pane, GridPane.class).getFirst();
        return (ComboBox<String>) nodes(grid, ComboBox.class).get(index);
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<String> quantityBox(BatchDataMappingPane pane) {
        return (ComboBox<String>) nodes(pane.getBottom(), ComboBox.class).getFirst();
    }

    @SuppressWarnings("unchecked")
    private static void setSpinner(Spinner<?> spinner, int value) {
        ((Spinner<Integer>) spinner).getValueFactory().setValue(value);
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
