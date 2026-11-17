package com.mohamed.barcodelabel.fx.workbench;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Starts the JavaFX toolkit once and runs test code on its thread. */
final class FxSupport {
    private FxSupport() {}

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
            } catch (Throwable error) {
                failure.set(error);
            } finally {
                done.countDown();
            }
        });
        try {
            assertTrue(done.await(15, TimeUnit.SECONDS), "JavaFX action timed out");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
        if (failure.get() instanceof AssertionError assertion) throw assertion;
        if (failure.get() != null) throw new AssertionError("JavaFX action failed", failure.get());
        return result.get();
    }

    static void runFx(Runnable action) {
        onFx(() -> {
            action.run();
            return null;
        });
    }

    static void await(Duration timeout, BooleanSupplier condition) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (onFx(condition::getAsBoolean)) return;
            try {
                Thread.sleep(25);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError(interrupted);
            }
        }
        throw new AssertionError("Timed out waiting for JavaFX");
    }

    /** Every node under {@code root}, including tab, split pane and scroll pane content, which need no skin. */
    static Stream<Node> all(Node root) {
        Stream<Node> children = Stream.empty();
        if (root instanceof TabPane tabs) children = tabs.getTabs().stream().map(tab -> tab.getContent());
        else if (root instanceof SplitPane split) children = split.getItems().stream();
        else if (root instanceof ScrollPane scroll && scroll.getContent() != null) children = Stream.of(scroll.getContent());
        else if (root instanceof Parent parent) children = parent.getChildrenUnmodifiable().stream();
        return Stream.concat(Stream.of(root), children.filter(java.util.Objects::nonNull).flatMap(FxSupport::all));
    }

    /** Fails on any visible text with Arabic letters under {@code root}: a text that was not translated. */
    static void assertNoArabic(Node root) {
        all(root).forEach(node -> {
            if (node instanceof javafx.scene.control.TabPane tabs) tabs.getTabs().forEach(tab -> assertNoArabic(tab.getText()));
            if (node instanceof javafx.scene.control.Labeled labeled) assertNoArabic(labeled.getText());
            if (node instanceof javafx.scene.control.TextInputControl input) assertNoArabic(input.getPromptText());
            if (node instanceof javafx.scene.control.ComboBoxBase<?> box) assertNoArabic(box.getPromptText());
            if (node instanceof javafx.scene.control.Control control && control.getTooltip() != null) {
                assertNoArabic(control.getTooltip().getText());
            }
        });
    }

    static void assertNoArabic(String text) {
        if (text != null && text.codePoints().anyMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.ARABIC)) {
            throw new AssertionError("Arabic text in the English UI: " + text);
        }
    }
}
