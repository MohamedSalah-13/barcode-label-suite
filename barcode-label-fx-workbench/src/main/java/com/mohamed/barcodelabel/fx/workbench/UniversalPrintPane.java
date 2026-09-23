package com.mohamed.barcodelabel.fx.workbench;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.page.PageMode;
import com.mohamed.barcodelabel.model.unit.UnitConversions;
import com.mohamed.barcodelabel.print.LabelPrintRequest;
import com.mohamed.barcodelabel.print.java2d.Java2DLabelPrinter;
import com.mohamed.barcodelabel.print.java2d.PrintCancelledException;
import com.mohamed.barcodelabel.print.java2d.PrintJobFailedException;
import com.mohamed.barcodelabel.print.java2d.PrinterCatalog;
import com.mohamed.barcodelabel.print.java2d.PrinterNotFoundException;
import com.mohamed.barcodelabel.render.RenderRequest;
import com.mohamed.barcodelabel.render.java2d.BufferedImageLabelRenderer;
import com.mohamed.barcodelabel.workflow.print.PlannedPage;
import com.mohamed.barcodelabel.workflow.print.UniversalPagePlanner;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class UniversalPrintPane extends BorderPane {
    private final ObjectProperty<LabelTemplate> template = new SimpleObjectProperty<>(this, "template");
    private final ObjectProperty<List<Map<String, ?>>> records = new SimpleObjectProperty<>(this, "records", List.of());
    private final ComboBox<String> printers = new ComboBox<>();
    private final Spinner<Integer> copies = new Spinner<>(1, 999, 1);
    private final Spinner<Integer> startSlot = new Spinner<>(0, 0, 0);
    private final ImageView preview = new ImageView();
    private final Label status = new Label("جاهز");
    private final Label pageStatus = new Label("صفحة 0 من 0");
    private final Button previous = new Button("السابق");
    private final Button next = new Button("التالي");
    private final Button print = new Button("طباعة");
    private final Button cancelPrint = new Button("إلغاء الطباعة");
    private final CheckBox allPages = new CheckBox("طباعة كل الصفحات");
    private final Spinner<Integer> fromPage = new Spinner<>(1, 1, 1);
    private final Spinner<Integer> toPage = new Spinner<>(1, 1, 1);
    private final UniversalPagePlanner planner = new UniversalPagePlanner();
    private final BufferedImageLabelRenderer renderer = new BufferedImageLabelRenderer();
    private final PrinterCatalog printerCatalog = new PrinterCatalog();
    private final Java2DLabelPrinter labelPrinter = new Java2DLabelPrinter();
    private final AtomicLong generation = new AtomicLong();
    private int pageIndex;
    private List<PlannedPage> pages = List.of();
    private List<Map<String, ?>> previewRecords = List.of();

    public UniversalPrintPane() {
        getStyleClass().add("universal-print-pane");
        preview.setPreserveRatio(true); preview.setSmooth(true); preview.setFitHeight(670);
        ScrollPane scroll = new ScrollPane(preview); scroll.setFitToWidth(true); scroll.setPannable(true);
        setCenter(scroll); setRight(settings()); setBottom(navigation());
        template.addListener((obs, oldValue, value) -> reset());
        records.addListener((obs, oldValue, value) -> reset());
        startSlot.valueProperty().addListener((obs, oldValue, value) -> resetPreview());
        previous.setOnAction(event -> move(-1)); next.setOnAction(event -> move(1)); print.setOnAction(event -> print());
        loadPrinters(); updateNavigation();
    }

    public LabelTemplate getTemplate() { return template.get(); }
    public void setTemplate(LabelTemplate value) { template.set(value); }
    public ObjectProperty<LabelTemplate> templateProperty() { return template; }
    public List<Map<String, ?>> getRecords() { return records.get(); }
    public void setRecords(List<Map<String, ?>> value) { records.set(value == null ? List.of() : List.copyOf(value)); }
    public ObjectProperty<List<Map<String, ?>>> recordsProperty() { return records; }

    private VBox settings() {
        printers.setPromptText("اختر الطابعة"); printers.setMaxWidth(Double.MAX_VALUE);
        copies.setEditable(true); commitOnFocusLoss(copies);
        startSlot.setEditable(true); commitOnFocusLoss(startSlot);
        fromPage.setEditable(true); commitOnFocusLoss(fromPage);
        toPage.setEditable(true); commitOnFocusLoss(toPage);
        allPages.setSelected(true);
        fromPage.setDisable(true); toPage.setDisable(true);
        allPages.selectedProperty().addListener((obs, oldValue, selected) -> {
            fromPage.setDisable(selected); toPage.setDisable(selected);
        });
        HBox range = new HBox(6, new Label("من"), fromPage, new Label("إلى"), toPage);
        range.setAlignment(Pos.CENTER_LEFT);
        print.getStyleClass().add("primary-button"); print.setMaxWidth(Double.MAX_VALUE);
        cancelPrint.setMaxWidth(Double.MAX_VALUE); cancelPrint.setDisable(true);
        cancelPrint.setOnAction(event -> { labelPrinter.cancelCurrent(); status.setText("جارٍ إلغاء الطباعة..."); });
        VBox box = new VBox(8, new Label("الطابعة"), printers, new Label("عدد النسخ"), copies,
                new Label("ابدأ من الخانة (A4 فقط)"), startSlot, allPages, range, print, cancelPrint, status);
        box.getStyleClass().add("print-settings"); box.setPrefWidth(270); return box;
    }

    private static void commitOnFocusLoss(Spinner<Integer> spinner) {
        spinner.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (!isFocused) spinner.increment(0);
        });
    }

    private HBox navigation() {
        HBox box = new HBox(12, previous, pageStatus, next); box.setAlignment(Pos.CENTER); box.setPadding(new Insets(10)); return box;
    }

    private void reset() {
        int max = getTemplate() != null && getTemplate().page().mode() == PageMode.SHEET
                ? getTemplate().page().sheetLayout().labelsPerPage() - 1 : 0;
        startSlot.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Math.max(0, max), 0));
        startSlot.setDisable(max == 0); resetPreview();
    }

    private void resetPreview() {
        pageIndex = 0;
        if (getTemplate() == null) {
            generation.incrementAndGet();
            pages = List.of();
            previewRecords = List.of();
            preview.setImage(null);
            status.setText("اختر قالبًا");
            updateNavigation();
            updatePrintAvailability();
            return;
        }
        previewRecords = getRecords().isEmpty() ? List.of(Map.of()) : getRecords();
        try {
            pages = planner.plan(getTemplate().page(), previewRecords.size(), startSlot.getValue());
        } catch (RuntimeException exception) {
            generation.incrementAndGet();
            pages = List.of();
            preview.setImage(null);
            status.setText("تعذر تخطيط الصفحات: " + rootMessage(exception));
            updateNavigation();
            updatePrintAvailability();
            return;
        }
        int pageMaximum = Math.max(1, pages.size());
        fromPage.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, pageMaximum, 1));
        toPage.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, pageMaximum, pageMaximum));
        updatePrintAvailability();
        renderPreview();
    }

    private void renderPreview() {
        updateNavigation();
        if (pages.isEmpty()) return;
        long request = generation.incrementAndGet(); int requestedPage = pageIndex;
        LabelTemplate currentTemplate = getTemplate(); List<Map<String, ?>> currentRecords = previewRecords; PlannedPage page = pages.get(requestedPage);
        boolean templateOnly = getRecords().isEmpty();
        status.setText(templateOnly ? "جارٍ إنشاء معاينة القالب..." : "جارٍ إنشاء المعاينة...");
        Thread.ofVirtual().name("universal-label-preview").start(() -> {
            try {
                BufferedImage image = renderPage(currentTemplate, currentRecords, page, 96, true);
                Platform.runLater(() -> { if (generation.get() == request) { preview.setImage(SwingFXUtils.toFXImage(image, null)); status.setText(templateOnly ? "معاينة القالب — استورد بيانات CSV للطباعة" : "المعاينة جاهزة"); } });
            } catch (Exception exception) {
                Platform.runLater(() -> { if (generation.get() == request) { preview.setImage(null); status.setText("خطأ في المعاينة: " + rootMessage(exception)); } });
            }
        });
    }

    private BufferedImage renderPage(LabelTemplate currentTemplate, List<Map<String, ?>> currentRecords,
                                     PlannedPage page, double dpi, boolean guides) throws Exception {
        int width = pixels(currentTemplate.page().pageSize().width(), dpi);
        int height = pixels(currentTemplate.page().pageSize().height(), dpi);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, width, height);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            for (var placement : page.placements()) {
                BufferedImage label = renderer.render(new RenderRequest(currentTemplate, currentRecords.get(placement.recordIndex()), dpi));
                int x = pixels(placement.bounds().x(), dpi); int y = pixels(placement.bounds().y(), dpi);
                graphics.drawImage(label, x, y, null);
                if (guides) { graphics.setColor(new Color(37, 99, 235, 130)); graphics.setStroke(new BasicStroke(1)); graphics.drawRect(x, y, label.getWidth() - 1, label.getHeight() - 1); }
            }
        } finally { graphics.dispose(); }
        return image;
    }

    private void move(int delta) { int target = pageIndex + delta; if (target >= 0 && target < pages.size()) { pageIndex = target; renderPreview(); } }
    private void updateNavigation() { pageStatus.setText(pages.isEmpty() ? "صفحة 0 من 0" : "صفحة " + (pageIndex + 1) + " من " + pages.size()); previous.setDisable(pageIndex <= 0); next.setDisable(pages.isEmpty() || pageIndex >= pages.size() - 1); }

    private void loadPrinters() {
        status.setText("جارٍ اكتشاف الطابعات...");
        Thread.ofVirtual().name("universal-printer-discovery").start(() -> {
            List<String> names = printerCatalog.discover().stream().map(descriptor -> descriptor.name()).toList();
            Platform.runLater(() -> {
                printers.setItems(FXCollections.observableArrayList(names));
                if (!names.isEmpty()) printers.getSelectionModel().selectFirst();
                updatePrintAvailability();
                if (getTemplate() == null) status.setText(names.isEmpty() ? "لم يتم العثور على طابعات" : "تم العثور على " + names.size() + " طابعة");
            });
        });
    }

    private void updatePrintAvailability() {
        print.setDisable(printers.getItems().isEmpty() || getTemplate() == null || getRecords().isEmpty() || pages.isEmpty());
    }

    private void print() {
        if (printers.getValue() == null || getTemplate() == null || getRecords().isEmpty() || pages.isEmpty()) return;
        boolean restrictRange = !allPages.isSelected();
        int first = restrictRange ? fromPage.getValue() : 0;
        int last = restrictRange ? toPage.getValue() : 0;
        if (restrictRange && first > last) {
            status.setText("صفحة البداية يجب ألا تتجاوز صفحة النهاية");
            return;
        }
        String rangeText = restrictRange ? "\nالصفحات: " + first + " إلى " + last : "";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "إرسال " + getRecords().size() + " ملصق إلى:\n" + printers.getValue() + rangeText,
                ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText("تأكيد الطباعة"); if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        print.setDisable(true); cancelPrint.setDisable(false); status.setText("جارٍ إرسال مهمة الطباعة...");
        var request = new LabelPrintRequest(getTemplate(), getRecords(), printers.getValue(),
                copies.getValue(), startSlot.getValue(), first, last);
        labelPrinter.print(request).whenComplete((result, error) -> Platform.runLater(() -> {
            print.setDisable(false); cancelPrint.setDisable(true);
            if (error == null) {
                status.setText("تمت طباعة " + result.labelsPrinted() + " ملصق في " + result.pagesPrinted() + " صفحة");
            } else {
                status.setText(describePrintError(error));
            }
        }));
    }

    private static String describePrintError(Throwable throwable) {
        Throwable error = unwrap(throwable);
        if (error instanceof PrintCancelledException) return "تم إلغاء الطباعة: " + rootMessage(error);
        if (error instanceof PrinterNotFoundException) return "الطابعة المحددة غير متاحة: " + rootMessage(error);
        if (error instanceof PrintJobFailedException failed) {
            return "فشلت الطباعة بعد " + failed.pagesCompleted() + " من " + failed.totalPages() + " صفحة: " + rootMessage(error);
        }
        return "فشلت الطباعة: " + rootMessage(error);
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof java.util.concurrent.CompletionException
                || current instanceof java.util.concurrent.ExecutionException) && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
    private static int pixels(double mm, double dpi) { return Math.max(1, (int) Math.round(UnitConversions.mmToPixels(mm, dpi))); }
    private static String rootMessage(Throwable throwable) { Throwable current = throwable; while (current.getCause() != null) current = current.getCause(); return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage(); }
}
