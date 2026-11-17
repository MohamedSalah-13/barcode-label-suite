package com.mohamed.barcodelabel.app;

import com.mohamed.barcodelabel.batch.TemplateFields;
import com.mohamed.barcodelabel.fx.batch.BatchProductionWorkbench;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.render.RenderRequest;
import com.mohamed.barcodelabel.render.java2d.BufferedImageLabelRenderer;
import javafx.application.Platform;
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
    void theAppOpensTheProductionWorkbenchInATitledWindow() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyStarted) {
            started.countDown();
        }
        assertTrue(started.await(10, TimeUnit.SECONDS), "JavaFX toolkit did not start");
        Platform.setImplicitExit(false);

        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Stage> shown = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                new BarcodeLabelSuiteApp().start(stage);
                shown.set(stage);
            } catch (Throwable error) {
                failure.set(error);
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(15, TimeUnit.SECONDS), "the app did not start");
        if (failure.get() != null) throw new AssertionError("start failed", failure.get());

        Stage stage = shown.get();
        assertEquals("Barcode Label Suite", stage.getTitle());
        assertTrue(stage.isShowing());
        assertInstanceOf(BatchProductionWorkbench.class, stage.getScene().getRoot());
        Platform.runLater(() -> List.copyOf(Window.getWindows()).forEach(Window::hide));
    }
}
