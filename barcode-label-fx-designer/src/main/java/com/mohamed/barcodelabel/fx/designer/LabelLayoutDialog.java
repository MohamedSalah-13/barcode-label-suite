package com.mohamed.barcodelabel.fx.designer;

import com.mohamed.barcodelabel.fx.designer.model.TemplateEditSession;
import com.mohamed.barcodelabel.fx.i18n.UiTexts;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.*;
import com.mohamed.barcodelabel.model.geometry.*;
import com.mohamed.barcodelabel.model.page.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import java.util.List;
import java.util.Locale;

/** Edits one complete label; copy placement never changes the production quantity. */
final class LabelLayoutDialog extends Dialog<LabelTemplate> {
    private final UiTexts texts;
    private final LabelTemplate original;
    private final CheckBox dual = new CheckBox();
    private final CheckBox centred = new CheckBox();
    private final CheckBox guide = new CheckBox();
    private final CheckBox dashed = new CheckBox();
    private final CheckBox sample = new CheckBox();
    private final TextField width = field("width"), height = field("height"), split = field("split");
    private final TextField top = field("top"), right = field("right"), bottom = field("bottom"), left = field("left");
    private final TextField gap = field("gap");
    private final ColorPicker color = new ColorPicker();
    private final Label error = new Label(), summary = new Label();
    private final StackPane preview = new StackPane();
    private LabelTemplate candidate;
    private boolean filling;

    LabelLayoutDialog(LabelTemplate original, UiTexts texts) {
        this.original = original;
        this.texts = texts;
        setTitle(t("layout.title"));
        setHeaderText(t("layout.hint"));
        getDialogPane().setNodeOrientation(texts.language().orientation());
        dual.setText(t("layout.dual")); dual.setId("layout-dual");
        centred.setText(t("layout.centred")); centred.setId("layout-centred");
        guide.setText(t("layout.guide")); guide.setId("layout-guide");
        dashed.setText(t("layout.dashed"));
        sample.setText(t("layout.replace")); sample.setId("layout-sample");
        sample.setWrapText(true);
        Button preset = new Button(t("layout.preset")); preset.setId("layout-preset");
        preset.setOnAction(e -> usePreset());
        GridPane form = new GridPane(); form.setHgap(10); form.setVgap(8); form.setPadding(new Insets(10));
        form.add(dual, 0, 0, 2, 1);
        row(form, 1, "width", width); row(form, 2, "height", height);
        row(form, 3, "top", top); row(form, 4, "right", right);
        row(form, 5, "bottom", bottom); row(form, 6, "left", left);
        form.add(centred, 0, 7, 2, 1);
        row(form, 8, "split", split); row(form, 9, "gap", gap);
        form.add(guide, 0, 10, 2, 1); row(form, 11, "color", color);
        form.add(dashed, 0, 12, 2, 1);
        form.add(preset, 0, 13, 2, 1); form.add(sample, 0, 14, 2, 1);
        ScrollPane scroll = new ScrollPane(form); scroll.setFitToWidth(true); scroll.setPrefViewportHeight(470);
        summary.setWrapText(true); error.setWrapText(true); error.getStyleClass().add("validation-error");
        preview.setMinSize(200, 150);
        ScrollPane previewScroll = new ScrollPane(preview); previewScroll.setPrefViewportWidth(360); previewScroll.setPrefViewportHeight(300);
        VBox view = new VBox(10, summary, previewScroll, error); view.setPrefWidth(370); view.setPadding(new Insets(10));
        HBox body = new HBox(10, scroll, view);
        getDialogPane().setContent(body);
        ButtonType apply = new ButtonType(t("layout.apply"), ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().setAll(apply, new ButtonType(t("layout.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));
        getDialogPane().lookupButton(apply).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            refresh();
            if (candidate == null || (candidate.dualLayout() != null && !candidate.contentFits())) event.consume();
        });
        setResultConverter(button -> button == apply ? candidate : null);
        fill();
        for (TextField input : List.of(width, height, top, right, bottom, left, split, gap))
            input.textProperty().addListener((o, a, b) -> refresh());
        for (CheckBox input : List.of(dual, centred, guide, dashed, sample))
            input.selectedProperty().addListener((o, a, b) -> refresh());
        color.valueProperty().addListener((o, a, b) -> refresh());
        refresh();
    }

    private void fill() {
        var layout = original.dualLayout();
        var size = original.page().sheetLayout() == null ? original.page().pageSize() : original.page().labelSize();
        width.setText(mm(size.width())); height.setText(mm(size.height()));
        var defaults = layout == null ? DualLabelLayout.standard() : layout;
        var margins = layout == null ? original.page().margins() : layout.margins();
        setMargins(margins);
        dual.setSelected(layout != null); centred.setSelected(defaults.splitPositionMm() == null);
        split.setText(mm(defaults.splitAt(size))); gap.setText(mm(defaults.gapMm()));
        guide.setSelected(defaults.showGuide()); dashed.setSelected(defaults.dashedGuide());
        color.setValue(Color.web(defaults.guideColor()));
    }

    private void usePreset() {
        filling = true;
        width.setText("40"); height.setText("25"); dual.setSelected(true); centred.setSelected(true);
        split.setText("12.5"); gap.setText("1"); setMargins(DualLabelLayout.standard().margins());
        guide.setSelected(true); dashed.setSelected(true); sample.setSelected(true);
        filling = false;
        refresh();
    }

    private void setMargins(InsetsMm m) {
        top.setText(mm(m.top())); right.setText(mm(m.right())); bottom.setText(mm(m.bottom())); left.setText(mm(m.left()));
    }

    private void refresh() {
        if (filling) return;
        split.setDisable(!dual.isSelected() || centred.isSelected());
        centred.setDisable(!dual.isSelected()); gap.setDisable(!dual.isSelected());
        guide.setDisable(!dual.isSelected()); color.setDisable(!dual.isSelected() || !guide.isSelected());
        dashed.setDisable(color.isDisabled()); sample.setDisable(!dual.isSelected());
        candidate = null;
        try {
            if (centred.isSelected()) {
                filling = true;
                try { split.setText(mm(number(height) / 2)); }
                finally { filling = false; }
            }
            candidate = build();
            var size = candidate.designSize();
            summary.setText(t(dual.isSelected() ? "layout.summaryDual" : "layout.summarySingle", mm(size.width()), mm(size.height())));
            DesignerCanvas canvas = new DesignerCanvas(new TemplateEditSession(candidate), texts);
            canvas.setZoom(Math.max(0.5, Math.min(2, 320 / (candidate.page().labelSize().width() * 4))));
            canvas.setMouseTransparent(true);
            preview.getChildren().setAll(canvas);
            error.setText(candidate.dualLayout() != null && !candidate.contentFits() ? t("layout.overflow") : "");
        } catch (RuntimeException exception) {
            candidate = null;
            error.setText(t("layout.invalid"));
            preview.getChildren().clear();
        }
    }

    LabelTemplate build() {
        var size = new DimensionsMm(number(width), number(height));
        var margins = new InsetsMm(number(top), number(right), number(bottom), number(left));
        DualLabelLayout layout = dual.isSelected() ? new DualLabelLayout(margins,
                centred.isSelected() ? null : number(split), number(gap), guide.isSelected(),
                String.format(Locale.ROOT, "#%02X%02X%02X", Math.round(color.getValue().getRed() * 255),
                        Math.round(color.getValue().getGreen() * 255), Math.round(color.getValue().getBlue() * 255)), dashed.isSelected()) : null;
        var oldPage = original.page();
        PageDefinition page;
        if (oldPage.mode() == PageMode.SHEET) {
            var old = oldPage.sheetLayout();
            page = new PageDefinition(PageMode.SHEET, oldPage.pageSize(), layout == null ? margins : oldPage.margins(),
                    oldPage.orientation(), new SheetLayout(old.columns(), old.rows(), size, old.horizontalGap(), old.verticalGap()));
        } else {
            page = new PageDefinition(oldPage.mode(), size, layout == null ? margins : InsetsMm.ZERO, oldPage.orientation(), null);
        }
        var result = original.withLayout(page, layout);
        if (layout != null && sample.isSelected()) result = result.withElements(sampleElements(result.designSize(), texts));
        return result;
    }

    static List<LabelElement> sampleElements(DimensionsMm size, UiTexts texts) {
        double w = size.width(), h = size.height();
        if (w < 30 || h < 9) throw new IllegalArgumentException("sample needs at least 30 x 9 mm");
        return List.of(
                new TextElement(ElementFrame.create(texts.get("layout.product"), new RectMm(0, 0, w * 0.62, 2.5), 0),
                        texts.get("layout.sampleProduct"), "Arial", 6.5, "#000000", true, false, TextAlignment.LEFT),
                new TextElement(ElementFrame.create(texts.get("layout.price"), new RectMm(w * 0.64, 0, w * 0.36, 2.5), 1),
                        texts.get("layout.samplePrice"), "Arial", 6.5, "#000000", true, false, TextAlignment.RIGHT),
                new BarcodeElement(ElementFrame.create(texts.get("element.barcode"), new RectMm(0, 2.75, w, h - 2.75), 2),
                        "100025", Symbology.CODE_128, 0.25, 2.5, true));
    }

    private void row(GridPane grid, int row, String key, javafx.scene.Node input) {
        grid.add(new Label(t("layout." + key)), 0, row); grid.add(input, 1, row);
    }
    private String t(String key, Object... values) { return texts.get(key, values); }
    private static TextField field(String name) {
        TextField input = new TextField(); input.setPrefColumnCount(7); input.setId("layout-" + name); return input;
    }
    private static double number(TextField input) { return Double.parseDouble(input.getText().strip()); }
    private static String mm(double value) { return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString(); }
}
