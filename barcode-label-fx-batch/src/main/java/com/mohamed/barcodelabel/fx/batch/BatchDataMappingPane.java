package com.mohamed.barcodelabel.fx.batch;

import com.mohamed.barcodelabel.batch.*;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.workflow.csv.CsvDataset;
import com.mohamed.barcodelabel.workflow.csv.CsvImportService;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class BatchDataMappingPane extends BorderPane {
    private static final String NO_QUANTITY = "— نسخة واحدة لكل صف —";
    private static final String NOT_PREPARED = "لم يتم إعداد دفعة طباعة";
    private static final String MAPPING_CHANGED = "تغيّرت المطابقة؛ اضغط «تحقق وجهّز الطباعة» من جديد";
    private static final int PREVIEW_ROWS = 8;
    private static final double PREVIEW_ROW_HEIGHT = 30;
    private static final double PREVIEW_HEADER_HEIGHT = 36;
    private final ObjectProperty<LabelTemplate> template = new SimpleObjectProperty<>(this, "template");
    private final ObjectProperty<CsvDataset> dataset = new SimpleObjectProperty<>(this, "dataset");
    private final ReadOnlyObjectWrapper<BatchPreparationResult> result = new ReadOnlyObjectWrapper<>(this, "result");
    private final TemplateFields templateFields = new TemplateFields();
    private final BatchPreparationService preparation = new BatchPreparationService();
    private final CsvImportService csv = new CsvImportService();
    private final MappingProfileStore profiles = new MappingProfileStore();
    private final GridPane mappings = new GridPane();
    private final TableView<Map<String, String>> preview = new TableView<>();
    private final LinkedHashMap<String, ComboBox<String>> fieldBoxes = new LinkedHashMap<>();
    private final ComboBox<String> quantityColumn = new ComboBox<>();
    private final Spinner<Integer> defaultCopies = new Spinner<>(1, 999, 1);
    private final Spinner<Integer> maxCopiesPerRowField = new Spinner<>(1, 100_000, 999);
    private final Spinner<Integer> maxTotalLabelsField = new Spinner<>(1, 5_000_000, 100_000);
    private final ListView<String> issues = new ListView<>();
    private final Label fileSummary = new Label("اختر ملف CSV للبدء");
    private final Label resultSummary = new Label(NOT_PREPARED);
    private final ProgressIndicator busy = new ProgressIndicator();
    private final Button prepareButton = new Button("تحقق وجهّز الطباعة", new FontIcon(Feather.CHECK_CIRCLE));
    private ColumnMapping loadedProfile;
    /** The name shown for the dataset; the next imported file's name until the dataset listener takes it. */
    private String pendingDatasetName;
    private String datasetName = "البيانات";
    /** The dataset and template fields the mapping controls were last built for. */
    private CsvDataset mappedDataset;
    private List<String> mappedFields = List.of();
    /** True while the mapping controls are being rebuilt, so their value changes do not count as user edits. */
    private boolean rebuilding;

    public BatchDataMappingPane() {
        getStyleClass().add("batch-data-mapping-pane");
        setTop(header()); setCenter(content()); setBottom(actions());
        template.addListener((obs, oldValue, value) -> templateChanged());
        dataset.addListener((obs, oldValue, value) -> {
            datasetName = pendingDatasetName != null ? pendingDatasetName : "البيانات";
            pendingDatasetName = null;
            rebuildMappings(false);
        });
        loadStylesheet();
    }

    public LabelTemplate getTemplate() { return template.get(); }
    public void setTemplate(LabelTemplate value) { template.set(value); }
    public ObjectProperty<LabelTemplate> templateProperty() { return template; }
    public CsvDataset getDataset() { return dataset.get(); }
    public void setDataset(CsvDataset value) { dataset.set(value); }
    public ObjectProperty<CsvDataset> datasetProperty() { return dataset; }
    public BatchPreparationResult getResult() { return result.get(); }
    public ReadOnlyObjectProperty<BatchPreparationResult> resultProperty() { return result.getReadOnlyProperty(); }

    private VBox header() {
        Label title = new Label("إعداد بيانات الطباعة الجماعية"); title.getStyleClass().add("batch-title");
        Button importCsv = new Button("استيراد CSV", new FontIcon(Feather.UPLOAD)); importCsv.getStyleClass().add("primary-button");
        Button saveProfile = new Button("حفظ المطابقة", new FontIcon(Feather.SAVE));
        Button loadProfile = new Button("فتح مطابقة", new FontIcon(Feather.FOLDER));
        importCsv.setOnAction(event -> importCsv()); saveProfile.setOnAction(event -> saveProfile()); loadProfile.setOnAction(event -> loadProfile());
        busy.setPrefSize(18, 18); busy.setMaxSize(18, 18); busy.setVisible(false); busy.setManaged(false);
        HBox toolbar = new HBox(8, importCsv, saveProfile, loadProfile, busy); toolbar.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(7, title, fileSummary, toolbar); box.setPadding(new Insets(14)); return box;
    }

    private SplitPane content() {
        mappings.setHgap(12); mappings.setVgap(9); mappings.setPadding(new Insets(12));
        ScrollPane mappingScroll = new ScrollPane(mappings); mappingScroll.setFitToWidth(true);
        preview.setPlaceholder(new Label("استورد CSV لعرض معاينة البيانات"));
        preview.setFixedCellSize(PREVIEW_ROW_HEIGHT);
        preview.setPrefHeight(160);
        VBox mappingSide = new VBox(8, new Label("معاينة البيانات (أول " + PREVIEW_ROWS + " صفوف)"), preview, mappingScroll);
        mappingSide.setPadding(new Insets(12)); VBox.setVgrow(mappingScroll, Priority.ALWAYS);
        issues.setPlaceholder(new Label("لا توجد أخطاء"));
        VBox validation = new VBox(8, new Label("نتيجة التحقق"), resultSummary, issues);
        validation.setPadding(new Insets(12)); VBox.setVgrow(issues, Priority.ALWAYS);
        SplitPane split = new SplitPane(mappingSide, validation); split.setDividerPositions(0.6); return split;
    }

    private HBox actions() {
        defaultCopies.setEditable(true); commitOnFocusLoss(defaultCopies); quantityColumn.setPrefWidth(240);
        maxCopiesPerRowField.setEditable(true); commitOnFocusLoss(maxCopiesPerRowField); maxCopiesPerRowField.setPrefWidth(90);
        maxTotalLabelsField.setEditable(true); commitOnFocusLoss(maxTotalLabelsField); maxTotalLabelsField.setPrefWidth(110);
        prepareButton.getStyleClass().add("primary-button"); prepareButton.setOnAction(event -> prepare());
        quantityColumn.valueProperty().addListener((obs, oldValue, value) -> mappingEdited());
        defaultCopies.valueProperty().addListener((obs, oldValue, value) -> mappingEdited());
        maxCopiesPerRowField.valueProperty().addListener((obs, oldValue, value) -> mappingEdited());
        maxTotalLabelsField.valueProperty().addListener((obs, oldValue, value) -> mappingEdited());
        HBox box = new HBox(10, new Label("عمود الكمية"), quantityColumn,
                new Label("الكمية الافتراضية"), defaultCopies,
                new Label("أقصى نسخ للصف"), maxCopiesPerRowField,
                new Label("أقصى إجمالي ملصقات"), maxTotalLabelsField,
                prepareButton);
        box.setPadding(new Insets(12)); box.setAlignment(Pos.CENTER_LEFT); return box;
    }

    /**
     * A new template keeps the current mapping and result when it uses the same fields (a style edit, say);
     * otherwise the controls are rebuilt for its fields, keeping the columns already chosen for the others.
     */
    private void templateChanged() {
        if (getTemplate() != null && getDataset() != null && getDataset() == mappedDataset
                && templateFields.scan(getTemplate()).equals(mappedFields)) return;
        rebuildMappings(getDataset() != null && getDataset() == mappedDataset);
    }

    private void rebuildMappings(boolean keepChoices) {
        ColumnMapping previous = keepChoices && !fieldBoxes.isEmpty() ? currentMapping() : null;
        rebuilding = true;
        try {
            rebuildPreview();
            mappings.getChildren().clear(); fieldBoxes.clear(); clearResult(NOT_PREPARED);
            mappedDataset = null; mappedFields = List.of();
            if (getTemplate() == null || getDataset() == null) {
                mappings.add(new Label("يجب تحديد قالب واستيراد CSV أولًا"), 0, 0); prepareButton.setDisable(true); return;
            }
            List<String> fields = templateFields.scan(getTemplate());
            ColumnMapping automatic = ColumnMapping.automatic(fields, getDataset().headers());
            ColumnMapping chosen = loadedProfile != null ? loadedProfile : previous != null ? previous : automatic;
            mappings.add(new Label("حقل القالب"), 0, 0); mappings.add(new Label("عمود CSV"), 1, 0);
            for (int index = 0; index < fields.size(); index++) {
                String field = fields.get(index); ComboBox<String> box = new ComboBox<>(FXCollections.observableArrayList(getDataset().headers()));
                String column = chosen.fields().getOrDefault(field, automatic.fields().get(field));
                box.setPromptText("اختر العمود"); box.setMaxWidth(Double.MAX_VALUE); box.setValue(column);
                box.valueProperty().addListener((obs, oldValue, value) -> mappingEdited());
                mappings.add(new Label("{{" + field + "}}"), 0, index + 1); mappings.add(box, 1, index + 1); GridPane.setHgrow(box, Priority.ALWAYS);
                fieldBoxes.put(field, box);
            }
            quantityColumn.setItems(FXCollections.observableArrayList()); quantityColumn.getItems().add(NO_QUANTITY); quantityColumn.getItems().addAll(getDataset().headers());
            quantityColumn.setValue(chosen.quantityColumn() == null ? NO_QUANTITY : chosen.quantityColumn());
            defaultCopies.getValueFactory().setValue(chosen.defaultCopies());
            maxCopiesPerRowField.getValueFactory().setValue(chosen.maxCopiesPerRow());
            maxTotalLabelsField.getValueFactory().setValue(chosen.maxTotalLabels());
            loadedProfile = null;
            mappedDataset = getDataset(); mappedFields = fields;
            prepareButton.setDisable(fields.isEmpty());
            fileSummary.setText(BatchTexts.fileSummary(datasetName, getDataset().rows().size(), getDataset().headers().size(), fields.size()));
        } finally {
            rebuilding = false;
        }
    }

    /** Any change to the mapping makes the last result stale: it no longer describes what would print. */
    private void mappingEdited() {
        if (!rebuilding && result.get() != null) clearResult(MAPPING_CHANGED);
    }

    private void clearResult(String summary) {
        result.set(null); issues.getItems().clear();
        resultSummary.setText(summary); resultSummary.getStyleClass().removeAll("result-ok", "result-error");
    }

    private void rebuildPreview() {
        preview.getColumns().clear();
        preview.getItems().clear();
        CsvDataset current = getDataset();
        if (current == null) return;
        for (String header : current.headers()) {
            TableColumn<Map<String, String>, String> column = new TableColumn<>(header);
            column.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOrDefault(header, "")));
            preview.getColumns().add(column);
        }
        preview.getItems().addAll(current.rows().stream().limit(PREVIEW_ROWS).toList());
        // Tall enough for every previewed row, so "the first 8 rows" are all visible without scrolling.
        preview.setPrefHeight(PREVIEW_HEADER_HEIGHT + Math.max(1, preview.getItems().size()) * PREVIEW_ROW_HEIGHT + 2);
    }

    private ColumnMapping currentMapping() {
        var values = new LinkedHashMap<String, String>();
        fieldBoxes.forEach((field, box) -> { if (box.getValue() != null) values.put(field, box.getValue()); });
        String quantity = NO_QUANTITY.equals(quantityColumn.getValue()) ? null : quantityColumn.getValue();
        return new ColumnMapping(values, quantity, defaultCopies.getValue(), maxCopiesPerRowField.getValue(), maxTotalLabelsField.getValue());
    }

    private void prepare() {
        LabelTemplate currentTemplate = getTemplate();
        CsvDataset currentDataset = getDataset();
        ColumnMapping mapping = currentMapping();
        runIo("تحقق وتجهيز الطباعة", () -> preparation.prepare(currentTemplate, currentDataset, mapping), prepared -> {
            // The user may have changed the mapping, the data or the template while this ran in the background.
            if (getTemplate() == currentTemplate && getDataset() == currentDataset && currentMapping().equals(mapping)) {
                applyPreparationResult(prepared, mapping);
            }
        });
    }

    private void applyPreparationResult(BatchPreparationResult prepared, ColumnMapping mapping) {
        result.set(prepared); issues.setItems(FXCollections.observableArrayList(prepared.issues().stream()
                .map(issue -> BatchTexts.describe(issue, mapping)).toList()));
        resultSummary.setText(prepared.ready()
                ? BatchTexts.ready(prepared.sourceRows(), prepared.labelCount())
                : "توجد أخطاء؛ لن يتم إرسال أي ملصق للطباعة");
        resultSummary.getStyleClass().removeAll("result-ok", "result-error"); resultSummary.getStyleClass().add(prepared.ready() ? "result-ok" : "result-error");
    }

    private void importCsv() {
        FileChooser chooser = new FileChooser(); chooser.setTitle("استيراد بيانات الدفعة"); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV / TSV", "*.csv", "*.tsv", "*.txt"));
        java.io.File file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow()); if (file == null) return;
        if (!fieldBoxes.isEmpty() && !confirm("استيراد ملف CSV جديد سيفقد إعداد المطابقة الحالي. هل تريد المتابعة؟")) return;
        runIo("استيراد CSV", () -> csv.read(file.toPath()), value -> { pendingDatasetName = file.getName(); setDataset(value); });
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        if (getScene() != null && getScene().getWindow() != null) alert.initOwner(getScene().getWindow());
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void saveProfile() {
        if (getDataset() == null || fieldBoxes.isEmpty()) return;
        FileChooser chooser = profileChooser("حفظ مطابقة الأعمدة"); chooser.setInitialFileName("csv-mapping.properties");
        java.io.File file = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow()); if (file == null) return;
        ColumnMapping mapping = currentMapping();
        runIo("حفظ إعداد المطابقة", () -> { profiles.save(file.toPath(), mapping); return mapping; }, value -> resultSummary.setText("تم حفظ إعداد المطابقة"));
    }

    private void loadProfile() {
        FileChooser chooser = profileChooser("فتح مطابقة أعمدة"); java.io.File file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow()); if (file == null) return;
        runIo("فتح إعداد المطابقة", () -> profiles.load(file.toPath()), value -> { loadedProfile = value; rebuildMappings(false); resultSummary.setText("تم تحميل إعداد المطابقة"); });
    }

    private <T> void runIo(String operation, IoSupplier<T> supplier, java.util.function.Consumer<T> success) {
        setBusy(true); CompletableFuture<T> future = new CompletableFuture<>();
        Thread.ofVirtual().name("batch-data-file-io").start(() -> { try { future.complete(supplier.get()); } catch (Throwable error) { future.completeExceptionally(error); } });
        future.whenComplete((value, error) -> Platform.runLater(() -> { setBusy(false); if (error == null) success.accept(value); else showError(operation, rootMessage(error)); }));
    }

    private void setBusy(boolean value) { busy.setVisible(value); busy.setManaged(value); prepareButton.setDisable(value || getDataset() == null); }
    private void showError(String operation, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, operation + ": " + message, ButtonType.OK);
        if (getScene() != null && getScene().getWindow() != null) alert.initOwner(getScene().getWindow());
        alert.show();
    }
    private static void commitOnFocusLoss(Spinner<Integer> spinner) {
        spinner.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (!isFocused) spinner.increment(0);
        });
    }

    private static FileChooser profileChooser(String title) { FileChooser chooser = new FileChooser(); chooser.setTitle(title); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Mapping profile", "*.properties")); return chooser; }
    private static String rootMessage(Throwable throwable) { Throwable current = throwable; while (current.getCause() != null) current = current.getCause(); return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage(); }
    private void loadStylesheet() { var resource = BatchDataMappingPane.class.getResource("batch-data-mapping.css"); if (resource != null) getStylesheets().add(resource.toExternalForm()); }
    @FunctionalInterface private interface IoSupplier<T> { T get() throws Exception; }
}
