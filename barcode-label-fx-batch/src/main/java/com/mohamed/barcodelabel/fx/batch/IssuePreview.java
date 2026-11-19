package com.mohamed.barcodelabel.fx.batch;

import com.mohamed.barcodelabel.batch.BatchValidationIssue;
import com.mohamed.barcodelabel.batch.ColumnMapping;
import com.mohamed.barcodelabel.data.TemplateFieldResolver;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.Symbology;
import com.mohamed.barcodelabel.render.RenderException;
import com.mohamed.barcodelabel.render.RenderRequest;
import com.mohamed.barcodelabel.render.java2d.BarcodeRasterizer;
import com.mohamed.barcodelabel.render.java2d.BufferedImageLabelRenderer;
import com.mohamed.barcodelabel.render.java2d.CompositeBarcodeRasterizer;
import com.mohamed.barcodelabel.workflow.csv.CsvDataset;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import java.awt.image.BufferedImage;
import java.util.Set;

/** Diagnostic view of one source record. It never supplies records to the printer. */
final class IssuePreview extends VBox {
    private final Label caption = new Label();
    private final ImageView image = new ImageView();
    private final BatchTexts texts;
    private Task<BufferedImage> pending;

    IssuePreview(BatchTexts texts) {
        this.texts = texts;
        setSpacing(4);
        image.setPreserveRatio(true);
        image.setFitHeight(160);
        image.fitWidthProperty().bind(widthProperty());
        image.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        getChildren().setAll(caption, image);
        managedProperty().bind(visibleProperty());
        clear();
    }

    void clear() {
        if (pending != null) pending.cancel();
        pending = null;
        image.setImage(null);
        setVisible(false);
    }

    void show(BatchValidationIssue issue, CsvDataset data, ColumnMapping mapping, LabelTemplate template) {
        clear();
        if (issue.kind().configuration()) return;
        int line = issue.sourceRow();
        int index = data.sourceRows().isEmpty() ? line - 2 : data.sourceRows().indexOf(line);
        if (index < 0 || index >= data.rows().size()) return;
        var row = mapping.resolveRow(data.rows().get(index));
        caption.setText(texts.get("batch.issuePreview", line));
        setVisible(true);
        var task = new Task<BufferedImage>() {
            @Override protected BufferedImage call() throws RenderException {
                var renderer = new BufferedImageLabelRenderer(new PreviewBarcodes(), new TemplateFieldResolver());
                var size = template.page().labelSize();
                double dpi = Math.min(150, 1024 * 25.4 / Math.max(size.width(), size.height()));
                return renderer.render(new RenderRequest(template, row, dpi));
            }
        };
        pending = task;
        task.setOnSucceeded(e -> {
            if (pending == task) image.setImage(SwingFXUtils.toFXImage(task.getValue(), null));
        });
        task.setOnFailed(e -> {
            if (pending == task) caption.setText(texts.get("batch.issuePreviewFailed", line));
        });
        Thread.ofVirtual().name("batch-issue-preview").start(task);
    }

    /** Keep the rest of a label visible if a barcode cannot be drawn. */
    private static final class PreviewBarcodes implements BarcodeRasterizer {
        private final BarcodeRasterizer encoder = CompositeBarcodeRasterizer.standard();

        @Override public Set<Symbology> supportedSymbologies() { return encoder.supportedSymbologies(); }

        @Override public BufferedImage rasterize(BarcodeElement element, int width, int height, double dpi) {
            try {
                if (!element.dataTemplate().isBlank()) {
                    var symbol = encoder.rasterize(element, width, height, dpi);
                    if (symbol.getWidth() <= width && symbol.getHeight() <= height) return symbol;
                }
            } catch (RenderException | RuntimeException ignored) {
                // An invalid value is expected in this diagnostic view.
            }
            var marker = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            var g = marker.createGraphics();
            try {
                g.setColor(java.awt.Color.RED);
                g.drawRect(0, 0, width - 1, height - 1);
                g.drawLine(0, 0, width - 1, height - 1);
                g.drawLine(0, height - 1, width - 1, 0);
            } finally {
                g.dispose();
            }
            return marker;
        }
    }
}
