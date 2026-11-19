package com.mohamed.barcodelabel.fx.workbench;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.Symbology;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.TextAlignment;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.print.LabelPrintRequest;
import com.mohamed.barcodelabel.print.PrintResult;
import com.mohamed.barcodelabel.workflow.paper.PaperPresetRegistry;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import com.mohamed.barcodelabel.fx.i18n.UiLanguage;
import com.mohamed.barcodelabel.fx.i18n.UiTexts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniversalPrintPaneTest {
    private static final UiTexts ARABIC = UiTexts.load(UniversalPrintPane.class, "messages", UiLanguage.ARABIC);
    private static final UiTexts ENGLISH = UiTexts.load(UniversalPrintPane.class, "messages", UiLanguage.ENGLISH);

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
    void rendersAndNavigatesAPlannedMultiPageSheet() {
        UniversalPrintPane pane = onFx(UniversalPrintPane::new);
        List<Map<String, ?>> records = java.util.stream.IntStream.range(0, 22)
                .<Map<String, ?>>mapToObj(index -> Map.of("name", "Item " + index))
                .toList();

        onFx(() -> {
            pane.setTemplate(template());
            pane.setRecords(records);
            return null;
        });

        await(Duration.ofSeconds(15), () -> onFx(() -> imageView(pane).getImage() != null));
        onFx(() -> {
            assertEquals("صفحة 1 من 2", label(pane, "صفحة 1 من 2").getText());
            Button next = button(pane, "التالي");
            assertTrue(!next.isDisabled());
            next.fire();
            return null;
        });

        await(Duration.ofSeconds(15), () -> onFx(() -> labelOrNull(pane, "صفحة 2 من 2") != null
                && imageView(pane).getImage() != null));
        onFx(() -> {
            assertEquals("صفحة 2 من 2", label(pane, "صفحة 2 من 2").getText());
            assertTrue(button(pane, "التالي").isDisabled());
            assertNotNull(imageView(pane).getImage());
            return null;
        });
    }

    @Test
    void withoutCsvOneLabelIsPreviewedFromTypedValues() {
        UniversalPrintPane pane = onFx(UniversalPrintPane::new);
        onFx(() -> {
            pane.setTemplate(template());
            pane.setRecords(null);
            assertTrue(pane.getRecords().isEmpty());
            assertTrue(label(pane, "بيانات الملصق").getParent().isVisible());
            assertEquals(List.of("name"), pane.blankManualFields());
            // A blank field keeps its placeholder in the preview, so the label still renders.
            assertEquals(Map.of("name", "{{name}}"), pane.manualRecord(true));
            assertEquals(Map.of("name", ""), pane.manualRecord(false));
            return null;
        });

        await(Duration.ofSeconds(15), () -> onFx(() -> imageView(pane).getImage() != null));
        onFx(() -> {
            assertEquals("صفحة 1 من 1", label(pane, "صفحة 1 من 1").getText());
            manualField(pane, "name").setText("  Milk 1L ");
            assertEquals(List.of(), pane.blankManualFields());
            assertEquals(Map.of("name", "Milk 1L"), pane.manualRecord(false));
            return null;
        });
    }

    @Test
    void theTypedLabelDataSectionIsInEnglish() {
        UniversalPrintPane pane = onFx(() -> new UniversalPrintPane(UiLanguage.ENGLISH));
        onFx(() -> {
            pane.setTemplate(template());
            assertTrue(label(pane, "Label data").getParent().isVisible());
            assertNotNull(find(pane, Label.class, node -> node.getText().startsWith("Without a CSV file")));
            return null;
        });
    }

    @Test
    void typedValuesSurviveADesignChangeAndHideOnceCsvIsLoaded() {
        UniversalPrintPane pane = onFx(UniversalPrintPane::new);
        onFx(() -> {
            pane.setTemplate(template());
            manualField(pane, "name").setText("Milk");
            pane.setTemplate(LabelTemplate.create("Renamed", template().page(), template().elements()));
            assertEquals("Milk", manualField(pane, "name").getText());

            pane.setRecords(List.of(Map.of("name", "From CSV")));
            assertTrue(!label(pane, "بيانات الملصق").getParent().isVisible());
            return null;
        });
    }

    @Test
    void sheetSlotsAreNumberedFromOneLikeTheUserCountsThem() {
        UniversalPrintPane pane = onFx(UniversalPrintPane::new);
        // 21 labels on the 3 x 7 sheet: starting at slot 1 they fill exactly one page.
        List<Map<String, ?>> records = java.util.stream.IntStream.range(0, 21)
                .<Map<String, ?>>mapToObj(index -> Map.of("name", "Item " + index))
                .toList();
        onFx(() -> {
            pane.setTemplate(template());
            pane.setRecords(records);
            Spinner<?> slot = startSlot(pane);
            assertEquals(1, slot.getValue());
            assertEquals(0, pane.startSlotIndex());
            assertEquals("صفحة 1 من 1", label(pane, "صفحة 1 من 1").getText());

            // The last of the 21 slots can be chosen; then only one label fits on the first page.
            @SuppressWarnings("unchecked")
            var factory = (SpinnerValueFactory.IntegerSpinnerValueFactory) slot.getValueFactory();
            assertEquals(21, factory.getMax());
            factory.setValue(21);
            assertEquals(20, pane.startSlotIndex());
            assertEquals("صفحة 1 من 2", label(pane, "صفحة 1 من 2").getText());
            return null;
        });
    }

    @Test
    void directCsvChecksRowsBeyondThePreviewAndReleasesPrintingAfterCorrection() {
        var pane = onFx(() -> new UniversalPrintPane(UiLanguage.ENGLISH, request -> {
            throw new AssertionError("An invalid batch reached the printer");
        }));
        var records = new java.util.ArrayList<Map<String, ?>>();
        for (int i = 0; i < 21; i++) records.add(Map.of("Code", "A1"));
        records.add(Map.of("Code", " "));
        onFx(() -> {
            var barcode = new BarcodeElement(
                    ElementFrame.create("SKU", new RectMm(1, 1, 55, 20), 0), "{{Code}}",
                    Symbology.CODE_128, 0.25, 2, true);
            pane.setTemplate(template().withElements(List.of(barcode)));
            pane.setRecords(records);
            return null;
        });
        await(Duration.ofSeconds(10), () -> onFx(() -> pane.getTop() instanceof ListView<?> list
                && list.getItems().contains("Row 23: Column Code — the CODE_128 barcode value is blank")));
        onFx(() -> {
            @SuppressWarnings("unchecked")
            ComboBox<String> printers = find(pane, ComboBox.class, box -> true);
            printers.getItems().setAll("Test printer");
            printers.setValue("Test printer");
            assertTrue(button(pane, ENGLISH.get("print.print")).isDisabled());
            pane.submitPrint(new LabelPrintRequest(pane.getTemplate(), records, "Test printer", 1, 0));
            records.set(21, Map.of("Code", "B2"));
            pane.setRecords(records);
            return null;
        });
        await(Duration.ofSeconds(10), () -> onFx(() -> pane.getTop() == null));
        onFx(() -> {
            @SuppressWarnings("unchecked")
            ComboBox<String> printers = find(pane, ComboBox.class, box -> true);
            printers.getItems().setAll("Test printer");
            printers.setValue("Test printer");
            find(pane, Spinner.class, spinner -> spinner.getTooltip() != null).increment();
            assertFalse(button(pane, ENGLISH.get("print.print")).isDisabled());
            return null;
        });
    }

    @Test
    void cachedChecksRevalidateMutableInputAndChangedSymbology() {
        var pane = onFx(() -> new UniversalPrintPane(UiLanguage.ENGLISH));
        var row = new java.util.HashMap<String, String>();
        row.put("Code", "ABC");
        var records = List.<Map<String, ?>>of(row);
        var barcode = new BarcodeElement(ElementFrame.create("SKU", new RectMm(1, 1, 55, 20), 0),
                "{{Code}}", Symbology.CODE_128, .25, 2, true);
        var initial = template().withElements(List.of(barcode));
        onFx(() -> { pane.setTemplate(initial); pane.setRecords(records); return null; });
        await(Duration.ofSeconds(10), () -> onFx(() -> pane.getTop() == null));
        onFx(() -> { row.put("Code", " "); pane.setRecords(records); return null; });
        await(Duration.ofSeconds(10), () -> onFx(() -> pane.getTop() instanceof ListView<?> list
                && list.getItems().contains("Row 2: Column Code — the CODE_128 barcode value is blank")));
        onFx(() -> { row.put("Code", "ABC"); pane.setRecords(records); return null; });
        await(Duration.ofSeconds(10), () -> onFx(() -> pane.getTop() == null));
        onFx(() -> {
            var numeric = new BarcodeElement(barcode.frame(), "{{Code}}", Symbology.EAN_13, .25, 2, true);
            pane.setTemplate(initial.withElements(List.of(numeric)));
            return null;
        });
        await(Duration.ofSeconds(10), () -> onFx(() -> pane.getTop() instanceof ListView<?> list
                && list.getItems().stream().anyMatch(item -> item.toString().contains("EAN_13"))));
        onFx(() -> { pane.setTemplate(initial); return null; });
        await(Duration.ofSeconds(10), () -> onFx(() -> pane.getTop() == null));
    }

    @Test
    void changingTheStartSlotCannotEnableAnotherPrintWhileAJobIsActive() {
        var pending = new CompletableFuture<PrintResult>();
        var submissions = new AtomicInteger();
        var pane = onFx(() -> new UniversalPrintPane(UiLanguage.ARABIC, request -> {
            submissions.incrementAndGet();
            return pending;
        }));
        onFx(() -> {
            // Supply a printer in memory, so this test also exercises a machine with no installed printers.
            @SuppressWarnings("unchecked")
            ComboBox<String> printers = find(pane, ComboBox.class, box -> true);
            printers.getItems().setAll("Test printer");
            printers.setValue("Test printer");
            pane.setTemplate(template());
            var records = List.<Map<String, ?>>of(Map.of("name", "Pen"));
            pane.setRecords(records);
            var request = new LabelPrintRequest(pane.getTemplate(), records, printers.getValue(), 1, 0);
            var print = button(pane, ARABIC.get("print.print"));
            assertFalse(print.isDisabled());
            pane.submitPrint(request);
            startSlot(pane).increment();
            assertEquals(1, pane.startSlotIndex());
            assertTrue(print.isDisabled());
            assertFalse(button(pane, ARABIC.get("print.cancel")).isDisabled());
            pane.submitPrint(request);
            assertEquals(1, submissions.get());
            pane.setTemplate(null);
            pending.complete(new PrintResult(1, 1));
            return null;
        });
        onFx(() -> {
            assertFalse(pane.printingProperty().get());
            assertTrue(button(pane, ARABIC.get("print.cancel")).isDisabled());
            assertTrue(button(pane, ARABIC.get("print.print")).isDisabled(), "the template was cleared during printing");
            return null;
        });
    }

    @Test
    void printConfirmationCountsCopiesAndPagesInArabic() {
        assertEquals("سيُطبع 10 ملصقات على صفحة واحدة\nالطابعة: Office",
                UniversalPrintPane.confirmation(ARABIC, "Office", 10, 1, 1, 0, 0));
        assertEquals("سيُطبع 30 ملصقاً على 3 صفحات\n(3 نسخ، في كل نسخة 10 ملصقات)\nالطابعة: Office",
                UniversalPrintPane.confirmation(ARABIC, "Office", 10, 1, 3, 0, 0));
        assertEquals("سيُطبع ملصقان على صفحتين\nالصفحات من 2 إلى 3\nالطابعة: Office",
                UniversalPrintPane.confirmation(ARABIC, "Office", 2, 2, 1, 2, 3));
    }

    @Test
    void printConfirmationInEnglish() {
        assertEquals("1 label will print on 1 page\nPrinter: Office",
                UniversalPrintPane.confirmation(ENGLISH, "Office", 1, 1, 1, 0, 0));
        assertEquals("30 labels will print on 3 pages\n(3 copies, 10 labels in each)\nPages 2 to 2\nPrinter: Office",
                UniversalPrintPane.confirmation(ENGLISH, "Office", 10, 1, 3, 2, 2));
    }

    @Test
    void workbenchKeepsThePrintTemplateSynchronizedWithDesignerChanges() {
        LabelTemplate initial = template();
        BarcodeLabelWorkbench workbench = onFx(() -> new BarcodeLabelWorkbench(initial));
        LabelTemplate updated = LabelTemplate.create("Updated preview", initial.page(), initial.elements());

        onFx(() -> {
            assertEquals(initial, workbench.printPane().getTemplate());
            workbench.setTemplate(updated);
            assertEquals(updated, workbench.printPane().getTemplate());
            return null;
        });
    }

    private static LabelTemplate template() {
        var page = new PaperPresetRegistry().find("a4-3x7-63.5x38.1").orElseThrow().page();
        var text = new TextElement(ElementFrame.create("name", new RectMm(2, 2, 55, 8), 0),
                "{{name}}", "Arial", 10, "#000000", false, false, TextAlignment.CENTER);
        return LabelTemplate.create("Preview test", page, List.of(text));
    }

    private static Spinner<?> startSlot(Parent root) {
        Spinner<?> found = find(root, Spinner.class, node -> node.getTooltip() != null
                && node.getTooltip().getText().startsWith("رقم أول خانة"));
        if (found == null) throw new AssertionError("Start slot spinner not found");
        return found;
    }

    private static TextField manualField(Parent root, String name) {
        TextField found = find(root, TextField.class, node -> name.equals(node.getPromptText()));
        if (found == null) throw new AssertionError("Manual field not found: " + name);
        return found;
    }

    private static Button button(Parent root, String text) {
        Button found = find(root, Button.class, node -> text.equals(node.getText()));
        if (found == null) throw new AssertionError("Button not found: " + text);
        return found;
    }

    private static Label label(Parent root, String text) {
        Label found = labelOrNull(root, text);
        if (found == null) throw new AssertionError("Label not found: " + text);
        return found;
    }

    private static Label labelOrNull(Parent root, String text) {
        return find(root, Label.class, node -> text.equals(node.getText()));
    }

    private static ImageView imageView(Parent root) {
        if (root instanceof UniversalPrintPane pane
                && pane.getCenter() instanceof ScrollPane scroll
                && scroll.getContent() instanceof ImageView image) {
            return image;
        }
        ImageView found = find(root, ImageView.class, node -> true);
        if (found == null) throw new AssertionError("Preview image was not found");
        return found;
    }

    private static <T extends Node> T find(Node node, Class<T> type, java.util.function.Predicate<T> predicate) {
        if (type.isInstance(node)) {
            T match = type.cast(node);
            if (predicate.test(match)) return match;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                T found = find(child, type, predicate);
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
            if (!completed.await(15, TimeUnit.SECONDS)) throw new AssertionError("JavaFX action timed out");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for JavaFX", interrupted);
        }
        if (failure.get() != null) throw new AssertionError("JavaFX action failed", failure.get());
        return result.get();
    }
}
