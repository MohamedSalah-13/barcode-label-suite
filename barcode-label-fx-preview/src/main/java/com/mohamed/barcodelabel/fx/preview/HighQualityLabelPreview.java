package com.mohamed.barcodelabel.fx.preview;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.render.RenderRequest;
import com.mohamed.barcodelabel.render.java2d.BufferedImageLabelRenderer;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class HighQualityLabelPreview extends BorderPane {
    private final ObjectProperty<LabelTemplate> template = new SimpleObjectProperty<>(this, "template");
    private final ObjectProperty<Map<String, ?>> data = new SimpleObjectProperty<>(this, "data", Map.of());
    private final DoubleProperty previewDpi = new SimpleDoubleProperty(this, "previewDpi", 150);
    private final ImageView imageView = new ImageView();
    private final Label status = new Label("لا يوجد قالب للمعاينة");
    private final BufferedImageLabelRenderer renderer = new BufferedImageLabelRenderer();
    private final AtomicLong renderGeneration = new AtomicLong();

    public HighQualityLabelPreview() {
        getStyleClass().add("high-quality-label-preview");
        imageView.setPreserveRatio(true);
        imageView.setSmooth(false);
        StackPane surface = new StackPane(imageView, status);
        surface.getStyleClass().add("preview-surface");
        StackPane.setAlignment(status, Pos.CENTER);
        ScrollPane scrollPane = new ScrollPane(surface);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);
        setCenter(scrollPane);

        template.addListener((observable, oldValue, newValue) -> requestRender());
        data.addListener((observable, oldValue, newValue) -> requestRender());
        previewDpi.addListener((observable, oldValue, newValue) -> requestRender());
        loadStylesheet();
    }

    public ObjectProperty<LabelTemplate> templateProperty() {
        return template;
    }

    public LabelTemplate getTemplate() {
        return template.get();
    }

    public void setTemplate(LabelTemplate value) {
        template.set(value);
    }

    public ObjectProperty<Map<String, ?>> dataProperty() {
        return data;
    }

    public Map<String, ?> getData() {
        return data.get();
    }

    public void setData(Map<String, ?> value) {
        data.set(value == null ? Map.of() : Map.copyOf(value));
    }

    public DoubleProperty previewDpiProperty() {
        return previewDpi;
    }

    public double getPreviewDpi() {
        return previewDpi.get();
    }

    public void setPreviewDpi(double value) {
        if (!Double.isFinite(value) || value < 72 || value > 600) {
            throw new IllegalArgumentException("previewDpi must be between 72 and 600");
        }
        previewDpi.set(value);
    }

    private void requestRender() {
        long generation = renderGeneration.incrementAndGet();
        LabelTemplate currentTemplate = getTemplate();
        if (currentTemplate == null) {
            imageView.setImage(null);
            status.setText("لا يوجد قالب للمعاينة");
            status.setVisible(true);
            return;
        }

        Map<String, ?> currentData = getData();
        double currentDpi = getPreviewDpi();
        status.setText("جارٍ إنشاء المعاينة...");
        status.setVisible(true);

        Thread.ofVirtual().name("label-preview-render").start(() -> {
            try {
                var image = renderer.render(new RenderRequest(currentTemplate, currentData, currentDpi));
                var fxImage = SwingFXUtils.toFXImage(image, null);
                Platform.runLater(() -> {
                    if (renderGeneration.get() == generation) {
                        imageView.setImage(fxImage);
                        imageView.setFitWidth(image.getWidth());
                        status.setVisible(false);
                    }
                });
            } catch (Exception exception) {
                Platform.runLater(() -> {
                    if (renderGeneration.get() == generation) {
                        imageView.setImage(null);
                        status.setText("تعذر إنشاء المعاينة: " + exception.getMessage());
                        status.setVisible(true);
                    }
                });
            }
        });
    }

    private void loadStylesheet() {
        var resource = HighQualityLabelPreview.class.getResource("high-quality-preview.css");
        if (resource != null) getStylesheets().add(resource.toExternalForm());
    }
}

