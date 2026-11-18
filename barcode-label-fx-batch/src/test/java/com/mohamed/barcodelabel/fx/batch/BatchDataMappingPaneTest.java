package com.mohamed.barcodelabel.fx.batch;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.LabelElement;
import com.mohamed.barcodelabel.model.element.Symbology;
import com.mohamed.barcodelabel.model.element.TextAlignment;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.workflow.csv.CsvDataset;
import com.mohamed.barcodelabel.workflow.paper.PaperPresetRegistry;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchDataMappingPaneTest {
    private static final String PREPARE = "تحقق وجهّز الطباعة";

    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyStarted) {
            started.countDown();
        }
        assertTrue(started.await(10, TimeUnit.SECONDS), "JavaFX toolkit did not start");
        Platform.setImplicitExit(false);
    }

    @Test
    void validationErrorsAreInArabicAndConfigurationErrorsHaveNoRowNumber() {
        BatchDataMappingPane pane = loadedPane(template(textWith("{{product_name}} {{price}}")), dataset());
        onFx(() -> {
            // The automatic mapping fills both; the user clears them.
            fieldBox(pane, 0).setValue(null);
            fieldBox(pane, 1).setValue(null);
            return null;
        });
        prepareAndWait(pane);
        onFx(() -> {
            assertFalse(pane.getResult().ready());
            assertEquals(List.of("خطأ — الحقل {{product_name}} غير مربوط بأي عمود",
                    "خطأ — الحقل {{price}} غير مربوط بأي عمود"), issueLines(pane));
            return null;
        });
    }

    @Test
    void aReadyBatchIsSummarisedWithArabicCounts() {
        BatchDataMappingPane pane = loadedPane(template(textWith("{{product_name}} {{price}}")), dataset());
        onFx(() -> {
            fieldBox(pane, 0).setValue("Name");
            fieldBox(pane, 1).setValue("Retail Price");
            quantityBox(pane).setValue("Qty");
            return null;
        });
        prepareAndWait(pane);
        onFx(() -> {
            assertTrue(pane.getResult().ready());
            assertEquals("جاهز: سيُطبع 10 ملصقات من 4 صفوف", resultSummary(pane).getText());
            return null;
        });
    }

    @Test
    void changingTheMappingAfterValidationDiscardsTheResult() {
        BatchDataMappingPane pane = loadedPane(template(textWith("{{product_name}} {{price}}")), dataset());
        onFx(() -> {
            fieldBox(pane, 0).setValue("Name");
            fieldBox(pane, 1).setValue("Retail Price");
            return null;
        });
        prepareAndWait(pane);
        onFx(() -> {
            assertTrue(pane.getResult().ready());
            fieldBox(pane, 1).setValue("Code");
            assertNull(pane.getResult());
            assertTrue(issueLines(pane).isEmpty());
            assertEquals("تغيّرت المطابقة؛ اضغط «تحقق وجهّز الطباعة» من جديد", resultSummary(pane).getText());
            return null;
        });

        prepareAndWait(pane);
        onFx(() -> {
            quantityBox(pane).getSelectionModel().selectFirst(); // "one copy per row" instead of Qty
            assertNull(pane.getResult(), "the quantity column is part of the mapping too");
            return null;
        });
    }

    @Test
    void aResultThatArrivesAfterTheMappingChangedIsIgnored() {
        BatchDataMappingPane pane = loadedPane(template(textWith("{{product_name}} {{price}}")), dataset());
        onFx(() -> {
            fieldBox(pane, 1).setValue("Retail Price");
            findButton(pane, PREPARE).fire();
            // Still on the FX thread, so the background preparation cannot have been applied yet.
            fieldBox(pane, 1).setValue("Code");
            return null;
        });
        sleep(500);
        onFx(() -> {
            assertNull(pane.getResult());
            return null;
        });
    }

    @Test
    void aStyleOnlyTemplateChangeKeepsTheMappingAndTheResult() {
        LabelTemplate template = template(textWith("{{product_name}} {{price}}"));
        BatchDataMappingPane pane = loadedPane(template, dataset());
        onFx(() -> {
            fieldBox(pane, 0).setValue("Code"); // manual choices: no header matches these field names
            fieldBox(pane, 1).setValue("Retail Price");
            return null;
        });
        prepareAndWait(pane);
        onFx(() -> {
            var result = pane.getResult();
            // Same fields, different look: what a style edit in the designer produces.
            pane.setTemplate(template.withElements(List.of(new TextElement(ElementFrame.create("name",
                    new RectMm(1, 1, 50, 7), 0), "{{product_name}} {{price}}", "Tahoma", 14, "#FF0000",
                    true, false, TextAlignment.RIGHT))));
            assertSame(result, pane.getResult());
            assertEquals("Code", fieldBox(pane, 0).getValue());
            return null;
        });
    }

    @Test
    void aTemplateWithNewFieldsKeepsTheColumnsAlreadyChosen() {
        BatchDataMappingPane pane = loadedPane(template(textWith("{{product_name}} {{price}}")), dataset());
        onFx(() -> {
            fieldBox(pane, 1).setValue("Code"); // a manual choice the automatic mapping would not make
            quantityBox(pane).getSelectionModel().selectFirst();
            pane.setTemplate(template(textWith("{{product_name}} {{price}}"), barcode("{{sku}}")));

            assertEquals(3, fieldBoxes(pane).size());
            assertEquals("Name", fieldBox(pane, 0).getValue());
            assertEquals("Code", fieldBox(pane, 1).getValue(), "the manual choice survives");
            assertEquals("Code", fieldBox(pane, 2).getValue(), "the new field gets the automatic suggestion");
            assertEquals(quantityBox(pane).getItems().getFirst(), quantityBox(pane).getValue(),
                    "the manual quantity choice survives too");
            assertNull(pane.getResult());
            return null;
        });
    }

    @Test
    void theSampleFileIsMappedAutomaticallyAndTheUserIsAskedToCheck() {
        BatchDataMappingPane pane = loadedPane(template(textWith("{{product_name}} {{price}}"), barcode("{{sku}}")),
                dataset());
        onFx(() -> {
            assertEquals(List.of("Name", "Retail Price", "Code"),
                    fieldBoxes(pane).stream().map(ComboBox::getValue).toList());
            assertEquals("Qty", quantityBox(pane).getValue());
            assertEquals(new BatchTexts(com.mohamed.barcodelabel.fx.i18n.UiLanguage.ARABIC).suggested(3, 3), resultSummary(pane).getText());
            return null;
        });
        prepareAndWait(pane);
        onFx(() -> {
            assertTrue(pane.getResult().ready(), "the suggested mapping is complete");
            assertEquals(10, pane.getResult().labelCount(), "with the quantities from Qty");
            return null;
        });
    }

    @Test
    void aNewDatasetStartsFromTheAutomaticMapping() {
        BatchDataMappingPane pane = loadedPane(template(textWith("{{product_name}} {{price}}")), dataset());
        onFx(() -> {
            fieldBox(pane, 0).setValue("Code");
            pane.setDataset(new CsvDataset(List.of("product_name", "price"),
                    List.of(Map.of("product_name", "Pen", "price", "5")), ','));
            assertEquals("product_name", fieldBox(pane, 0).getValue());
            assertEquals("price", fieldBox(pane, 1).getValue());
            return null;
        });
    }

    @Test
    void theSummaryShowsTheDatasetsSizeAndThePreviewFitsEveryPreviewedRow() {
        BatchDataMappingPane pane = loadedPane(template(textWith("{{product_name}} {{price}}")), dataset());
        onFx(() -> {
            assertTrue(all(pane).anyMatch(node -> node instanceof Label label
                    && label.getText().equals("4 صفوف، 4 أعمدة — حقول القالب: 2 — الملف: البيانات")));
            TableView<?> preview = all(pane).filter(TableView.class::isInstance).map(TableView.class::cast)
                    .findFirst().orElseThrow();
            assertEquals(4, preview.getItems().size());
            assertTrue(preview.getPrefHeight() >= preview.getFixedCellSize() * 5,
                    "room for the header and all 4 rows: " + preview.getPrefHeight());
            return null;
        });
    }

    @Test
    void invalidBarcodeBlocksPrintingUntilCorrectedAndAChangedSymbologyRequiresRechecking() {
        var workbench = onFx(() -> new BatchProductionWorkbench(template(barcode("{{sku}}"))));
        var pane = workbench.batchPane();
        var print = workbench.labelWorkbench().printPane();
        onFx(() -> {
            pane.setDataset(new CsvDataset(List.of("Code"), List.of(Map.of("Code", " "), Map.of("Code", "ABC")), ','));
            return null;
        });
        prepareAndWait(pane);
        onFx(() -> {
            assertEquals(List.of("خطأ — صف 2: عمود Code — قيمة الباركود CODE_128 فارغة"), issueLines(pane));
            assertNull(workbench.labelWorkbench().getDataset());
            assertTrue(findButton(print, "طباعة").isDisabled());
            assertNotNull(print.getTop(), "the workspace explains why printing is blocked");
            pane.setDataset(new CsvDataset(List.of("Code"), List.of(Map.of("Code", "ABC")), ','));
            return null;
        });
        prepareAndWait(pane);
        await(Duration.ofSeconds(10), () -> onFx(() -> print.getTop() == null));
        onFx(() -> {
            assertTrue(pane.getResult().ready());
            assertEquals("ABC", print.getRecords().getFirst().get("sku"));
            var old = barcode("{{sku}}");
            var numeric = new BarcodeElement(old.frame(), old.dataTemplate(), Symbology.EAN_13, 0.25, 2, true);
            workbench.labelWorkbench().setTemplate(template(numeric));
            assertNull(pane.getResult());
            assertTrue(findButton(print, "طباعة").isDisabled());
            return null;
        });
        prepareAndWait(pane);
        onFx(() -> {
            assertFalse(pane.getResult().ready());
            assertTrue(issueLines(pane).getFirst().contains("صف 2: عمود Code"));
            assertTrue(issueLines(pane).getFirst().contains("EAN_13"));
            return null;
        });
    }

    @Test
    void aBatchThatIsNoLongerValidIsTakenOutOfThePrintWorkspace() {
        LabelTemplate template = template(textWith("{{product_name}} {{price}}"));
        BatchProductionWorkbench workbench = onFx(() -> new BatchProductionWorkbench(template));
        BatchDataMappingPane pane = workbench.batchPane();
        onFx(() -> {
            pane.setTemplate(workbench.getTemplate());
            pane.setDataset(dataset());
            fieldBox(pane, 0).setValue("Name");
            fieldBox(pane, 1).setValue("Retail Price");
            return null;
        });
        prepareAndWait(pane);
        onFx(() -> {
            assertNotNull(workbench.labelWorkbench().getDataset());
            fieldBox(pane, 1).setValue("Code");
            assertNull(workbench.labelWorkbench().getDataset(), "the stale batch must not stay printable");
            return null;
        });

        // Data the user put into the print workspace directly is not the batch's to remove.
        onFx(() -> {
            CsvDataset own = dataset();
            workbench.labelWorkbench().setDataset(own);
            pane.setDataset(dataset());
            assertSame(own, workbench.labelWorkbench().getDataset());
            return null;
        });
    }

    private static BatchDataMappingPane loadedPane(LabelTemplate template, CsvDataset dataset) {
        return onFx(() -> {
            BatchDataMappingPane pane = new BatchDataMappingPane();
            pane.setTemplate(template);
            pane.setDataset(dataset);
            return pane;
        });
    }

    private static void prepareAndWait(BatchDataMappingPane pane) {
        onFx(() -> {
            findButton(pane, PREPARE).fire();
            return null;
        });
        await(Duration.ofSeconds(10), () -> onFx(() -> pane.getResult() != null));
    }

    /** Name, Code, Retail Price, Qty — like sample-products-quantities.csv, quantities 2+4+3+1 = 10. */
    private static CsvDataset dataset() {
        List<String> headers = List.of("Name", "Code", "Retail Price", "Qty");
        List<Map<String, String>> rows = new ArrayList<>();
        String[][] values = {{"قلم أزرق", "SKU-1", "12.50", "2"}, {"دفتر", "SKU-2", "35.00", "4"},
                {"علبة أقلام", "SKU-3", "79.95", "3"}, {"مسطرة", "SKU-4", "18.00", "1"}};
        for (String[] row : values) {
            var map = new LinkedHashMap<String, String>();
            for (int i = 0; i < headers.size(); i++) map.put(headers.get(i), row[i]);
            rows.add(map);
        }
        return new CsvDataset(headers, rows, ',');
    }

    private static LabelTemplate template(LabelElement... elements) {
        var page = new PaperPresetRegistry().find("a4-3x7-63.5x38.1").orElseThrow().page();
        return LabelTemplate.create("Products", page, List.of(elements));
    }

    private static TextElement textWith(String text) {
        return new TextElement(ElementFrame.create("name", new RectMm(1, 1, 50, 7), 0), text, "Arial", 10,
                "#000000", false, false, TextAlignment.CENTER);
    }

    private static BarcodeElement barcode(String data) {
        return new BarcodeElement(ElementFrame.create("barcode", new RectMm(1, 9, 50, 20), 1), data,
                Symbology.CODE_128, 0.25, 2, true);
    }

    /** Every node under {@code root}, including split pane items and scroll pane content, which need no skin. */
    private static Stream<Node> all(Node root) {
        Stream<Node> children = Stream.empty();
        if (root instanceof SplitPane split) children = split.getItems().stream();
        else if (root instanceof ScrollPane scroll && scroll.getContent() != null) children = Stream.of(scroll.getContent());
        else if (root instanceof Parent parent) children = parent.getChildrenUnmodifiable().stream();
        return Stream.concat(Stream.of(root), children.flatMap(BatchDataMappingPaneTest::all));
    }

    /** The field mapping combo boxes, in template field order; the quantity box sits in the bottom bar. */
    @SuppressWarnings("unchecked")
    private static List<ComboBox<String>> fieldBoxes(BatchDataMappingPane pane) {
        return all(pane.getCenter()).filter(ComboBox.class::isInstance).map(node -> (ComboBox<String>) node).toList();
    }

    private static ComboBox<String> fieldBox(BatchDataMappingPane pane, int index) {
        return fieldBoxes(pane).get(index);
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<String> quantityBox(BatchDataMappingPane pane) {
        return all(pane.getBottom()).filter(ComboBox.class::isInstance).map(node -> (ComboBox<String>) node)
                .findFirst().orElseThrow();
    }

    private static Label resultSummary(BatchDataMappingPane pane) {
        return all(pane).filter(node -> node instanceof Label label && label.getParent() != null
                        && label.getParent().getChildrenUnmodifiable().stream().anyMatch(ListView.class::isInstance)
                        && label.getParent().getChildrenUnmodifiable().indexOf(label) == 1)
                .map(Label.class::cast).findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static List<String> issueLines(BatchDataMappingPane pane) {
        ListView<String> issues = all(pane).filter(ListView.class::isInstance).map(node -> (ListView<String>) node)
                .findFirst().orElseThrow();
        return List.copyOf(issues.getItems());
    }

    private static Button findButton(Node root, String text) {
        return all(root).filter(node -> node instanceof Button button && text.equals(button.getText()))
                .map(Button.class::cast).findFirst()
                .orElseThrow(() -> new AssertionError("Button not found: " + text));
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private static void await(Duration timeout, BooleanSupplier condition) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) return;
            sleep(25);
        }
        throw new AssertionError("Timed out waiting for JavaFX operation");
    }

    private static <T> T onFx(Supplier<T> action) {
        if (Platform.isFxApplicationThread()) return action.get();
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch completed = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(action.get());
            } catch (Throwable error) {
                failure.set(error);
            } finally {
                completed.countDown();
            }
        });
        try {
            if (!completed.await(10, TimeUnit.SECONDS)) throw new AssertionError("JavaFX action timed out");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for JavaFX", interrupted);
        }
        if (failure.get() instanceof AssertionError assertion) throw assertion;
        if (failure.get() != null) throw new AssertionError("JavaFX action failed", failure.get());
        return result.get();
    }
}
