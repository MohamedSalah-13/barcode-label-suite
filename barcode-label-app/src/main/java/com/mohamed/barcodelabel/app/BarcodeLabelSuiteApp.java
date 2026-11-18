package com.mohamed.barcodelabel.app;

import com.mohamed.barcodelabel.fx.batch.BatchProductionWorkbench;
import com.mohamed.barcodelabel.fx.workbench.CustomPaperStore;
import com.mohamed.barcodelabel.fx.i18n.UiLanguage;
import com.mohamed.barcodelabel.fx.i18n.UiTexts;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.*;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.workflow.paper.PaperPresetRegistry;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.List;
import java.util.prefs.Preferences;

/**
 * The desktop application: the production workbench in the user's language, with a language menu at the end of
 * the tab bar. The language is remembered between runs.
 */
public final class BarcodeLabelSuiteApp extends Application {
    private final LanguageSetting languageSetting;
    /** The paper sizes the user named, kept between runs. */
    private final CustomPaperStore paperSizes;
    /** The label being designed when the app last closed, opened again on start. */
    private final LastDesign lastDesign;
    private Stage stage;
    private UiLanguage language;
    private UiTexts texts;
    private LabelTemplate startingTemplate;
    private BatchProductionWorkbench workbench;
    private ComboBox<UiLanguage> languagePicker;

    public BarcodeLabelSuiteApp() {
        this(LanguageSetting.preferences(),
                CustomPaperStore.preferences(Preferences.userNodeForPackage(BarcodeLabelSuiteApp.class).node("paperSizes")),
                LastDesign.userFile());
    }

    BarcodeLabelSuiteApp(LanguageSetting languageSetting, CustomPaperStore paperSizes,
                         LastDesign lastDesign) {
        this.languageSetting = languageSetting;
        this.paperSizes = paperSizes;
        this.lastDesign = lastDesign;
    }

    @Override public void start(Stage stage) {
        this.stage = stage;
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        language = languageSetting.load();
        startingTemplate = template(language);
        // The label from the last run, if there is one; it is the user's, so a language switch keeps it as it is.
        LabelTemplate first = lastDesign.load().orElse(startingTemplate);
        stage.setScene(new Scene(root(first), 1480, 940));
        stage.setTitle("Barcode Label Suite");
        stage.getIcons().setAll(icons());
        stage.setMinWidth(1150); stage.setMinHeight(750); stage.show();
    }

    /**
     * Keeps the label being designed for the next run, unless it is still the untouched starting label, which the
     * next run makes again in its own language.
     */
    @Override public void stop() {
        if (workbench == null) return;
        LabelTemplate current = workbench.getTemplate();
        if (current != startingTemplate) lastDesign.save(current);
    }

    /**
     * The window icon in each size scripts/AppIcon.java draws; the system picks the one that fits the title bar,
     * the taskbar or the task switcher.
     */
    static List<Image> icons() {
        return java.util.stream.IntStream.of(16, 24, 32, 48, 64, 128, 256)
                .mapToObj(size -> BarcodeLabelSuiteApp.class.getResource("icons/app-" + size + ".png"))
                .filter(java.util.Objects::nonNull)
                .map(url -> new Image(url.toExternalForm()))
                .toList();
    }

    UiLanguage language() { return language; }
    BatchProductionWorkbench workbench() { return workbench; }
    ComboBox<UiLanguage> languagePicker() { return languagePicker; }

    /**
     * Rebuilds the window in another language. The label being designed is kept; if it is still the untouched
     * starting label, the other language's starting label replaces it, so its texts follow the language too.
     */
    void switchLanguage(UiLanguage next) {
        if (next == null || next == language) return;
        if (languagePicker.isDisabled()) {
            languagePicker.setValue(language);
            return;
        }
        var session = workbench.snapshotSession();
        LabelTemplate current = workbench.getTemplate();
        language = next;
        languageSetting.save(next);
        if (current == startingTemplate) current = startingTemplate = template(next);
        stage.getScene().setRoot(root(current));
        workbench.restoreSession(session);
    }

    private Parent root(LabelTemplate template) {
        texts = UiTexts.load(BarcodeLabelSuiteApp.class, "messages", language);
        workbench = new BatchProductionWorkbench(template, language);
        workbench.labelWorkbench().setCustomPaperStore(paperSizes);
        languagePicker = new ComboBox<>();
        // Finish file operations and batch preparation before replacing their UI callbacks.
        languagePicker.disableProperty().bind(workbench.labelWorkbench().printPane().printingProperty()
                .or(workbench.labelWorkbench().workingProperty()).or(workbench.batchPane().workingProperty()));
        languagePicker.getItems().setAll(UiLanguage.values());
        languagePicker.setValue(language);
        languagePicker.setTooltip(new Tooltip(texts.get("app.language")));
        languagePicker.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        languagePicker.valueProperty().addListener((obs, oldValue, value) -> switchLanguage(value));
        // At the end of the tab bar, where the tabs leave room: top left in Arabic, top right in English.
        StackPane root = new StackPane(workbench, languagePicker);
        root.setNodeOrientation(language.orientation());
        StackPane.setAlignment(languagePicker, Pos.TOP_RIGHT);
        StackPane.setMargin(languagePicker, new Insets(3, 8, 0, 8));
        return root;
    }

    /** The Arabic starting template; its fields match the sample CSV in the README. */
    static LabelTemplate template() { return template(UiLanguage.ARABIC); }

    /** The template the app opens with, named and worded in this language; the fields are the same in both. */
    static LabelTemplate template(UiLanguage language) {
        UiTexts texts = UiTexts.load(BarcodeLabelSuiteApp.class, "messages", language);
        var page = new PaperPresetRegistry().find("a4-3x7-63.5x38.1").orElseThrow().page();
        var border = new ShapeElement(ElementFrame.create(texts.get("element.border"), new RectMm(1, 1, 61.5, 36.1), 0), ShapeType.ROUNDED_RECTANGLE, "#CBD5E1", "transparent", 0.25);
        var name = new TextElement(ElementFrame.create(texts.get("element.product"), new RectMm(4, 3, 55, 7), 1), "{{product_name}}", "Arial", 11, "#111827", true, false, TextAlignment.CENTER);
        var barcode = new BarcodeElement(ElementFrame.create(texts.get("element.barcode"), new RectMm(4, 11, 55.5, 19), 2), "{{sku}}", Symbology.CODE_128, 0.25, 2.5, true);
        var price = new TextElement(ElementFrame.create(texts.get("element.price"), new RectMm(10, 31, 43.5, 5), 3), texts.get("template.price"), "Arial", 9, "#B91C1C", true, false, TextAlignment.CENTER);
        return LabelTemplate.create(texts.get("template.name"), page, List.of(border, name, barcode, price));
    }

    public static void main(String[] args) { launch(args); }
}
