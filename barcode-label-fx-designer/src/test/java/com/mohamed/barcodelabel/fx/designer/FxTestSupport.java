package com.mohamed.barcodelabel.fx.designer;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.TextAlignment;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.SheetLayout;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Starts the JavaFX toolkit once and runs test code on the FX application thread. */
final class FxTestSupport {
    static final double LABEL_WIDTH = 63.5;
    static final double LABEL_HEIGHT = 38.1;

    private FxTestSupport() {}

    static void startToolkit() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyStarted) {
            started.countDown();
        }
        assertTrue(started.await(10, TimeUnit.SECONDS), "JavaFX toolkit did not start");
        Platform.setImplicitExit(false);
    }

    static <T> T onFx(Supplier<T> action) {
        if (Platform.isFxApplicationThread()) return action.get();
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(action.get());
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                done.countDown();
            }
        });
        try {
            assertTrue(done.await(15, TimeUnit.SECONDS), "FX action timed out");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
        if (failure.get() instanceof RuntimeException runtime) throw runtime;
        if (failure.get() instanceof Error error) throw error;
        if (failure.get() != null) throw new AssertionError(failure.get());
        return result.get();
    }

    static void runFx(Runnable action) {
        onFx(() -> {
            action.run();
            return null;
        });
    }

    /** A 63.5 x 38.1 mm A4-sheet label with two text elements that do not overlap. */
    static LabelTemplate twoTextTemplate() {
        return template(List.of(
                text("First", new RectMm(2, 3, 20, 6), 0),
                text("Second", new RectMm(30, 12, 20, 6), 1)));
    }

    static LabelTemplate template(List<? extends com.mohamed.barcodelabel.model.element.LabelElement> elements) {
        var layout = new SheetLayout(3, 7, new DimensionsMm(LABEL_WIDTH, LABEL_HEIGHT), 2.5, 0);
        var page = PageDefinition.a4Sheet(layout, new InsetsMm(15.15, 7.25, 15.15, 7.25));
        return LabelTemplate.create("Designer test", page, List.copyOf(elements));
    }

    static TextElement text(String value, RectMm bounds, int z) {
        return new TextElement(ElementFrame.create(value, bounds, z), value, "Arial", 10,
                "#111827", false, false, TextAlignment.LEFT);
    }

    static void key(EventTarget target, KeyCode code, boolean control, boolean shift) {
        Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, shift, control, false, false));
    }

    static void mouse(EventTarget target, javafx.event.EventType<MouseEvent> type, double x, double y, boolean control) {
        boolean pressed = type != MouseEvent.MOUSE_RELEASED;
        Event.fireEvent(target, new MouseEvent(type, x, y, x, y, MouseButton.PRIMARY, 1,
                false, control, false, false, pressed, false, false, false, false, false, null));
    }

    /** Presses at {@code from}, drags by ({@code dx}, {@code dy}) and releases, in scene coordinates. */
    static void drag(Node target, double fromX, double fromY, double dx, double dy) {
        mouse(target, MouseEvent.MOUSE_PRESSED, fromX, fromY, false);
        mouse(target, MouseEvent.MOUSE_DRAGGED, fromX + dx, fromY + dy, false);
        mouse(target, MouseEvent.MOUSE_RELEASED, fromX + dx, fromY + dy, false);
    }
}
