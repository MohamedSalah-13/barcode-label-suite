package com.mohamed.barcodelabel.fx.workbench;

import com.mohamed.barcodelabel.fx.i18n.UiLanguage;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.PageMode;
import com.mohamed.barcodelabel.model.page.PageOrientation;
import com.mohamed.barcodelabel.print.LabelPrintRequest;
import com.mohamed.barcodelabel.print.PrintOffset;
import com.mohamed.barcodelabel.print.PrintResult;
import com.mohamed.barcodelabel.print.java2d.SheetPageRenderer;
import com.mohamed.barcodelabel.print.java2d.SheetPreviewRequest;
import com.mohamed.barcodelabel.workflow.paper.PaperPresetRegistry;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

class CalibrationTest {
    private final Preferences prefs = Preferences.userRoot().node("com/mohamed/barcodelabel/test/" + UUID.randomUUID());
    private final CalibrationStore store = new CalibrationStore(prefs);
    private final PageDefinition a4 = new PaperPresetRegistry().all().getFirst().page();

    @BeforeAll static void startFx() throws InterruptedException { FxSupport.startToolkit(); }
    @AfterEach void removePrefs() throws Exception { prefs.removeNode(); prefs.flush(); }

    @Test void offsetsAreRememberedByPrinterAndPhysicalPageSize() throws Exception {
        String office = "مخزن/Office ".repeat(12);
        var roll = new DimensionsMm(50, 30);
        store.save(office, a4.pageSize(), new PrintOffset(1.5, -2));
        store.save(office, roll, new PrintOffset(-3, 0.25));
        var reopened = new CalibrationStore(prefs);
        assertEquals(new PrintOffset(1.5, -2), reopened.load(office, new DimensionsMm(210, 297)));
        assertEquals(new PrintOffset(-3, 0.25), reopened.load(office, roll));
        assertEquals(PrintOffset.ZERO, reopened.load("Other printer", roll));
        reopened.save(office, roll, PrintOffset.ZERO);
        assertEquals(PrintOffset.ZERO, store.load(office, roll));
        assertEquals(new PrintOffset(1.5, -2), store.load(office, a4.pageSize()));
    }

    @Test void theTestCrossFitsSmallStockAndIsMeasurableFromThePageEdges() throws Exception {
        var roll = new PageDefinition(PageMode.ROLL, new DimensionsMm(8, 12), InsetsMm.ZERO, PageOrientation.PORTRAIT, null);
        var test = CalibrationSheet.request("Thermal", roll, "Test sheet");
        var image = new SheetPageRenderer().render(new SheetPreviewRequest(test.template(), test.records(), 0, 0, 254, false));
        assertEquals(80, image.getWidth());
        assertEquals(120, image.getHeight());
        // At 10 px/mm the centre is 4 mm from the left and 6 mm from the top.
        assertEquals(0, image.getRGB(40, 60) & 255);
        assertEquals(0, image.getRGB(40, 48) & 255);
        assertEquals(0, image.getRGB(52, 60) & 255);
        assertEquals(255, image.getRGB(52, 48) & 255);
    }

    @Test void savingInTheDialogCalibratesRealJobsButTheTestSheetAlwaysUsesZero() {
        var sent = new AtomicReference<LabelPrintRequest>();
        FxSupport.runFx(() -> {
            var pane = new UniversalPrintPane(UiLanguage.ENGLISH, request -> {
                sent.set(request);
                return CompletableFuture.completedFuture(new PrintResult(1, 1));
            }, store);
            var template = LabelTemplate.create("Products", a4, List.of());
            pane.setTemplate(template);
            @SuppressWarnings("unchecked")
            var printers = (ComboBox<String>) FxSupport.all(pane.getRight()).filter(ComboBox.class::isInstance)
                    .findFirst().orElseThrow();
            printers.getItems().setAll("Office", "Other");
            printers.setValue("Office");
            var dialog = pane.calibrationDialog();
            var fields = fields(dialog);
            fields.get(0).setText("−١٫٥");
            fields.get(1).setText("oops");
            save(dialog);
            assertEquals(PrintOffset.ZERO, store.load("Office", a4.pageSize()));
            fields.get(1).setText("+2.25");
            save(dialog);
            var saved = new PrintOffset(-1.5, 2.25);
            assertEquals(saved, store.load("Office", a4.pageSize()));
            printers.setValue("Other");
            assertEquals("0", fields(pane.calibrationDialog()).getFirst().getText());
            printers.setValue("Office");
            assertEquals("-1.5", fields(pane.calibrationDialog()).getFirst().getText());
            var job = new LabelPrintRequest(template, List.of(Map.of()), "Office", 3, 4, 1, 1);
            pane.submitPrint(job);
            assertEquals(job.withOffset(saved), sent.get());
            assertTrue(pane.calibrationDialog().testSheet.isDisabled());
        });
        FxSupport.runFx(() -> {
            var pane = new UniversalPrintPane(UiLanguage.ENGLISH, request -> {
                sent.set(request);
                return CompletableFuture.completedFuture(new PrintResult(1, 1));
            }, new CalibrationStore(prefs));
            pane.setTemplate(LabelTemplate.create("Another design", a4, List.of()));
            @SuppressWarnings("unchecked")
            var printers = (ComboBox<String>) FxSupport.all(pane.getRight()).filter(ComboBox.class::isInstance)
                    .findFirst().orElseThrow();
            printers.setValue("Office");
            pane.setBatchValidationRequired(true);
            pane.calibrationDialog().testSheet.fire();
            var test = sent.get();
            assertEquals("Office", test.printerName());
            assertEquals(PrintOffset.ZERO, test.offset());
            assertEquals(a4.pageSize(), test.template().page().pageSize());
            assertEquals(List.of(Map.of()), test.records());
            assertEquals(1, test.copies());
            assertEquals(0, test.startSlotIndex());
            assertNotEquals("Another design", test.template().name());
        });
    }

    private static List<TextField> fields(CalibrationDialog dialog) {
        return FxSupport.all(dialog.getDialogPane().getContent()).filter(TextField.class::isInstance)
                .map(TextField.class::cast).toList();
    }

    private static void save(CalibrationDialog dialog) {
        var pane = dialog.getDialogPane();
        ((Button) pane.lookupButton(pane.getButtonTypes().getFirst())).fire();
    }
}
