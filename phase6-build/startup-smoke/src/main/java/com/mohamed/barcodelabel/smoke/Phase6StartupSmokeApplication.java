package com.mohamed.barcodelabel.smoke;

import com.mohamed.barcodelabel.fx.batch.BatchProductionWorkbench;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.workflow.paper.PaperPresetRegistry;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.List;

public final class Phase6StartupSmokeApplication extends Application {
    @Override public void start(Stage stage) {
        var page = new PaperPresetRegistry().find("a4-3x7-63.5x38.1").orElseThrow().page();
        var workbench = new BatchProductionWorkbench(LabelTemplate.create("Batch smoke", page, List.of()));
        if (workbench.labelWorkbench() == null || workbench.batchPane() == null) throw new IllegalStateException("Batch UI was not initialized");
        System.out.println("PHASE6_BATCH_STARTUP_OK");
        Platform.exit();
    }
    public static void main(String[] args) { launch(args); }
}
