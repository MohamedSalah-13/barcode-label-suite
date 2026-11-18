package com.mohamed.barcodelabel.fx.workbench;

import com.mohamed.barcodelabel.batch.TemplateFields;
import com.mohamed.barcodelabel.batch.BarcodePreflight;
import com.mohamed.barcodelabel.batch.BatchValidationIssue;
import com.mohamed.barcodelabel.fx.i18n.Counted;
import com.mohamed.barcodelabel.fx.i18n.UiLanguage;
import com.mohamed.barcodelabel.fx.i18n.UiTexts;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.page.PageMode;
import com.mohamed.barcodelabel.model.unit.UnitConversions;
import com.mohamed.barcodelabel.print.LabelPrintRequest;
import com.mohamed.barcodelabel.print.LabelPrinter;
import com.mohamed.barcodelabel.print.java2d.Java2DLabelPrinter;
import com.mohamed.barcodelabel.print.java2d.PrintCancelledException;
import com.mohamed.barcodelabel.print.java2d.PrintJobFailedException;
import com.mohamed.barcodelabel.print.java2d.PrinterCatalog;
import com.mohamed.barcodelabel.print.java2d.PrinterNotFoundException;
import com.mohamed.barcodelabel.render.RenderRequest;
import com.mohamed.barcodelabel.render.java2d.BufferedImageLabelRenderer;
import com.mohamed.barcodelabel.workflow.print.PlannedPage;
import com.mohamed.barcodelabel.workflow.print.UniversalPagePlanner;
import com.mohamed.barcodelabel.workflow.data.RepeatedList;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.prefs.Preferences;

public final class UniversalPrintPane extends BorderPane {
    private final UiTexts texts;
    private final BarcodeIssueTexts barcodeTexts;
    private final ListView<String> validation = new ListView<>();
    private boolean batchValidationRequired;
    private List<Integer> sourceRows = List.of();
    private Task<List<String>> preflight;
    private final ObjectProperty<LabelTemplate> template = new SimpleObjectProperty<>(this, "template");
    private final ObjectProperty<List<Map<String, ?>>> records = new SimpleObjectProperty<>(this, "records", List.of());
    private final ComboBox<String> printers = new ComboBox<>();
    private final Spinner<Integer> copies = new Spinner<>(1, 999, 1);
    /** The first free slot on the first sheet, counted from 1 as the user sees it; the planner counts from 0. */
    private final Spinner<Integer> startSlot = new Spinner<>(1, 1, 1);
    private final ImageView preview = new ImageView();
    private final Label status = new Label();
    private final Label pageStatus = new Label();
    private final Button previous = new Button();
    private final Button next = new Button();
    private final Button print = new Button();
    private final Button cancelPrint = new Button();
    private final CheckBox allPages = new CheckBox();
    private final Spinner<Integer> fromPage = new Spinner<>(1, 1, 1);
    private final Spinner<Integer> toPage = new Spinner<>(1, 1, 1);
    /** Without CSV data, one label is printed from the values typed here, one field per {{placeholder}}. */
    private final Map<String, TextField> manualValues = new LinkedHashMap<>();
    private final GridPane manualGrid = new GridPane();
    private final VBox manualBox = new VBox(6);
    private final TemplateFields templateFields = new TemplateFields();
    private final UniversalPagePlanner planner = new UniversalPagePlanner();
    private final BufferedImageLabelRenderer renderer = new BufferedImageLabelRenderer();
    private final PrinterCatalog printerCatalog = new PrinterCatalog();
    private final LabelPrinter labelPrinter;
    private final CalibrationStore calibrations;
    private final ReadOnlyBooleanWrapper printing = new ReadOnlyBooleanWrapper(this, "printing");
    private final AtomicLong generation = new AtomicLong();
    private int pageIndex;
    private List<PlannedPage> pages = List.of();
    private List<Map<String, ?>> previewRecords = List.of();

    /** Print choices and preview position; messages and previews are recreated in the new UI language. */
    public record Session(String printer, int copies, int startSlot, boolean allPages, int fromPage,
                          int toPage, int pageIndex, Map<String, String> manualValues) {
        public Session { manualValues = Map.copyOf(manualValues); }
    }

    public Session snapshotSession() {
        Map<String, String> typed = new LinkedHashMap<>();
        manualValues.forEach((name, field) -> typed.put(name, field.getText()));
        return new Session(printers.getValue(), copies.getValue(), startSlot.getValue(), allPages.isSelected(),
                fromPage.getValue(), toPage.getValue(), pageIndex, typed);
    }

    /** Restore after the template and records, which determine the available slots and pages. */
    public void restoreSession(Session session) {
        printers.setValue(session.printer());
        copies.getValueFactory().setValue(session.copies());
        manualValues.forEach((name, field) -> field.setText(session.manualValues().getOrDefault(name, "")));
        startSlot.getValueFactory().setValue(session.startSlot());
        allPages.setSelected(session.allPages());
        fromPage.getValueFactory().setValue(session.fromPage());
        toPage.getValueFactory().setValue(session.toPage());
        pageIndex = Math.max(0, Math.min(session.pageIndex(), pages.size() - 1));
        renderPreview();
    }

    /** An Arabic print pane. */
    public UniversalPrintPane() {
        this(UiLanguage.ARABIC);
    }

    /** A print pane in this language. It takes the orientation of the pane it is placed in. */
    public UniversalPrintPane(UiLanguage language) {
        this(language, new Java2DLabelPrinter());
    }

    UniversalPrintPane(UiLanguage language, LabelPrinter labelPrinter) {
        this(language, labelPrinter, new CalibrationStore(Preferences.userNodeForPackage(UniversalPrintPane.class)
                .node("printCalibration")));
    }

    UniversalPrintPane(UiLanguage language, LabelPrinter labelPrinter, CalibrationStore calibrations) {
        this.labelPrinter = labelPrinter;
        this.calibrations = calibrations;
        texts = UiTexts.load(UniversalPrintPane.class, "messages", language);
        barcodeTexts = new BarcodeIssueTexts(language);
        status.setText(t("print.ready")); previous.setText(t("print.previous")); next.setText(t("print.next"));
        print.setText(t("print.print")); cancelPrint.setText(t("print.cancel")); allPages.setText(t("print.allPages"));
        getStyleClass().add("universal-print-pane");
        preview.setPreserveRatio(true); preview.setSmooth(true); preview.setFitHeight(670);
        ScrollPane scroll = new ScrollPane(preview); scroll.setFitToWidth(true); scroll.setPannable(true);
        setCenter(scroll); setRight(settings()); setBottom(navigation());
        template.addListener((obs, oldValue, value) -> { rebuildManualFields(); checkBarcodes(); reset(); });
        records.addListener((obs, oldValue, value) -> { updateManualVisibility(); checkBarcodes(); reset(); });
        startSlot.valueProperty().addListener((obs, oldValue, value) -> resetPreview());
        previous.setOnAction(event -> move(-1)); next.setOnAction(event -> move(1)); print.setOnAction(event -> print());
        loadPrinters(); updateNavigation(); updatePrintAvailability();
    }

    public ReadOnlyBooleanProperty printingProperty() { return printing.getReadOnlyProperty(); }

    public LabelTemplate getTemplate() { return template.get(); }
    public void setTemplate(LabelTemplate value) { template.set(value); }
    public ObjectProperty<LabelTemplate> templateProperty() { return template; }
    public List<Map<String, ?>> getRecords() { return records.get(); }
    public void setRecords(List<Map<String, ?>> value) { setRecords(value, List.of()); }

    void setRecords(List<Map<String, ?>> value, List<Integer> csvRows) {
        boolean rowsChanged = !sourceRows.equals(csvRows);
        sourceRows = csvRows;
        List<Map<String, ?>> next = value == null ? List.of() : RepeatedList.copyOf(value);
        if (rowsChanged && next.equals(getRecords())) checkBarcodes();
        if (!next.equals(getRecords())) records.set(next);
    }
    public ObjectProperty<List<Map<String, ?>>> recordsProperty() { return records; }

    /** A pending or failed CSV mapping must not fall back to printing a manual label. */
    public void setBatchValidationRequired(boolean required) {
        if (batchValidationRequired != required) {
            batchValidationRequired = required;
            checkBarcodes();
        }
    }

    private void checkBarcodes() {
        if (preflight != null) preflight.cancel();
        preflight = null;
        if (batchValidationRequired) {
            showValidation(List.of(t("barcode.prepare")));
            return;
        }
        List<Map<String, ?>> data = getRecords();
        if (getTemplate() == null || data.isEmpty()) {
            showValidation(List.of());
            return;
        }
        var check = new BarcodePreflight(getTemplate(), Map.of());
        if (!check.hasBarcodes()) {
            showValidation(List.of());
            return;
        }
        List<Integer> rows = sourceRows;
        var task = new Task<List<String>>() {
            @Override protected List<String> call() {
                List<BatchValidationIssue> issues = new ArrayList<>();
                for (int i = 0; i < data.size() && !isCancelled(); i++) {
                    check.checkRow(data.get(i), rows.size() == data.size() ? rows.get(i) : i + 2, issues);
                }
                return issues.stream().map(issue -> t("barcode.inRow", issue.sourceRow(), barcodeTexts.describe(issue))).toList();
            }
        };
        preflight = task;
        showValidation(List.of(t("barcode.checking")));
        task.setOnSucceeded(event -> {
            if (preflight == task) {
                preflight = null;
                showValidation(task.getValue());
            }
        });
        task.setOnFailed(event -> {
            if (preflight == task) {
                preflight = null;
                showValidation(List.of(t("print.failed", rootMessage(task.getException()))));
            }
        });
        Thread.ofVirtual().name("barcode-preflight").start(task);
    }

    private void showValidation(List<String> messages) {
        validation.getItems().setAll(messages);
        validation.setPrefHeight(Math.min(messages.size(), 4) * 28 + 4);
        setTop(messages.isEmpty() ? null : validation);
        updatePrintAvailability();
    }

    private VBox settings() {
        printers.setPromptText(t("print.printerPrompt")); printers.setMaxWidth(Double.MAX_VALUE);
        Button calibrate = new Button(t("calibration.title"));
        calibrate.setMaxWidth(Double.MAX_VALUE);
        calibrate.disableProperty().bind(printing.or(printers.valueProperty().isNull()).or(template.isNull()));
        calibrate.setOnAction(event -> calibrationDialog().showAndWait());
        copies.setEditable(true); commitOnFocusLoss(copies);
        startSlot.setEditable(true); commitOnFocusLoss(startSlot);
        startSlot.setTooltip(new Tooltip(t("print.startSlotTip")));
        fromPage.setEditable(true); commitOnFocusLoss(fromPage);
        toPage.setEditable(true); commitOnFocusLoss(toPage);
        allPages.setSelected(true);
        fromPage.setDisable(true); toPage.setDisable(true);
        allPages.selectedProperty().addListener((obs, oldValue, selected) -> {
            fromPage.setDisable(selected); toPage.setDisable(selected);
        });
        // The captions keep their width and the page spinners share the rest, so "From" and "to" are never cut short.
        Label from = new Label(t("print.from")); Label to = new Label(t("print.to"));
        from.setMinWidth(Region.USE_PREF_SIZE); to.setMinWidth(Region.USE_PREF_SIZE);
        fromPage.setPrefWidth(80); toPage.setPrefWidth(80);
        HBox range = new HBox(6, from, fromPage, to, toPage);
        range.setAlignment(Pos.CENTER_LEFT);
        print.getStyleClass().add("primary-button"); print.setMaxWidth(Double.MAX_VALUE);
        cancelPrint.setMaxWidth(Double.MAX_VALUE); cancelPrint.disableProperty().bind(printing.not());
        cancelPrint.setOnAction(event -> { labelPrinter.cancelCurrent(); status.setText(t("print.cancelling")); });
        Label manualTitle = new Label(t("print.manualTitle"));
        manualTitle.getStyleClass().add("panel-title");
        Label manualHint = new Label(t("print.manualHint"));
        manualHint.setWrapText(true);
        manualGrid.setHgap(8); manualGrid.setVgap(6);
        ColumnConstraints captions = new ColumnConstraints(); captions.setMinWidth(Region.USE_PREF_SIZE);
        ColumnConstraints inputs = new ColumnConstraints(); inputs.setHgrow(Priority.ALWAYS);
        manualGrid.getColumnConstraints().setAll(captions, inputs);
        manualBox.getChildren().setAll(manualTitle, manualHint, manualGrid, new Separator());
        updateManualVisibility();
        VBox box = new VBox(8, manualBox, new Label(t("print.printer")), printers, calibrate, new Label(t("print.copies")), copies,
                new Label(t("print.startSlot")), startSlot, allPages, range, print, cancelPrint, status);
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
        int slots = getTemplate() != null && getTemplate().page().mode() == PageMode.SHEET
                ? getTemplate().page().sheetLayout().labelsPerPage() : 1;
        startSlot.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Math.max(1, slots), 1));
        startSlot.setDisable(slots <= 1); resetPreview();
    }

    private void resetPreview() {
        pageIndex = 0;
        if (getTemplate() == null) {
            generation.incrementAndGet();
            pages = List.of();
            previewRecords = List.of();
            preview.setImage(null);
            status.setText(t("print.chooseTemplate"));
            updateNavigation();
            updatePrintAvailability();
            return;
        }
        previewRecords = getRecords().isEmpty() ? List.of(manualRecord(true)) : getRecords();
        try {
            pages = planner.plan(getTemplate().page(), previewRecords.size(), startSlotIndex());
        } catch (RuntimeException exception) {
            generation.incrementAndGet();
            pages = List.of();
            preview.setImage(null);
            status.setText(t("print.planFailed", rootMessage(exception)));
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
        boolean manual = getRecords().isEmpty();
        status.setText(t("print.rendering"));
        Thread.ofVirtual().name("universal-label-preview").start(() -> {
            try {
                BufferedImage image = renderPage(currentTemplate, currentRecords, page, 96, true);
                Platform.runLater(() -> { if (generation.get() == request) { preview.setImage(SwingFXUtils.toFXImage(image, null)); status.setText(t(manual ? "print.manualPreview" : "print.previewReady")); } });
            } catch (Exception exception) {
                Platform.runLater(() -> { if (generation.get() == request) { preview.setImage(null); status.setText(t("print.previewError", rootMessage(exception))); } });
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
    private void updateNavigation() { pageStatus.setText(pages.isEmpty() ? t("print.pageStatus", 0, 0) : t("print.pageStatus", pageIndex + 1, pages.size())); previous.setDisable(pageIndex <= 0); next.setDisable(pages.isEmpty() || pageIndex >= pages.size() - 1); }

    private void loadPrinters() {
        status.setText(t("print.discovering"));
        Thread.ofVirtual().name("universal-printer-discovery").start(() -> {
            List<String> names = printerCatalog.discover().stream().map(descriptor -> descriptor.name()).toList();
            Platform.runLater(() -> {
                String selected = printers.getValue();
                printers.setItems(FXCollections.observableArrayList(names));
                if (selected != null && names.contains(selected)) printers.setValue(selected);
                else if (!names.isEmpty()) printers.getSelectionModel().selectFirst();
                updatePrintAvailability();
                if (getTemplate() == null) status.setText(names.isEmpty() ? t("print.noPrinters")
                        : t("print.printersFound", texts.countAfterPreposition(Counted.PRINTERS, names.size())));
            });
        });
    }

    private void updatePrintAvailability() {
        print.setDisable(printing.get() || !validation.getItems().isEmpty() || printers.getItems().isEmpty()
                || getTemplate() == null || pages.isEmpty());
    }

    private void print() {
        if (print.isDisabled() || printers.getValue() == null) return;
        boolean manual = getRecords().isEmpty();
        List<String> blank = manual ? blankManualFields() : List.of();
        if (!blank.isEmpty()) {
            status.setText(t("print.blankFields", String.join(texts.language() == UiLanguage.ARABIC ? "، " : ", ", blank)));
            manualValues.get(blank.getFirst()).requestFocus();
            return;
        }
        List<Map<String, ?>> currentRecords = getRecords();
        List<Map<String, ?>> printRecords = manual ? List.of(manualRecord(false)) : currentRecords;
        boolean restrictRange = !allPages.isSelected();
        int first = restrictRange ? fromPage.getValue() : 0;
        int last = restrictRange ? toPage.getValue() : 0;
        if (restrictRange && first > last) {
            status.setText(t("print.rangeInvalid"));
            return;
        }
        int firstIndex = restrictRange ? first - 1 : 0;
        int lastIndex = restrictRange ? last - 1 : pages.size() - 1;
        int labelsPerCopy = pages.subList(firstIndex, lastIndex + 1).stream().mapToInt(page -> page.placements().size()).sum();
        ButtonType ok = new ButtonType(t("dialog.ok"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(t("dialog.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        var request = new LabelPrintRequest(getTemplate(), printRecords, printers.getValue(),
                copies.getValue(), startSlotIndex(), first, last);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                confirmation(texts, printers.getValue(), labelsPerCopy, lastIndex - firstIndex + 1, copies.getValue(),
                        restrictRange ? first : 0, restrictRange ? last : 0),
                ok, cancel);
        alert.setTitle(t("print.confirmTitle")); alert.setHeaderText(t("print.confirmHeader"));
        alert.getDialogPane().setNodeOrientation(texts.language().orientation());
        if (alert.showAndWait().orElse(cancel) != ok) return;
        if (getTemplate() == request.template() && getRecords() == currentRecords) submitPrint(request);
    }

    void submitPrint(LabelPrintRequest request) {
        if (printing.get() || !validation.getItems().isEmpty()) return;
        var offset = calibrations.load(request.printerName(), request.template().page().pageSize());
        runPrintJob(request.withOffset(offset));
    }

    CalibrationDialog calibrationDialog() {
        String printer = printers.getValue();
        var page = getTemplate().page();
        var dialog = new CalibrationDialog(texts, printer, page.pageSize(), calibrations);
        if (getScene() != null) dialog.initOwner(getScene().getWindow());
        dialog.testSheet.disableProperty().bind(printing);
        dialog.testSheet.setOnAction(event -> {
            dialog.jobStatus.textProperty().bind(status.textProperty());
            runPrintJob(CalibrationSheet.request(printer, page, t("calibration.test")));
        });
        return dialog;
    }

    private void runPrintJob(LabelPrintRequest request) {
        if (printing.get()) return;
        printing.set(true);
        updatePrintAvailability();
        status.setText(t("print.sending"));
        labelPrinter.print(request).whenComplete((result, error) -> Platform.runLater(() -> {
            printing.set(false);
            updatePrintAvailability();
            if (error == null) {
                status.setText(t("print.done", texts.count(Counted.LABELS, result.labelsPrinted()),
                        texts.countAfterPreposition(Counted.PAGES, result.pagesPrinted())));
            } else {
                status.setText(describePrintError(error));
            }
        }));
    }

    private String describePrintError(Throwable throwable) {
        Throwable error = unwrap(throwable);
        if (error instanceof PrintCancelledException) return t("print.cancelled", rootMessage(error));
        if (error instanceof PrinterNotFoundException) return t("print.printerMissing", rootMessage(error));
        if (error instanceof PrintJobFailedException failed) {
            return t("print.failedAfter", failed.pagesCompleted(), failed.totalPages(), rootMessage(error));
        }
        return t("print.failed", rootMessage(error));
    }

    /** One field per placeholder of the template, keeping what was already typed for a field of the same name. */
    private void rebuildManualFields() {
        Map<String, String> typed = new LinkedHashMap<>();
        manualValues.forEach((name, field) -> typed.put(name, field.getText()));
        manualValues.clear();
        manualGrid.getChildren().clear();
        List<String> names = getTemplate() == null ? List.of() : templateFields.scan(getTemplate());
        for (int row = 0; row < names.size(); row++) {
            String name = names.get(row);
            TextField field = new TextField(typed.getOrDefault(name, ""));
            field.setPromptText(name);
            field.setMaxWidth(Double.MAX_VALUE);
            field.textProperty().addListener((obs, oldValue, value) -> { if (getRecords().isEmpty()) resetPreview(); });
            manualValues.put(name, field);
            manualGrid.add(new Label(name), 0, row);
            manualGrid.add(field, 1, row);
        }
        updateManualVisibility();
    }

    private void updateManualVisibility() {
        boolean show = getRecords().isEmpty() && !manualValues.isEmpty();
        manualBox.setVisible(show); manualBox.setManaged(show);
    }

    /** The template's fields, in order, for which nothing has been typed. */
    List<String> blankManualFields() {
        return manualValues.entrySet().stream()
                .filter(entry -> entry.getValue().getText().isBlank()).map(Map.Entry::getKey).toList();
    }

    /**
     * The typed values as one record. For the preview a blank field keeps its {{placeholder}}, so the label still
     * shows where the value goes and a barcode still has data to draw.
     */
    Map<String, String> manualRecord(boolean forPreview) {
        Map<String, String> record = new LinkedHashMap<>();
        manualValues.forEach((name, field) -> {
            String value = field.getText().strip();
            record.put(name, value.isEmpty() && forPreview ? "{{" + name + "}}" : value);
        });
        return record;
    }

    int startSlotIndex() {
        return startSlot.getValue() - 1;
    }

    /**
     * The text asking the user to confirm a print job. {@code pages} is the number of sheet sides printed for
     * one copy; {@code firstPage}/{@code lastPage} are 0 when every page is printed.
     */
    static String confirmation(UiTexts texts, String printer, int labelsPerCopy, int pages, int copies, int firstPage, int lastPage) {
        StringBuilder text = new StringBuilder(texts.get("print.confirmTotal",
                texts.count(Counted.LABELS, (long) labelsPerCopy * copies),
                texts.countAfterPreposition(Counted.PAGES, (long) pages * copies)));
        if (copies > 1) {
            text.append('\n').append(texts.get("print.confirmCopies",
                    texts.count(Counted.COPIES, copies), texts.count(Counted.LABELS, labelsPerCopy)));
        }
        if (firstPage > 0) text.append('\n').append(texts.get("print.confirmRange", firstPage, lastPage));
        return text.append('\n').append(texts.get("print.confirmPrinter", printer)).toString();
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof java.util.concurrent.CompletionException
                || current instanceof java.util.concurrent.ExecutionException) && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
    public UiLanguage language() { return texts.language(); }
    private String t(String key, Object... arguments) { return texts.get(key, arguments); }
    private static int pixels(double mm, double dpi) { return Math.max(1, (int) Math.round(UnitConversions.mmToPixels(mm, dpi))); }
    private static String rootMessage(Throwable throwable) { Throwable current = throwable; while (current.getCause() != null) current = current.getCause(); return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage(); }
}
