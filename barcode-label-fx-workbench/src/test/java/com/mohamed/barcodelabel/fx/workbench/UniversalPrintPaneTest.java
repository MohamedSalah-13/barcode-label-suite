package com.mohamed.barcodelabel.fx.workbench;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.TextAlignment;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.workflow.paper.PaperPresetRegistry;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniversalPrintPaneTest {
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
    void emptyRecordsStillRenderATemplatePreviewButCannotBePrinted() {
        UniversalPrintPane pane = onFx(UniversalPrintPane::new);
        onFx(() -> {
            pane.setTemplate(template());
            pane.setRecords(null);
            assertTrue(pane.getRecords().isEmpty());
            return null;
        });

        await(Duration.ofSeconds(15), () -> onFx(() -> imageView(pane).getImage() != null));
        onFx(() -> {
            assertEquals("صفحة 1 من 1", label(pane, "صفحة 1 من 1").getText());
            assertTrue(button(pane, "طباعة").isDisabled());
            assertNotNull(imageView(pane).getImage());
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
    void printConfirmationCountsCopiesAndPagesInArabic() {
        assertEquals("سيُطبع 10 ملصقات على صفحة واحدة\nالطابعة: Office",
                UniversalPrintPane.confirmation("Office", 10, 1, 1, 0, 0));
        assertEquals("سيُطبع 30 ملصقاً على 3 صفحات\n(3 نسخ، في كل نسخة 10 ملصقات)\nالطابعة: Office",
                UniversalPrintPane.confirmation("Office", 10, 1, 3, 0, 0));
        assertEquals("سيُطبع ملصقان على صفحتين\nالصفحات من 2 إلى 3\nالطابعة: Office",
                UniversalPrintPane.confirmation("Office", 2, 2, 1, 2, 3));
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
