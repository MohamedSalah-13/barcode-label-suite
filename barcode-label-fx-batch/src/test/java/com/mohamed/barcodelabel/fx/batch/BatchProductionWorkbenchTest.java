package com.mohamed.barcodelabel.fx.batch;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.Symbology;
import com.mohamed.barcodelabel.model.element.TextAlignment;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.workflow.csv.CsvDataset;
import com.mohamed.barcodelabel.workflow.paper.PaperPresetRegistry;
import javafx.application.Platform;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchProductionWorkbenchTest {
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
    void nullValuesBecomeEmptyStringsNotTheLiteralWordNull() {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("sku", "A1");
        record.put("note", null);
        CsvDataset dataset = BatchProductionWorkbench.asDataset(List.of(record));
        assertEquals("", dataset.rows().get(0).get("note"));
    }

    @Test
    void missingKeysAlsoBecomeEmptyStrings() {
        Map<String, Object> first = Map.of("sku", "A1", "price", "10");
        Map<String, Object> second = Map.of("sku", "A2");
        CsvDataset dataset = BatchProductionWorkbench.asDataset(List.of(first, second));
        assertTrue(dataset.headers().containsAll(List.of("sku", "price")));
        assertEquals("", dataset.rows().get(1).get("price"));
    }

    @Test
    void headersAreTheUnionOfAllRecordKeys() {
        Map<String, Object> first = Map.of("a", "1");
        Map<String, Object> second = Map.of("b", "2");
        CsvDataset dataset = BatchProductionWorkbench.asDataset(List.of(first, second));
        assertEquals(2, dataset.headers().size());
        assertTrue(dataset.headers().containsAll(List.of("a", "b")));
    }

    @Test
    void constructsTheCompleteRightToLeftProductionWorkspace() {
        BatchProductionWorkbench workbench = onFx(() -> new BatchProductionWorkbench(template()));

        onFx(() -> {
            assertEquals(NodeOrientation.RIGHT_TO_LEFT, workbench.getNodeOrientation());
            assertNotNull(workbench.labelWorkbench());
            assertNotNull(workbench.batchPane());
            TabPane tabs = (TabPane) workbench.getCenter();
            assertEquals(2, tabs.getTabs().size());
            assertFalse(tabs.getTabs().get(0).isClosable());
            assertFalse(tabs.getTabs().get(1).isClosable());
            return null;
        });
    }

    @Test
    void preparingAnAutomaticallyMappedBatchFeedsThePrintWorkspace() {
        BatchProductionWorkbench workbench = onFx(() -> new BatchProductionWorkbench(template()));
        CsvDataset dataset = new CsvDataset(
                List.of("product_name", "sku", "price"),
                List.of(
                        Map.of("product_name", "Blue pen", "sku", "A1", "price", "12.5"),
                        Map.of("product_name", "Notebook", "sku", "B2", "price", "30")),
                ',');

        onFx(() -> {
            workbench.batchPane().setTemplate(workbench.getTemplate());
            workbench.batchPane().setDataset(dataset);
            findButton(workbench.batchPane(), "تحقق وجهّز الطباعة").fire();
            return null;
        });

        await(Duration.ofSeconds(10), () -> onFx(() -> workbench.batchPane().getResult() != null));
        onFx(() -> {
            assertTrue(workbench.batchPane().getResult().ready());
            assertEquals(2, workbench.batchPane().getResult().labelCount());
            assertEquals(2, workbench.labelWorkbench().getDataset().rows().size());
            assertEquals("B2", workbench.labelWorkbench().getDataset().rows().get(1).get("sku"));
            var session = workbench.snapshotSession();
            var restored = new BatchProductionWorkbench(workbench.getTemplate(),
                    com.mohamed.barcodelabel.fx.i18n.UiLanguage.ENGLISH);
            restored.restoreSession(session);
            assertEquals(session.batch().mapping(), restored.snapshotSession().batch().mapping());
            assertEquals(session.batch().result(), restored.batchPane().getResult());
            assertTrue(restored.snapshotSession().printingPreparedBatch());
            assertTrue(texts(restored).contains("Ready: 2 labels will print from 2 rows"));
            // Changing the fields after restoration must invalidate the batch and remove its print data.
            restored.labelWorkbench().setTemplate(template().withElements(List.of()));
            assertEquals(null, restored.batchPane().getResult());
            assertEquals(null, restored.labelWorkbench().getDataset());
            return null;
        });
    }

    @Test
    void theEnglishWorkspaceIsLeftToRightAndPreparesABatchInEnglish() {
        BatchProductionWorkbench workbench = onFx(() -> {
            BatchProductionWorkbench english = new BatchProductionWorkbench(template(), com.mohamed.barcodelabel.fx.i18n.UiLanguage.ENGLISH);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setScene(new javafx.scene.Scene(english, 1400, 900));
            stage.show();
            return english;
        });
        CsvDataset dataset = new CsvDataset(List.of("Name", "Code", "Retail Price", "Qty"), List.of(
                Map.of("Name", "Blue pen", "Code", "A1", "Retail Price", "12.5", "Qty", "2"),
                Map.of("Name", "Notebook", "Code", "B2", "Retail Price", "30", "Qty", "3")), ',');
        onFx(() -> {
            assertEquals(com.mohamed.barcodelabel.fx.i18n.UiLanguage.ENGLISH, workbench.language());
            assertEquals(NodeOrientation.LEFT_TO_RIGHT, workbench.getEffectiveNodeOrientation());
            TabPane tabs = (TabPane) workbench.getCenter();
            assertEquals(List.of("Design, preview and print", "CSV mapping and quantities"),
                    tabs.getTabs().stream().map(tab -> tab.getText()).toList());
            tabs.getSelectionModel().select(1);
            workbench.batchPane().setTemplate(workbench.getTemplate());
            workbench.batchPane().setDataset(dataset);
            findButton(workbench.batchPane(), "Validate and prepare printing").fire();
            return null;
        });
        await(Duration.ofSeconds(10), () -> onFx(() -> workbench.batchPane().getResult() != null));
        onFx(() -> {
            assertTrue(workbench.batchPane().getResult().ready());
            assertTrue(texts(workbench).contains("Ready: 5 labels will print from 2 rows"), texts(workbench).toString());
            texts(workbench).forEach(text -> assertTrue(text.codePoints()
                    .noneMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.ARABIC), "Arabic text: " + text));
            List.copyOf(javafx.stage.Window.getWindows()).forEach(javafx.stage.Window::hide);
            return null;
        });
    }

    /** Every label, button and prompt text in the workspace, including the content of both tabs. */
    private static List<String> texts(Node root) {
        List<String> result = new java.util.ArrayList<>();
        if (root instanceof javafx.scene.control.Labeled labeled && labeled.getText() != null) result.add(labeled.getText());
        if (root instanceof javafx.scene.control.ComboBoxBase<?> box && box.getPromptText() != null) result.add(box.getPromptText());
        if (root instanceof TabPane tabs) tabs.getTabs().forEach(tab -> { result.add(tab.getText()); result.addAll(texts(tab.getContent())); });
        else if (root instanceof javafx.scene.control.SplitPane split) split.getItems().forEach(item -> result.addAll(texts(item)));
        else if (root instanceof javafx.scene.control.ScrollPane scroll && scroll.getContent() != null) result.addAll(texts(scroll.getContent()));
        else if (root instanceof Parent parent) parent.getChildrenUnmodifiable().forEach(child -> result.addAll(texts(child)));
        return result;
    }

    private static Button findButton(Parent root, String text) {
        return root.getChildrenUnmodifiable().stream()
                .map(node -> findButton(node, text))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Button not found: " + text));
    }

    private static Button findButton(Node node, String text) {
        if (node instanceof Button button && text.equals(button.getText())) return button;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Button found = findButton(child, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void await(Duration timeout, BooleanSupplier condition) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) return;
            try {
                Thread.sleep(25);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for JavaFX", interrupted);
            }
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
        if (failure.get() != null) throw new AssertionError("JavaFX action failed", failure.get());
        return result.get();
    }

    private static LabelTemplate template() {
        var page = new PaperPresetRegistry().find("a4-3x7-63.5x38.1").orElseThrow().page();
        var name = new TextElement(ElementFrame.create("name", new RectMm(1, 1, 50, 7), 0),
                "{{product_name}} — {{price}}", "Arial", 10, "#000000", false, false, TextAlignment.CENTER);
        var barcode = new BarcodeElement(ElementFrame.create("barcode", new RectMm(1, 9, 50, 20), 1),
                "{{sku}}", Symbology.CODE_128, 0.25, 2, true);
        return LabelTemplate.create("Products", page, List.of(name, barcode));
    }
}
