package com.mohamed.barcodelabel.fx.batch;

import com.mohamed.barcodelabel.fx.workbench.BarcodeLabelWorkbench;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.workflow.csv.CsvDataset;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Adds validated column mapping and per-row quantities around the reusable phase-5 workbench. */
public final class BatchProductionWorkbench extends BorderPane {
    private final BarcodeLabelWorkbench labelWorkbench;
    private final BatchDataMappingPane batchPane = new BatchDataMappingPane();
    private final TabPane tabs = new TabPane();

    public BatchProductionWorkbench(LabelTemplate template) {
        labelWorkbench = new BarcodeLabelWorkbench(template);
        setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        Tab production = tab("تصميم ومعاينة وطباعة", labelWorkbench);
        Tab batch = tab("مطابقة CSV والكميات", batchPane);
        batch.setOnSelectionChanged(event -> { if (batch.isSelected()) batchPane.setTemplate(labelWorkbench.getTemplate()); });
        batchPane.resultProperty().addListener((obs, oldValue, value) -> {
            if (value != null && value.ready()) labelWorkbench.setDataset(asDataset(value.printRecords()));
        });
        tabs.getTabs().setAll(production, batch); setCenter(tabs);
    }

    public BarcodeLabelWorkbench labelWorkbench() { return labelWorkbench; }
    public BatchDataMappingPane batchPane() { return batchPane; }
    public LabelTemplate getTemplate() { return labelWorkbench.getTemplate(); }

    private static CsvDataset asDataset(List<Map<String, ?>> records) {
        LinkedHashSet<String> headerSet = new LinkedHashSet<>(); records.forEach(record -> headerSet.addAll(record.keySet()));
        List<String> headers = List.copyOf(headerSet); List<Map<String, String>> rows = new ArrayList<>(records.size());
        for (Map<String, ?> record : records) {
            var row = new java.util.LinkedHashMap<String, String>(); headers.forEach(header -> row.put(header, String.valueOf(record.getOrDefault(header, "")))); rows.add(row);
        }
        return new CsvDataset(headers, rows, ',');
    }

    private static Tab tab(String title, javafx.scene.Node content) { Tab tab = new Tab(title, content); tab.setClosable(false); return tab; }
}
