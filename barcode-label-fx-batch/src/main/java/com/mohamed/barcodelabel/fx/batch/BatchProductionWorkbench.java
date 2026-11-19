package com.mohamed.barcodelabel.fx.batch;

import com.mohamed.barcodelabel.batch.BarcodeValidationCache;

import com.mohamed.barcodelabel.fx.i18n.UiLanguage;
import com.mohamed.barcodelabel.fx.i18n.UiTexts;
import com.mohamed.barcodelabel.fx.workbench.BarcodeLabelWorkbench;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.workflow.csv.CsvDataset;
import com.mohamed.barcodelabel.workflow.data.RepeatedList;
import com.mohamed.barcodelabel.batch.BatchPreparationResult;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Adds validated column mapping and per-row quantities around the reusable label workbench. */
public final class BatchProductionWorkbench extends BorderPane {
    private final BarcodeLabelWorkbench labelWorkbench;
    private final BatchDataMappingPane batchPane;
    private final TabPane tabs = new TabPane();
    /** The print data this workbench last handed to the print workspace from a ready batch. */
    private CsvDataset preparedData;

    /** An Arabic production workbench. */
    public BatchProductionWorkbench(LabelTemplate template) {
        this(template, UiLanguage.ARABIC);
    }

    /** A production workbench in this language: Arabic right to left, English left to right. */
    public BatchProductionWorkbench(LabelTemplate template, UiLanguage language) {
        this(template, language, new BarcodeValidationCache());
    }

    public BatchProductionWorkbench(LabelTemplate template, UiLanguage language, BarcodeValidationCache cache) {
        UiTexts texts = UiTexts.load(BatchProductionWorkbench.class, "messages", language);
        labelWorkbench = new BarcodeLabelWorkbench(template, language, cache);
        batchPane = new BatchDataMappingPane(language, cache);
        setNodeOrientation(language.orientation());
        Tab production = tab(texts.get("production.designTab"), labelWorkbench);
        Tab batch = tab(texts.get("production.batchTab"), batchPane);
        batchPane.setTemplate(template);
        labelWorkbench.templateProperty().addListener((obs, old, current) -> batchPane.setTemplate(current));
        batchPane.datasetProperty().addListener((obs, old, current) -> updatePrintGate());
        batchPane.resultProperty().addListener((obs, oldValue, value) -> {
            if (value != null && value.ready()) {
                preparedData = asDataset(value);
                labelWorkbench.setDataset(preparedData);
            } else if (preparedData != null && labelWorkbench.getDataset() == preparedData) {
                // The batch is no longer valid (new file, changed mapping or errors), so its labels must not be
                // printed. Data the user imported into the print workspace directly is left alone.
                labelWorkbench.setDataset(null);
                preparedData = null;
            }
            updatePrintGate();
        });
        tabs.getTabs().setAll(production, batch); setCenter(tabs);
    }

    public UiLanguage language() { return batchPane.language(); }

    /** Keeps the origin of the print data as well as the two workspaces' state. */
    public record Session(BatchDataMappingPane.Session batch, BarcodeLabelWorkbench.Session label,
                          boolean printingPreparedBatch, int selectedTab) {}

    public Session snapshotSession() {
        return new Session(batchPane.snapshotSession(), labelWorkbench.snapshotSession(),
                preparedData != null && labelWorkbench.getDataset() == preparedData,
                tabs.getSelectionModel().getSelectedIndex());
    }

    public void restoreSession(Session session) {
        batchPane.restoreSession(session.batch());
        labelWorkbench.restoreSession(session.label());
        // The result listener may have built a fresh dataset; retain the restored dataset's ownership so a
        // later mapping edit clears prepared labels, but leaves a directly imported CSV alone.
        preparedData = session.printingPreparedBatch() ? labelWorkbench.getDataset() : null;
        tabs.getSelectionModel().select(session.selectedTab());
        updatePrintGate();
    }

    private void updatePrintGate() {
        var result = batchPane.getResult();
        labelWorkbench.printPane().setBatchValidationRequired(batchPane.getDataset() != null
                && (result == null || !result.ready() || result.labelCount() == 0));
    }
    public BarcodeLabelWorkbench labelWorkbench() { return labelWorkbench; }
    public BatchDataMappingPane batchPane() { return batchPane; }
    public LabelTemplate getTemplate() { return labelWorkbench.getTemplate(); }

    static CsvDataset asDataset(List<Map<String, ?>> records) {
        return asDataset(records, List.of());
    }

    static CsvDataset asDataset(BatchPreparationResult result) {
        return asDataset(result.printRecords(), result.printSourceRows());
    }

    private static CsvDataset asDataset(List<Map<String, ?>> records, List<Integer> sourceRows) {
        List<Map<String, ?>> logical = records instanceof RepeatedList<Map<String, ?>> repeated ? repeated.values() : records;
        LinkedHashSet<String> headerSet = new LinkedHashSet<>(); logical.forEach(record -> headerSet.addAll(record.keySet()));
        List<String> headers = List.copyOf(headerSet);
        List<Map<String, String>> rows = RepeatedList.mapCopy(records, record -> {
            var row = new java.util.LinkedHashMap<String, String>();
            headers.forEach(header -> { Object raw = record.get(header); row.put(header, raw == null ? "" : String.valueOf(raw)); });
            return row;
        });
        return new CsvDataset(headers, rows, ',', sourceRows);
    }

    private static Tab tab(String title, javafx.scene.Node content) { Tab tab = new Tab(title, content); tab.setClosable(false); return tab; }
}
