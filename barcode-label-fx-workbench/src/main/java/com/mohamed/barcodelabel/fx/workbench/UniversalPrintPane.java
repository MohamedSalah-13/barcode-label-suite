package com.mohamed.barcodelabel.fx.workbench;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.page.PageMode;
import com.mohamed.barcodelabel.model.page.PageOrientation;
import com.mohamed.barcodelabel.model.unit.UnitConversions;
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
import javafx.print.*;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterJob;
import java.util.Arrays;
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
    private final UniversalPagePlanner planner = new UniversalPagePlanner();
    private final BufferedImageLabelRenderer renderer = new BufferedImageLabelRenderer();
    private final AtomicLong generation = new AtomicLong();
    private int pageIndex;
    private List<PlannedPage> pages = List.of();

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
        copies.setEditable(true); startSlot.setEditable(true); print.getStyleClass().add("primary-button"); print.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(8, new Label("الطابعة"), printers, new Label("عدد النسخ"), copies,
                new Label("ابدأ من الخانة (A4 فقط)"), startSlot, print, status);
        box.getStyleClass().add("print-settings"); box.setPrefWidth(270); return box;
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
        if (getTemplate() == null || getRecords().isEmpty()) {
            pages = List.of(); preview.setImage(null); status.setText(getTemplate() == null ? "اختر قالبًا" : "استورد بيانات CSV"); updateNavigation(); return;
        }
        try { pages = planner.plan(getTemplate().page(), getRecords().size(), startSlot.getValue()); }
        catch (RuntimeException exception) { pages = List.of(); status.setText("تعذر تخطيط الصفحات: " + rootMessage(exception)); }
        renderPreview();
    }

    private void renderPreview() {
        updateNavigation();
        if (pages.isEmpty()) return;
        long request = generation.incrementAndGet(); int requestedPage = pageIndex;
        LabelTemplate currentTemplate = getTemplate(); List<Map<String, ?>> currentRecords = getRecords(); PlannedPage page = pages.get(requestedPage);
        status.setText("جارٍ إنشاء المعاينة...");
        Thread.ofVirtual().name("universal-label-preview").start(() -> {
            try {
                BufferedImage image = renderPage(currentTemplate, currentRecords, page, 96, true);
                Platform.runLater(() -> { if (generation.get() == request) { preview.setImage(SwingFXUtils.toFXImage(image, null)); status.setText("المعاينة جاهزة"); } });
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
            List<String> names = Arrays.stream(PrintServiceLookup.lookupPrintServices(null, null)).map(PrintService::getName).sorted().toList();
            Platform.runLater(() -> { printers.setItems(FXCollections.observableArrayList(names)); if (!names.isEmpty()) printers.getSelectionModel().selectFirst(); print.setDisable(names.isEmpty()); status.setText(names.isEmpty() ? "لم يتم العثور على طابعات" : "تم العثور على " + names.size() + " طابعة"); });
        });
    }

    private void print() {
        if (printers.getValue() == null || getTemplate() == null || pages.isEmpty()) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "إرسال " + getRecords().size() + " ملصق إلى:\n" + printers.getValue(), ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText("تأكيد الطباعة"); if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        print.setDisable(true); status.setText("جارٍ إرسال مهمة الطباعة...");
        LabelTemplate currentTemplate = getTemplate(); List<Map<String, ?>> currentRecords = getRecords(); List<PlannedPage> currentPages = pages;
        int copyCount = copies.getValue(); String printerName = printers.getValue();
        Thread.ofVirtual().name("universal-label-print").start(() -> {
            try {
                printBlocking(currentTemplate, currentRecords, currentPages, copyCount, printerName);
                Platform.runLater(() -> { print.setDisable(false); status.setText("تمت طباعة " + (currentRecords.size() * copyCount) + " ملصق"); });
            } catch (Exception exception) {
                Platform.runLater(() -> { print.setDisable(false); status.setText("فشلت الطباعة: " + rootMessage(exception)); });
            }
        });
    }

    private void printBlocking(LabelTemplate currentTemplate, List<Map<String, ?>> currentRecords,
                               List<PlannedPage> currentPages, int copyCount, String printerName) throws Exception {
        PrintService service = Arrays.stream(PrintServiceLookup.lookupPrintServices(null, null)).filter(item -> item.getName().equals(printerName)).findFirst()
                .orElseThrow(() -> new IllegalStateException("الطابعة لم تعد متاحة"));
        PrinterJob job = PrinterJob.getPrinterJob(); job.setPrintService(service); job.setCopies(copyCount); job.setJobName(currentTemplate.name());
        PageFormat format = pageFormat(currentTemplate); Book book = new Book();
        java.awt.print.Printable printable = (graphics, pageFormat, index) -> {
            if (index < 0 || index >= currentPages.size()) return java.awt.print.Printable.NO_SUCH_PAGE;
            try {
                BufferedImage image = renderPage(currentTemplate, currentRecords, currentPages.get(index), 300, false);
                Graphics2D g = (Graphics2D) graphics.create();
                try { AffineTransform transform = new AffineTransform(); transform.scale(pageFormat.getWidth() / image.getWidth(), pageFormat.getHeight() / image.getHeight()); g.drawImage(image, transform, null); }
                finally { g.dispose(); }
                return java.awt.print.Printable.PAGE_EXISTS;
            } catch (Exception exception) { throw new java.awt.print.PrinterException(rootMessage(exception)); }
        };
        book.append(printable, format, currentPages.size()); job.setPageable(book); job.print();
    }

    private static PageFormat pageFormat(LabelTemplate template) {
        double width = UnitConversions.mmToPoints(template.page().pageSize().width()); double height = UnitConversions.mmToPoints(template.page().pageSize().height());
        Paper paper = new Paper(); paper.setSize(width, height); paper.setImageableArea(0, 0, width, height);
        PageFormat format = new PageFormat(); format.setPaper(paper); format.setOrientation(template.page().orientation() == PageOrientation.LANDSCAPE ? PageFormat.LANDSCAPE : PageFormat.PORTRAIT); return format;
    }
    private static int pixels(double mm, double dpi) { return Math.max(1, (int) Math.round(UnitConversions.mmToPixels(mm, dpi))); }
    private static String rootMessage(Throwable throwable) { Throwable current = throwable; while (current.getCause() != null) current = current.getCause(); return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage(); }
}
