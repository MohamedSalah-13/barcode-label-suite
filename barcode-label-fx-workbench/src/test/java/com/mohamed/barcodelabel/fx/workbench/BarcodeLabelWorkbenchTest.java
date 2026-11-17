package com.mohamed.barcodelabel.fx.workbench;

import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.TextAlignment;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.model.page.PageMode;
import com.mohamed.barcodelabel.workflow.csv.CsvDataset;
import com.mohamed.barcodelabel.workflow.paper.PaperPreset;
import com.mohamed.barcodelabel.workflow.paper.PaperPresetRegistry;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.mohamed.barcodelabel.fx.workbench.FxSupport.all;
import static com.mohamed.barcodelabel.fx.workbench.FxSupport.await;
import static com.mohamed.barcodelabel.fx.workbench.FxSupport.onFx;
import static com.mohamed.barcodelabel.fx.workbench.FxSupport.runFx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BarcodeLabelWorkbenchTest {
    private static final Duration WAIT = Duration.ofSeconds(15);

    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        FxSupport.startToolkit();
    }

    @AfterEach
    void closeWindows() {
        runFx(() -> List.copyOf(Window.getWindows()).forEach(Window::hide));
    }

    @Test
    void applyingAPaperPresetChangesTheTemplatesPage() {
        BarcodeLabelWorkbench workbench = onFx(() -> new BarcodeLabelWorkbench(template("Products")));
        runFx(() -> {
            button(workbench, "تطبيق المقاس").fire();
            assertEquals("اختر مقاسًا من القائمة أولًا", status(workbench).getText());

            PaperPreset roll = workbench.paperPresets().all().stream()
                    .filter(preset -> preset.page().mode() == PageMode.ROLL).findFirst().orElseThrow();
            presets(workbench).setValue(roll);
            button(workbench, "تطبيق المقاس").fire();
            assertEquals(PageMode.ROLL, workbench.getTemplate().page().mode());
            assertEquals(roll.page().labelSize(), workbench.getTemplate().page().labelSize());
            assertEquals("تم تطبيق المقاس: " + roll.displayName(), status(workbench).getText());
            assertEquals(workbench.getTemplate(), workbench.printPane().getTemplate(), "the print pane follows");
        });
    }

    @Test
    void theDataTabShowsTheDatasetAndThePrintPaneGetsItsRecords() {
        BarcodeLabelWorkbench workbench = onFx(() -> new BarcodeLabelWorkbench(template("Products")));
        runFx(() -> {
            workbench.setDataset(new CsvDataset(List.of("name", "sku", "price"), List.of(
                    Map.of("name", "Pen", "sku", "A1", "price", "5"),
                    Map.of("name", "Book", "sku", "B2", "price", "9")), ','));
            TableView<?> table = all(workbench).filter(TableView.class::isInstance).map(TableView.class::cast)
                    .findFirst().orElseThrow();
            assertEquals(3, table.getColumns().size());
            assertEquals(2, table.getItems().size());
            assertTrue(labelShown(workbench, "سجلان — 3 أعمدة"));
            assertEquals(2, workbench.printPane().getRecords().size());

            workbench.printPane().setRecords(List.of());
            tabs(workbench).getSelectionModel().select(2);
            assertEquals(2, workbench.printPane().getRecords().size(), "opening the print tab resyncs it");

            workbench.setDataset(null);
            assertTrue(table.getItems().isEmpty());
            assertTrue(labelShown(workbench, "لم يتم استيراد بيانات بعد"));
            assertTrue(workbench.printPane().getRecords().isEmpty());
        });
    }

    @Test
    void savingRemembersTheFileSoTheNextSaveNeedsNoChooser(@TempDir Path directory) throws Exception {
        BarcodeLabelWorkbench workbench = onFx(() -> new BarcodeLabelWorkbench(template("First")));
        Path file = directory.resolve("label.label.json");
        runFx(() -> workbench.saveTo(file));
        await(WAIT, () -> file.equals(workbench.getCurrentTemplateFile()));
        assertEquals("تم حفظ القالب في label.label.json", onFx(() -> status(workbench).getText()));
        assertTrue(Files.readString(file).contains("\"First\""));

        runFx(() -> {
            workbench.setTemplate(template("Second"));
            assertEquals("تم تحميل القالب: Second", status(workbench).getText());
            button(workbench, "حفظ").fire();
        });
        await(WAIT, () -> status(workbench).getText().startsWith("تم حفظ"));
        assertTrue(Files.readString(file).contains("\"Second\""), "the second save went to the same file");
    }

    @Test
    void openingAFileLoadsItsTemplateAndAnUnreadableOneIsReported(@TempDir Path directory) {
        Path file = directory.resolve("saved.label.json");
        BarcodeLabelWorkbench writer = onFx(() -> new BarcodeLabelWorkbench(template("Saved label")));
        runFx(() -> writer.saveTo(file));
        await(WAIT, () -> file.equals(writer.getCurrentTemplateFile()));

        BarcodeLabelWorkbench reader = onFx(() -> new BarcodeLabelWorkbench(template("Other")));
        runFx(() -> reader.open(file));
        await(WAIT, () -> reader.getTemplate().name().equals("Saved label"));
        assertEquals(file, onFx(reader::getCurrentTemplateFile));
        assertEquals("تم فتح saved.label.json", onFx(() -> status(reader).getText()));

        Path broken = directory.resolve("broken.label.json");
        runFx(() -> {
            try {
                Files.writeString(broken, "{ not json");
            } catch (java.io.IOException exception) {
                throw new AssertionError(exception);
            }
            reader.open(broken);
        });
        await(WAIT, () -> status(reader).getText().startsWith("تعذر إكمال العملية"));
        assertEquals("Saved label", onFx(() -> reader.getTemplate().name()), "the open template is kept");
    }

    @Test
    void importingACsvShowsItOnTheDataTab(@TempDir Path directory) throws Exception {
        Path csv = directory.resolve("products.csv");
        Files.writeString(csv, "name,sku\nPen,A1\nBook,B2\n");
        BarcodeLabelWorkbench workbench = onFx(() -> new BarcodeLabelWorkbench(template("Products")));
        runFx(() -> workbench.importCsv(csv));
        await(WAIT, () -> workbench.getDataset() != null);
        runFx(() -> {
            assertEquals(2, workbench.getDataset().rows().size());
            assertEquals(1, tabs(workbench).getSelectionModel().getSelectedIndex(), "the data tab is shown");
            assertEquals("تم استيراد سجلان من products.csv", status(workbench).getText());
        });
    }

    private static LabelTemplate template(String name) {
        var page = new PaperPresetRegistry().find("a4-3x7-63.5x38.1").orElseThrow().page();
        var text = new TextElement(ElementFrame.create("name", new RectMm(2, 2, 55, 8), 0),
                "{{name}}", "Arial", 10, "#000000", false, false, TextAlignment.CENTER);
        return LabelTemplate.create(name, page, List.of(text));
    }

    private static Button button(BarcodeLabelWorkbench workbench, String text) {
        return all(workbench.getTop()).filter(node -> node instanceof Button b && b.getText().equals(text))
                .map(Button.class::cast).findFirst().orElseThrow(() -> new AssertionError("no button " + text));
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<PaperPreset> presets(BarcodeLabelWorkbench workbench) {
        return all(workbench.getTop()).filter(ComboBox.class::isInstance).map(node -> (ComboBox<PaperPreset>) node)
                .findFirst().orElseThrow();
    }

    private static TabPane tabs(BarcodeLabelWorkbench workbench) {
        return (TabPane) workbench.getCenter();
    }

    private static Label status(BarcodeLabelWorkbench workbench) {
        return (Label) ((HBox) workbench.getBottom()).getChildren().get(1);
    }

    private static boolean labelShown(BarcodeLabelWorkbench workbench, String text) {
        return all(workbench).anyMatch(node -> node instanceof Label label && label.getText().equals(text));
    }
}
