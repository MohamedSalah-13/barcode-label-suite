package com.mohamed.barcodelabel.smoke;

import com.mohamed.barcodelabel.fx.workbench.BarcodeLabelWorkbench;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.workflow.paper.PaperPresetRegistry;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.List;

public final class Phase5StartupSmokeApplication extends Application {
    @Override public void start(Stage stage) {
        var page = new PaperPresetRegistry().find("a4-3x7-63.5x38.1").orElseThrow().page();
        BarcodeLabelWorkbench workbench = new BarcodeLabelWorkbench(LabelTemplate.create("Startup smoke", page, List.of()));
        if (workbench.getTemplate() == null) throw new IllegalStateException("Workbench template was not initialized");
        System.out.println("PHASE5_STARTUP_SMOKE_OK");
        Platform.exit();
    }

    public static void main(String[] args) { launch(args); }
}
