package com.mohamed.barcodelabel.fx.print;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.print.LabelPrintRequest;
import com.mohamed.barcodelabel.print.SheetLayoutEngine;
import com.mohamed.barcodelabel.print.java2d.Java2DLabelPrinter;
import com.mohamed.barcodelabel.print.java2d.PrintCancelledException;
import com.mohamed.barcodelabel.print.java2d.PrintJobFailedException;
import com.mohamed.barcodelabel.print.java2d.PrinterCatalog;
import com.mohamed.barcodelabel.print.java2d.PrinterDescriptor;
import com.mohamed.barcodelabel.print.java2d.PrinterNotFoundException;
import com.mohamed.barcodelabel.print.java2d.SheetPageRenderer;
import com.mohamed.barcodelabel.print.java2d.SheetPreviewRequest;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class SheetPrintPane extends BorderPane {
    private final ObjectProperty<LabelTemplate> template = new SimpleObjectProperty<>(this, "template");
    private final ObjectProperty<List<Map<String, ?>>> records = new SimpleObjectProperty<>(this, "records", List.of());
    private final ComboBox<PrinterDescriptor> printers = new ComboBox<>();
    private final Spinner<Integer> copies = new Spinner<>(1, 999, 1);
    private final Spinner<Integer> startSlot = new Spinner<>(0, 0, 0);
    private final CheckBox allPages = new CheckBox("طباعة كل الصفحات");
    private final Spinner<Integer> fromPageField = new Spinner<>(1, 1, 1);
    private final Spinner<Integer> toPageField = new Spinner<>(1, 1, 1);
    private final Button previousPage = new Button("السابق");
    private final Button nextPage = new Button("التالي");
    private final Button printButton = new Button("طباعة");
    private final Button cancelPrintButton = new Button("إلغاء الطباعة");
    private final Label pageStatus = new Label("صفحة 0 من 0");
    private final Label operationStatus = new Label("جاهز");
    private final Label zoomLabel = new Label("100%");
    private final Slider zoomSlider = new Slider(0.25, 2, 1);
    private final ImageView pageImage = new ImageView();
    private final SheetPageRenderer pageRenderer = new SheetPageRenderer();
    private final SheetLayoutEngine layoutEngine = new SheetLayoutEngine();
    private final PrinterCatalog printerCatalog = new PrinterCatalog();
    private final Java2DLabelPrinter labelPrinter = new Java2DLabelPrinter();
    private final AtomicLong previewGeneration = new AtomicLong();
    private int currentPage;
    private int pageCount;
    private double nativePageWidth;

    public SheetPrintPane() {
        setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        getStyleClass().add("sheet-print-pane");
        pageImage.setPreserveRatio(true);
        pageImage.setSmooth(true);
        ScrollPane previewScroll = new ScrollPane(pageImage);
        previewScroll.setPannable(true);
        setCenter(previewScroll);
        setRight(createSettingsPanel());
        setBottom(createNavigationBar());
        zoomSlider.valueProperty().addListener((observable, oldValue, newValue) -> applyZoom());

        template.addListener((observable, oldValue, newValue) -> {
            configureStartSlot();
            currentPage = 0;
            refreshPreview();
        });
        records.addListener((observable, oldValue, newValue) -> {
            currentPage = 0;
            refreshPreview();
        });
        startSlot.valueProperty().addListener((observable, oldValue, newValue) -> {
            currentPage = 0;
            refreshPreview();
        });
        previousPage.setOnAction(event -> changePage(-1));
        nextPage.setOnAction(event -> changePage(1));
        printButton.setOnAction(event -> confirmAndPrint());
        loadStylesheet();
        loadPrinters();
        updateNavigation();
    }

    public LabelTemplate getTemplate() { return template.get(); }
    public void setTemplate(LabelTemplate value) { template.set(value); }
    public ObjectProperty<LabelTemplate> templateProperty() { return template; }

    public List<Map<String, ?>> getRecords() { return records.get(); }
    public void setRecords(List<Map<String, ?>> value) { records.set(value == null ? List.of() : List.copyOf(value)); }
    public ObjectProperty<List<Map<String, ?>>> recordsProperty() { return records; }

    private VBox createSettingsPanel() {
        printers.setPromptText("اختر الطابعة");
        printers.setMaxWidth(Double.MAX_VALUE);
        Button refreshPrinters = new Button("تحديث");
        refreshPrinters.setOnAction(event -> loadPrinters());
        HBox printerRow = new HBox(6, printers, refreshPrinters);
        HBox.setHgrow(printers, Priority.ALWAYS);
        copies.setEditable(true); commitOnFocusLoss(copies);
        startSlot.setEditable(true); commitOnFocusLoss(startSlot);

        allPages.setSelected(true);
        fromPageField.setEditable(true); commitOnFocusLoss(fromPageField); fromPageField.setDisable(true);
        toPageField.setEditable(true); commitOnFocusLoss(toPageField); toPageField.setDisable(true);
        allPages.selectedProperty().addListener((observable, oldValue, newValue) -> {
            fromPageField.setDisable(newValue);
            toPageField.setDisable(newValue);
        });
        HBox pageRangeRow = new HBox(6, new Label("من"), fromPageField, new Label("إلى"), toPageField);
        pageRangeRow.setAlignment(Pos.CENTER_LEFT);

        printButton.getStyleClass().add("primary-button");
        printButton.setMaxWidth(Double.MAX_VALUE);
        cancelPrintButton.setMaxWidth(Double.MAX_VALUE);
        cancelPrintButton.setDisable(true);
        cancelPrintButton.setOnAction(event -> { labelPrinter.cancelCurrent(); operationStatus.setText("جارٍ إلغاء الطباعة..."); });
        VBox panel = new VBox(8,
                new Label("الطابعة"), printerRow,
                new Label("عدد النسخ"), copies,
                new Label("ابدأ من الخانة"), startSlot,
                allPages, pageRangeRow,
                printButton, cancelPrintButton, operationStatus);
        panel.getStyleClass().add("print-settings");
        panel.setPrefWidth(260);
        return panel;
    }

    private static void commitOnFocusLoss(Spinner<Integer> spinner) {
        spinner.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (!isFocused) spinner.increment(0);
        });
    }

    private HBox createNavigationBar() {
        zoomSlider.setPrefWidth(160);
        Button resetZoom = new Button("100%");
        resetZoom.setOnAction(event -> zoomSlider.setValue(1));
        Label spacer = new Label();
        HBox navigation = new HBox(12, previousPage, pageStatus, nextPage, spacer,
                new Separator(), new Label("التكبير"), zoomSlider, zoomLabel, resetZoom);
        navigation.setAlignment(Pos.CENTER);
        navigation.setPadding(new Insets(10));
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return navigation;
    }

    private void applyZoom() {
        zoomLabel.setText(Math.round(zoomSlider.getValue() * 100) + "%");
        if (pageImage.getImage() != null) {
            pageImage.setFitWidth(nativePageWidth * zoomSlider.getValue());
        }
    }

    private void configureStartSlot() {
        int max = getTemplate() == null ? 0 : Math.max(0, getTemplate().page().sheetLayout().labelsPerPage() - 1);
        startSlot.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, max, 0));
    }

    private void configurePageRange() {
        int max = Math.max(1, pageCount);
        int from = Math.min(fromPageField.getValue(), max);
        int to = Math.min(toPageField.getValue(), max);
        fromPageField.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, max, from));
        toPageField.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, max, Math.max(from, to)));
    }

    private void loadPrinters() {
        operationStatus.setText("جارٍ اكتشاف الطابعات...");
        Thread.ofVirtual().name("printer-discovery").start(() -> {
            var discovered = printerCatalog.discover();
            Platform.runLater(() -> {
                printers.setItems(FXCollections.observableArrayList(discovered));
                if (!discovered.isEmpty()) printers.getSelectionModel().selectFirst();
                operationStatus.setText(discovered.isEmpty() ? "لم يتم العثور على طابعات" : "تم العثور على " + discovered.size() + " طابعة");
                printButton.setDisable(discovered.isEmpty());
            });
        });
    }

    private void refreshPreview() {
        long generation = previewGeneration.incrementAndGet();
        LabelTemplate currentTemplate = getTemplate();
        List<Map<String, ?>> currentRecords = getRecords();
        if (currentTemplate == null || currentRecords.isEmpty()) {
            pageImage.setImage(null);
            pageCount = 0;
            operationStatus.setText(currentTemplate == null ? "اختر قالبًا" : "لا توجد بيانات للطباعة");
            updateNavigation();
            return;
        }
        int start = startSlot.getValue();
        pageCount = layoutEngine.plan(currentTemplate.page(), currentRecords.size(), start).size();
        currentPage = Math.min(currentPage, Math.max(0, pageCount - 1));
        updateNavigation();
        configurePageRange();
        operationStatus.setText("جارٍ إنشاء معاينة الورقة...");
        int requestedPage = currentPage;
        Thread.ofVirtual().name("sheet-preview-render").start(() -> {
            try {
                var request = new SheetPreviewRequest(currentTemplate, currentRecords, start, requestedPage, 96, true);
                var rendered = pageRenderer.render(request);
                var image = SwingFXUtils.toFXImage(rendered, null);
                Platform.runLater(() -> {
                    if (previewGeneration.get() == generation) {
                        pageImage.setImage(image);
                        nativePageWidth = image.getWidth();
                        applyZoom();
                        operationStatus.setText("المعاينة جاهزة");
                    }
                });
            } catch (Exception exception) {
                Platform.runLater(() -> {
                    if (previewGeneration.get() == generation) {
                        pageImage.setImage(null);
                        operationStatus.setText("خطأ في المعاينة: " + exception.getMessage());
                    }
                });
            }
        });
    }

    private void changePage(int delta) {
        int target = currentPage + delta;
        if (target >= 0 && target < pageCount) {
            currentPage = target;
            refreshPreview();
        }
    }

    private void updateNavigation() {
        pageStatus.setText(pageCount == 0 ? "صفحة 0 من 0" : "صفحة " + (currentPage + 1) + " من " + pageCount);
        previousPage.setDisable(currentPage <= 0);
        nextPage.setDisable(pageCount == 0 || currentPage >= pageCount - 1);
    }

    private void confirmAndPrint() {
        PrinterDescriptor selected = printers.getValue();
        if (selected == null || getTemplate() == null || getRecords().isEmpty()) return;
        boolean restrictRange = !allPages.isSelected();
        int firstPage = restrictRange ? fromPageField.getValue() : 0;
        int lastPage = restrictRange ? toPageField.getValue() : 0;
        if (restrictRange && firstPage > lastPage) {
            operationStatus.setText("صفحة البداية يجب ألا تتجاوز صفحة النهاية");
            return;
        }
        String rangeText = restrictRange ? "\nالصفحات: " + firstPage + " إلى " + lastPage : "";
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "سيتم إرسال " + getRecords().size() + " ملصق إلى الطابعة:\n" + selected.name() + rangeText,
                ButtonType.OK, ButtonType.CANCEL);
        confirmation.setHeaderText("تأكيد الطباعة");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        printButton.setDisable(true);
        cancelPrintButton.setDisable(false);
        operationStatus.setText("جارٍ إرسال مهمة الطباعة...");
        var request = new LabelPrintRequest(getTemplate(), getRecords(), selected.name(),
                copies.getValue(), startSlot.getValue(), firstPage, lastPage);
        labelPrinter.print(request).whenComplete((result, error) -> Platform.runLater(() -> {
            printButton.setDisable(false);
            cancelPrintButton.setDisable(true);
            if (error != null) {
                operationStatus.setText(describeError(error));
            } else {
                operationStatus.setText("تمت طباعة " + result.labelsPrinted() + " ملصق في " + result.pagesPrinted() + " صفحة");
            }
        }));
    }

    private static String describeError(Throwable error) {
        error = unwrap(error);
        if (error instanceof PrintCancelledException) {
            return "تم إلغاء الطباعة: " + rootMessage(error);
        }
        if (error instanceof PrinterNotFoundException) {
            return "الطابعة المحددة غير متاحة: " + rootMessage(error);
        }
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

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage();
    }

    private void loadStylesheet() {
        var resource = SheetPrintPane.class.getResource("sheet-print-pane.css");
        if (resource != null) getStylesheets().add(resource.toExternalForm());
    }
}

