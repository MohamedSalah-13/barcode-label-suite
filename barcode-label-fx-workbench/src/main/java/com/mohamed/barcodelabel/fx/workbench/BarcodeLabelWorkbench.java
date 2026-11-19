package com.mohamed.barcodelabel.fx.workbench;

import com.mohamed.barcodelabel.batch.BarcodeValidationCache;

import com.mohamed.barcodelabel.fx.designer.VisualLabelDesigner;
import com.mohamed.barcodelabel.fx.i18n.Counted;
import com.mohamed.barcodelabel.fx.i18n.UiLanguage;
import com.mohamed.barcodelabel.fx.i18n.UiTexts;
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
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.kordamp.ikonli.feather.Feather;

import java.io.IOException;
import java.math.BigDecimal;
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
    private final UiTexts texts;
    private final VisualLabelDesigner designer;
    private final UniversalPrintPane printPane;
    private final TemplateDocuments documents = new TemplateDocuments();
    private final CsvImportService csv = new CsvImportService();
    private final PaperPresetRegistry paperPresets = new PaperPresetRegistry();
    private final ObjectProperty<Path> currentTemplateFile = new SimpleObjectProperty<>(this, "currentTemplateFile");
    private final ObjectProperty<CsvDataset> dataset = new SimpleObjectProperty<>(this, "dataset");
    private final TableView<Map<String, String>> dataTable = new TableView<>();
    private final Label dataSummary = new Label();
    private final Label status = new Label();
    private final ProgressIndicator busy = new ProgressIndicator();
    private final TabPane tabs = new TabPane();
    private final ComboBox<PaperPreset> presetBox = new ComboBox<>();
    private final Button deletePaper;
    private CustomPaperStore customPapers = CustomPaperStore.inMemory();
    private List<PaperPreset> savedPapers = List.of();
    private final Button openButton;
    private final Button saveButton;
    private final Button saveAsButton;
    private final Button importButton;
    private final javafx.beans.property.ReadOnlyBooleanWrapper working = new javafx.beans.property.ReadOnlyBooleanWrapper();

    /** The data and navigation state of this workspace, independent of its UI language. */
    public record Session(CsvDataset dataset, Path templateFile, int selectedTab, PaperPreset selectedPaper,
                          UniversalPrintPane.Session print) {}

    public Session snapshotSession() {
        return new Session(getDataset(), getCurrentTemplateFile(), tabs.getSelectionModel().getSelectedIndex(),
                presetBox.getValue(), printPane.snapshotSession());
    }

    public void restoreSession(Session session) {
        setDataset(session.dataset());
        currentTemplateFile.set(session.templateFile());
        presetBox.setValue(session.selectedPaper());
        tabs.getSelectionModel().select(session.selectedTab());
        printPane.restoreSession(session.print());
    }

    public javafx.beans.property.ReadOnlyBooleanProperty workingProperty() { return working.getReadOnlyProperty(); }

    private Button iconButton(Feather glyph, String key) {
        return IconButtons.create(glyph, t(key));
    }

    /** An Arabic workbench. */
    public BarcodeLabelWorkbench(LabelTemplate initialTemplate) {
        this(initialTemplate, UiLanguage.ARABIC);
    }

    /** A workbench in this language: Arabic right to left, English left to right. */
    public BarcodeLabelWorkbench(LabelTemplate initialTemplate, UiLanguage language) {
        this(initialTemplate, language, new BarcodeValidationCache());
    }

    public BarcodeLabelWorkbench(LabelTemplate initialTemplate, UiLanguage language, BarcodeValidationCache cache) {
        texts = UiTexts.load(BarcodeLabelWorkbench.class, "messages", language);
        designer = new VisualLabelDesigner(initialTemplate, language);
        printPane = new UniversalPrintPane(language, cache);
        setNodeOrientation(language.orientation());
        openButton = iconButton(Feather.FOLDER, "workbench.open");
        saveButton = iconButton(Feather.SAVE, "workbench.save");
        saveAsButton = iconButton(Feather.COPY, "workbench.saveAs");
        importButton = iconButton(Feather.UPLOAD, "workbench.importCsv");
        deletePaper = iconButton(Feather.TRASH_2, "workbench.deletePaper");
        dataSummary.setText(t("workbench.noData")); status.setText(t("workbench.ready"));
        getStyleClass().add("barcode-label-workbench");
        setTop(toolbar()); setCenter(contentTabs()); setBottom(statusBar());
        dataset.addListener((obs, oldValue, value) -> updateDataset(value));
        designer.templateProperty().addListener((obs, oldValue, value) -> printPane.setTemplate(value));
        syncPrintPane();
        loadStylesheet();
    }

    public UiLanguage language() { return texts.language(); }
    public LabelTemplate getTemplate() { return designer.getTemplate(); }
    public void setTemplate(LabelTemplate template) { designer.setTemplate(template); status.setText(t("workbench.templateLoaded", template.name())); }
    public ReadOnlyObjectProperty<LabelTemplate> templateProperty() { return designer.templateProperty(); }
    public CsvDataset getDataset() { return dataset.get(); }
    public void setDataset(CsvDataset value) { dataset.set(value); }
    public ObjectProperty<CsvDataset> datasetProperty() { return dataset; }
    public Path getCurrentTemplateFile() { return currentTemplateFile.get(); }
    public ReadOnlyObjectProperty<Path> currentTemplateFileProperty() { return currentTemplateFile; }
    public VisualLabelDesigner designer() { return designer; }
    public UniversalPrintPane printPane() { return printPane; }
    public PaperPresetRegistry paperPresets() { return paperPresets; }

    /**
     * Where the sizes named in the custom size dialog are kept. The workbench lists the sizes already saved there
     * after the built-in ones, and saves there each size the user names, changes or deletes. Without a store they
     * last only while the application runs.
     */
    public void setCustomPaperStore(CustomPaperStore store) {
        customPapers = java.util.Objects.requireNonNull(store, "store");
        savedPapers = List.copyOf(store.load());
        refreshPresets(presetBox.getValue());
    }

    /** The named sizes, in the order they were first saved. */
    public List<PaperPreset> savedPaperSizes() { return savedPapers; }

    private HBox toolbar() {
        refreshPresets(null);
        presetBox.valueProperty().addListener((obs, oldValue, value) -> deletePaper.setDisable(!CustomPaperDialog.isCustom(value)));
        deletePaper.setDisable(true);
        deletePaper.setOnAction(event -> deletePaper());
        presetBox.setConverter(new StringConverter<>() {
            @Override public String toString(PaperPreset preset) { return preset == null ? "" : presetName(preset); }
            @Override public PaperPreset fromString(String text) { return null; }
        });
        presetBox.setPromptText(t("workbench.paperPrompt")); presetBox.setPrefWidth(290);
        Button applyPreset = iconButton(Feather.CHECK_SQUARE, "workbench.applyPaper");
        Button labelLayout = iconButton(Feather.LAYOUT, "workbench.labelLayout");
        labelLayout.setOnAction(event -> designer.editorPane().editLayout());
        Button customPaper = iconButton(Feather.SLIDERS, "workbench.customPaper");
        openButton.setOnAction(event -> chooseOpen()); saveButton.setOnAction(event -> save(false)); saveAsButton.setOnAction(event -> save(true));
        importButton.setOnAction(event -> chooseCsv()); applyPreset.setOnAction(event -> applyPreset()); customPaper.setOnAction(event -> customPaper());
        HBox bar = new HBox(8, openButton, saveButton, saveAsButton, new Separator(), presetBox, applyPreset, customPaper, labelLayout,
                deletePaper, new Separator(), importButton);
        bar.setAlignment(Pos.CENTER_LEFT); bar.setPadding(new Insets(10)); bar.getStyleClass().add("workbench-toolbar"); return bar;
    }

    private TabPane contentTabs() {
        Tab design = tab(t("workbench.designTab"), designer);
        VBox dataPane = new VBox(8, dataSummary, dataTable); dataPane.setPadding(new Insets(12)); VBox.setVgrow(dataTable, Priority.ALWAYS);
        Tab data = tab(t("workbench.dataTab"), dataPane); Tab print = tab(t("workbench.printTab"), printPane);
        print.setOnSelectionChanged(event -> { if (print.isSelected()) syncPrintPane(); });
        tabs.getTabs().setAll(design, data, print); return tabs;
    }

    private HBox statusBar() {
        busy.setMaxSize(18, 18); busy.setVisible(false); busy.setManaged(false);
        HBox bar = new HBox(8, busy, status); bar.setAlignment(Pos.CENTER_LEFT); bar.setPadding(new Insets(7, 12, 7, 12)); bar.getStyleClass().add("status-bar"); return bar;
    }

    private void chooseOpen() {
        FileChooser chooser = templateChooser(t("workbench.openTitle"));
        java.io.File selected = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (selected != null) open(selected.toPath());
    }

    /** Opens a template file in the background, then shows it in the designer. */
    void open(Path file) {
        runIo(t("workbench.opening"), () -> documents.open(file), template -> {
            designer.setTemplate(template); currentTemplateFile.set(file); status.setText(t("workbench.opened", file.getFileName()));
        });
    }

    private void save(boolean forceChoose) {
        Path target = forceChoose ? null : getCurrentTemplateFile();
        if (target == null) {
            FileChooser chooser = templateChooser(t("workbench.saveTitle")); chooser.setInitialFileName(safeFileName(getTemplate().name()) + ".label.json");
            java.io.File selected = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
            if (selected == null) return; target = selected.toPath();
        }
        saveTo(target);
    }

    /** Saves the current template to a file in the background; later plain saves go to the same file. */
    void saveTo(Path target) {
        LabelTemplate snapshot = getTemplate();
        runIo(t("workbench.saving"), () -> { documents.save(target, snapshot); return target; }, path -> {
            currentTemplateFile.set(path); status.setText(t("workbench.saved", path.getFileName()));
        });
    }

    private void chooseCsv() {
        FileChooser chooser = new FileChooser(); chooser.setTitle(t("workbench.csvTitle"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV / TSV", "*.csv", "*.tsv", "*.txt"));
        java.io.File selected = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (selected != null) importCsv(selected.toPath());
    }

    /** Reads a CSV file in the background, then shows its rows on the data tab. */
    void importCsv(Path file) {
        runIo(t("workbench.reading"), () -> csv.read(file), value -> {
            dataset.set(value); tabs.getSelectionModel().select(1);
            status.setText(t("workbench.imported", texts.count(Counted.RECORDS, value.rows().size()), file.getFileName()));
        });
    }

    private void applyPreset() {
        PaperPreset selected = presetBox.getValue();
        if (selected == null) { status.setText(t("workbench.choosePaperFirst")); return; }
        try {
            designer.setTemplate(documents.withPage(getTemplate(), selected.page()));
            status.setText(t("workbench.paperApplied", presetName(selected)));
        } catch (IllegalArgumentException exception) {
            status.setText(t("workbench.layoutDoesNotFit"));
        }
    }

    private void customPaper() {
        // A saved size that is selected opens filled in, to be changed; saving it under its name replaces it.
        PaperPreset selected = CustomPaperDialog.isCustom(presetBox.getValue()) ? presetBox.getValue() : null;
        new CustomPaperDialog(texts, selected).showAndWait().ifPresent(this::saveCustomPaper);
    }

    /** Adds the size to the paper list, or replaces the one with its name, saves the list and applies the size. */
    void saveCustomPaper(PaperPreset preset) {
        LabelTemplate updated;
        try {
            updated = documents.withPage(getTemplate(), preset.page());
        } catch (IllegalArgumentException exception) {
            status.setText(t("workbench.layoutDoesNotFit"));
            return;
        }
        List<PaperPreset> next = new java.util.ArrayList<>(savedPapers);
        int existing = indexOf(next, preset.id());
        if (existing >= 0) next.set(existing, preset); else next.add(preset);
        storePapers(next);
        refreshPresets(preset);
        designer.setTemplate(updated);
        status.setText(t("workbench.customSaved", preset.displayName()));
    }

    /** Removes the selected saved size from the list and the store; the label keeps its page. */
    void deletePaper() {
        PaperPreset selected = presetBox.getValue();
        if (!CustomPaperDialog.isCustom(selected)) return;
        List<PaperPreset> next = new java.util.ArrayList<>(savedPapers);
        next.remove(indexOf(next, selected.id()));
        storePapers(next);
        refreshPresets(null);
        status.setText(t("workbench.paperDeleted", selected.displayName()));
    }

    private void storePapers(List<PaperPreset> next) {
        savedPapers = List.copyOf(next);
        customPapers.save(savedPapers);
    }

    /** The built-in sizes, then the saved ones; {@code selected} stays selected if it is still in the list. */
    private void refreshPresets(PaperPreset selected) {
        List<PaperPreset> all = new java.util.ArrayList<>(paperPresets.all());
        all.addAll(savedPapers);
        presetBox.setItems(FXCollections.observableArrayList(all));
        presetBox.setValue(selected == null ? null : all.stream().filter(p -> p.id().equals(selected.id())).findFirst().orElse(null));
    }

    private static int indexOf(List<PaperPreset> presets, String id) {
        for (int i = 0; i < presets.size(); i++) if (presets.get(i).id().equals(id)) return i;
        return -1;
    }

    private void updateDataset(CsvDataset value) {
        dataTable.getColumns().clear();
        if (value == null) {
            dataTable.getItems().clear();
            dataSummary.setText(t("workbench.noData"));
            syncPrintPane();
            return;
        }
        for (String header : value.headers()) {
            TableColumn<Map<String, String>, String> column = new TableColumn<>(header);
            column.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getOrDefault(header, "")));
            column.setPrefWidth(150); dataTable.getColumns().add(column);
        }
        dataTable.setItems(FXCollections.observableArrayList(value.rows()));
        dataSummary.setText(t("workbench.dataSummary", texts.count(Counted.RECORDS, value.rows().size()), texts.count(Counted.COLUMNS, value.headers().size())));
        syncPrintPane();
    }

    private void syncPrintPane() {
        printPane.setTemplate(designer.getTemplate());
        CsvDataset data = getDataset();
        printPane.setRecords(data == null ? List.of() : data.printRecords(), data == null ? List.of() : data.sourceRows());
    }

    private <T> void runIo(String message, IoSupplier<T> action, Consumer<T> success) {
        setBusy(true, message); CompletableFuture<T> future = new CompletableFuture<>();
        Thread.ofVirtual().name("barcode-label-file-io").start(() -> {
            try { future.complete(action.get()); } catch (Throwable error) { future.completeExceptionally(error); }
        });
        future.whenComplete((value, error) -> Platform.runLater(() -> {
            setBusy(false, null);
            if (error == null) success.accept(value);
            else showError(t("workbench.failed"), rootMessage(error));
        }));
    }

    private void setBusy(boolean value, String message) {
        working.set(value);
        busy.setVisible(value); busy.setManaged(value); openButton.setDisable(value); saveButton.setDisable(value); saveAsButton.setDisable(value); importButton.setDisable(value);
        if (message != null) status.setText(message);
    }

    private void showError(String title, String message) {
        status.setText(title + ": " + message);
        Alert alert = new Alert(Alert.AlertType.ERROR, message, new ButtonType(t("dialog.ok"), ButtonBar.ButtonData.OK_DONE));
        alert.setTitle(t("workbench.errorTitle")); alert.setHeaderText(title); alert.getDialogPane().setNodeOrientation(texts.language().orientation()); alert.show();
    }

    private FileChooser templateChooser(String title) {
        FileChooser chooser = new FileChooser(); chooser.setTitle(title); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(t("workbench.templateFilter"), "*.json", "*.label.json")); return chooser;
    }

    /**
     * A preset's name in the UI language. The built-in presets are named from their size; one registered by the
     * host application keeps its own display name.
     */
    String presetName(PaperPreset preset) {
        if (!BUILT_IN_PRESETS.find(preset.id()).map(preset::equals).orElse(false)) return preset.displayName();
        var page = preset.page();
        if (page.sheetLayout() != null) {
            var layout = page.sheetLayout();
            return t("paper.sheet", layout.columns(), layout.rows(), mm(layout.labelSize().width()), mm(layout.labelSize().height()));
        }
        return t("paper.roll", mm(page.pageSize().width()), mm(page.pageSize().height()));
    }

    private static final PaperPresetRegistry BUILT_IN_PRESETS = new PaperPresetRegistry();
    private static String mm(double value) { return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString(); }
    private String t(String key, Object... arguments) { return texts.get(key, arguments); }
    private static Tab tab(String title, javafx.scene.Node content) { Tab tab = new Tab(title, content); tab.setClosable(false); return tab; }
    private static String safeFileName(String name) { String cleaned = name.replaceAll("[\\\\/:*?\"<>|]", "-").trim(); return cleaned.isEmpty() ? "label-template" : cleaned; }
    private static String rootMessage(Throwable throwable) { Throwable current = throwable; while (current.getCause() != null) current = current.getCause(); return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage(); }
    private void loadStylesheet() { var resource = BarcodeLabelWorkbench.class.getResource("barcode-label-workbench.css"); if (resource != null) getStylesheets().add(resource.toExternalForm()); }

    @FunctionalInterface private interface IoSupplier<T> { T get() throws IOException; }
}
