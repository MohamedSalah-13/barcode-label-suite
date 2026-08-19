package com.mohamed.barcodelabel.fx.workbench;

import com.mohamed.barcodelabel.fx.designer.VisualLabelDesigner;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.workflow.csv.CsvDataset;
import com.mohamed.barcodelabel.workflow.csv.CsvImportService;
import com.mohamed.barcodelabel.workflow.paper.PaperPreset;
import com.mohamed.barcodelabel.workflow.paper.PaperPresetRegistry;
import com.mohamed.barcodelabel.workflow.template.TemplateDocuments;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Complete embeddable workspace: visual design, JSON template files, paper presets,
 * CSV data preview and printing for sheets, rolls and single-label pages.
 */
public final class BarcodeLabelWorkbench extends BorderPane {
    private final VisualLabelDesigner designer;
    private final UniversalPrintPane printPane = new UniversalPrintPane();
    private final TemplateDocuments documents = new TemplateDocuments();
    private final CsvImportService csv = new CsvImportService();
    private final PaperPresetRegistry paperPresets = new PaperPresetRegistry();
    private final ObjectProperty<Path> currentTemplateFile = new SimpleObjectProperty<>(this, "currentTemplateFile");
    private final ObjectProperty<CsvDataset> dataset = new SimpleObjectProperty<>(this, "dataset");
    private final TableView<Map<String, String>> dataTable = new TableView<>();
    private final Label dataSummary = new Label("لم يتم استيراد بيانات بعد");
    private final Label status = new Label("جاهز");
    private final ProgressIndicator busy = new ProgressIndicator();
    private final TabPane tabs = new TabPane();
    private final ComboBox<PaperPreset> presetBox = new ComboBox<>();
    private final Button openButton = new Button("فتح قالب");
    private final Button saveButton = new Button("حفظ");
    private final Button saveAsButton = new Button("حفظ باسم");
    private final Button importButton = new Button("استيراد CSV");

    public BarcodeLabelWorkbench(LabelTemplate initialTemplate) {
        designer = new VisualLabelDesigner(initialTemplate);
        setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        getStyleClass().add("barcode-label-workbench");
        setTop(toolbar()); setCenter(contentTabs()); setBottom(statusBar());
        dataset.addListener((obs, oldValue, value) -> updateDataset(value));
        loadStylesheet();
    }

    public LabelTemplate getTemplate() { return designer.getTemplate(); }
    public void setTemplate(LabelTemplate template) { designer.setTemplate(template); status.setText("تم تحميل القالب: " + template.name()); }
    public ReadOnlyObjectProperty<LabelTemplate> templateProperty() { return designer.templateProperty(); }
    public CsvDataset getDataset() { return dataset.get(); }
    public void setDataset(CsvDataset value) { dataset.set(value); }
    public ObjectProperty<CsvDataset> datasetProperty() { return dataset; }
    public Path getCurrentTemplateFile() { return currentTemplateFile.get(); }
    public ReadOnlyObjectProperty<Path> currentTemplateFileProperty() { return currentTemplateFile; }
    public VisualLabelDesigner designer() { return designer; }
    public UniversalPrintPane printPane() { return printPane; }
    public PaperPresetRegistry paperPresets() { return paperPresets; }

    private HBox toolbar() {
        presetBox.setItems(FXCollections.observableArrayList(paperPresets.all()));
        presetBox.setPromptText("اختر مقاس الورق"); presetBox.setPrefWidth(290);
        Button applyPreset = new Button("تطبيق المقاس"); Button customPaper = new Button("مقاس مخصص");
        openButton.setOnAction(event -> chooseOpen()); saveButton.setOnAction(event -> save(false)); saveAsButton.setOnAction(event -> save(true));
        importButton.setOnAction(event -> chooseCsv()); applyPreset.setOnAction(event -> applyPreset()); customPaper.setOnAction(event -> customPaper());
        HBox bar = new HBox(8, openButton, saveButton, saveAsButton, new Separator(), presetBox, applyPreset, customPaper,
                new Separator(), importButton);
        bar.setAlignment(Pos.CENTER_LEFT); bar.setPadding(new Insets(10)); bar.getStyleClass().add("workbench-toolbar"); return bar;
    }

    private TabPane contentTabs() {
        Tab design = tab("تصميم الملصق", designer);
        VBox dataPane = new VBox(8, dataSummary, dataTable); dataPane.setPadding(new Insets(12)); VBox.setVgrow(dataTable, Priority.ALWAYS);
        Tab data = tab("بيانات الطباعة", dataPane); Tab print = tab("المعاينة والطباعة", printPane);
        print.setOnSelectionChanged(event -> { if (print.isSelected()) syncPrintPane(); });
        tabs.getTabs().setAll(design, data, print); return tabs;
    }

    private HBox statusBar() {
        busy.setMaxSize(18, 18); busy.setVisible(false); busy.setManaged(false);
        HBox bar = new HBox(8, busy, status); bar.setAlignment(Pos.CENTER_LEFT); bar.setPadding(new Insets(7, 12, 7, 12)); bar.getStyleClass().add("status-bar"); return bar;
    }

    private void chooseOpen() {
        FileChooser chooser = templateChooser("فتح قالب ملصق");
        java.io.File selected = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (selected == null) return;
        runIo("جارٍ فتح القالب...", () -> documents.open(selected.toPath()), template -> {
            designer.setTemplate(template); currentTemplateFile.set(selected.toPath()); status.setText("تم فتح " + selected.getName());
        });
    }

    private void save(boolean forceChoose) {
        Path target = forceChoose ? null : getCurrentTemplateFile();
        if (target == null) {
            FileChooser chooser = templateChooser("حفظ قالب الملصق"); chooser.setInitialFileName(safeFileName(getTemplate().name()) + ".label.json");
            java.io.File selected = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
            if (selected == null) return; target = selected.toPath();
        }
        Path finalTarget = target; LabelTemplate snapshot = getTemplate();
        runIo("جارٍ حفظ القالب...", () -> { documents.save(finalTarget, snapshot); return finalTarget; }, path -> {
            currentTemplateFile.set(path); status.setText("تم حفظ القالب في " + path.getFileName());
        });
    }

    private void chooseCsv() {
        FileChooser chooser = new FileChooser(); chooser.setTitle("استيراد بيانات CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV / TSV", "*.csv", "*.tsv", "*.txt"));
        java.io.File selected = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (selected == null) return;
        runIo("جارٍ قراءة ملف البيانات...", () -> csv.read(selected.toPath()), value -> {
            dataset.set(value); tabs.getSelectionModel().select(1); status.setText("تم استيراد " + value.rows().size() + " سجل من " + selected.getName());
        });
    }

    private void applyPreset() {
        PaperPreset selected = presetBox.getValue();
        if (selected == null) { status.setText("اختر مقاسًا من القائمة أولًا"); return; }
        designer.setTemplate(documents.withPage(getTemplate(), selected.page()));
        status.setText("تم تطبيق المقاس: " + selected.displayName());
    }

    private void customPaper() {
        new CustomPaperDialog().showAndWait().ifPresent(page -> {
            designer.setTemplate(documents.withPage(getTemplate(), page)); status.setText("تم تطبيق مقاس الورق المخصص");
        });
    }

    private void updateDataset(CsvDataset value) {
        dataTable.getColumns().clear();
        if (value == null) { dataTable.getItems().clear(); dataSummary.setText("لم يتم استيراد بيانات بعد"); return; }
        for (String header : value.headers()) {
            TableColumn<Map<String, String>, String> column = new TableColumn<>(header);
            column.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getOrDefault(header, "")));
            column.setPrefWidth(150); dataTable.getColumns().add(column);
        }
        dataTable.setItems(FXCollections.observableArrayList(value.rows()));
        dataSummary.setText(value.rows().size() + " سجل — " + value.headers().size() + " عمود");
        syncPrintPane();
    }

    private void syncPrintPane() {
        printPane.setTemplate(designer.getTemplate());
        printPane.setRecords(getDataset() == null ? List.of() : getDataset().printRecords());
    }

    private <T> void runIo(String message, IoSupplier<T> action, Consumer<T> success) {
        setBusy(true, message); CompletableFuture<T> future = new CompletableFuture<>();
        Thread.ofVirtual().name("barcode-label-file-io").start(() -> {
            try { future.complete(action.get()); } catch (Throwable error) { future.completeExceptionally(error); }
        });
        future.whenComplete((value, error) -> Platform.runLater(() -> {
            setBusy(false, null);
            if (error == null) success.accept(value);
            else showError("تعذر إكمال العملية", rootMessage(error));
        }));
    }

    private void setBusy(boolean value, String message) {
        busy.setVisible(value); busy.setManaged(value); openButton.setDisable(value); saveButton.setDisable(value); saveAsButton.setDisable(value); importButton.setDisable(value);
        if (message != null) status.setText(message);
    }

    private void showError(String title, String message) {
        status.setText(title + ": " + message); Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK); alert.setHeaderText(title); alert.show();
    }

    private static FileChooser templateChooser(String title) {
        FileChooser chooser = new FileChooser(); chooser.setTitle(title); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Barcode label template", "*.json", "*.label.json")); return chooser;
    }
    private static Tab tab(String title, javafx.scene.Node content) { Tab tab = new Tab(title, content); tab.setClosable(false); return tab; }
    private static String safeFileName(String name) { String cleaned = name.replaceAll("[\\\\/:*?\"<>|]", "-").trim(); return cleaned.isEmpty() ? "label-template" : cleaned; }
    private static String rootMessage(Throwable throwable) { Throwable current = throwable; while (current.getCause() != null) current = current.getCause(); return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage(); }
    private void loadStylesheet() { var resource = BarcodeLabelWorkbench.class.getResource("barcode-label-workbench.css"); if (resource != null) getStylesheets().add(resource.toExternalForm()); }

    @FunctionalInterface private interface IoSupplier<T> { T get() throws IOException; }
}
