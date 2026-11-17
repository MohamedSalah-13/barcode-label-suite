package com.mohamed.barcodelabel.fx.workbench;

import com.mohamed.barcodelabel.fx.i18n.UiLanguage;
import com.mohamed.barcodelabel.fx.i18n.UiTexts;
import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.PageMode;
import com.mohamed.barcodelabel.model.page.PageOrientation;
import com.mohamed.barcodelabel.model.page.SheetLayout;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

final class CustomPaperDialog extends Dialog<PageDefinition> {
    private final UiTexts texts;
    private final ComboBox<PageMode> mode = new ComboBox<>();
    private final TextField pageWidth = field("210");
    private final TextField pageHeight = field("297");
    private final TextField top = field("10");
    private final TextField right = field("5");
    private final TextField bottom = field("10");
    private final TextField left = field("5");
    private final TextField columns = field("3");
    private final TextField rows = field("7");
    private final TextField labelWidth = field("63.5");
    private final TextField labelHeight = field("38.1");
    private final TextField horizontalGap = field("2.5");
    private final TextField verticalGap = field("0");
    private final Label error = new Label();

    CustomPaperDialog() {
        this(UiTexts.load(CustomPaperDialog.class, "messages", UiLanguage.ARABIC));
    }

    CustomPaperDialog(UiTexts texts) {
        this.texts = texts;
        getDialogPane().setNodeOrientation(texts.language().orientation());
        setTitle(t("custom.title"));
        setHeaderText(t("custom.header"));
        mode.setConverter(new StringConverter<>() {
            @Override public String toString(PageMode value) { return value == null ? "" : t("mode." + value.name()); }
            @Override public PageMode fromString(String text) { return null; }
        });
        mode.getItems().setAll(PageMode.SHEET, PageMode.ROLL, PageMode.SINGLE);
        mode.setValue(PageMode.SHEET);
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8); grid.setPadding(new Insets(8));
        int row = 0;
        add(grid, row++, t("custom.mode"), mode);
        add(grid, row++, t("custom.pageWidth"), pageWidth); add(grid, row++, t("custom.pageHeight"), pageHeight);
        add(grid, row++, t("custom.top"), top); add(grid, row++, t("custom.right"), right);
        add(grid, row++, t("custom.bottom"), bottom); add(grid, row++, t("custom.left"), left);
        add(grid, row++, t("custom.columns"), columns); add(grid, row++, t("custom.rows"), rows);
        add(grid, row++, t("custom.labelWidth"), labelWidth); add(grid, row++, t("custom.labelHeight"), labelHeight);
        add(grid, row++, t("custom.horizontalGap"), horizontalGap); add(grid, row++, t("custom.verticalGap"), verticalGap);
        error.getStyleClass().add("validation-error"); error.setWrapText(true);
        grid.add(error, 0, row, 2, 1);
        getDialogPane().setContent(grid);
        ButtonType apply = new ButtonType(t("custom.apply"), ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(apply, new ButtonType(t("dialog.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));
        Node applyButton = getDialogPane().lookupButton(apply);
        applyButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try { build(); error.setText(""); }
            catch (RuntimeException exception) { error.setText(message(exception)); event.consume(); }
        });
        mode.valueProperty().addListener((obs, oldValue, value) -> updateMode());
        updateMode();
        setResultConverter(button -> button == apply ? build() : null);
    }

    private PageDefinition build() {
        var margins = new InsetsMm(number(top, "custom.top"), number(right, "custom.right"),
                number(bottom, "custom.bottom"), number(left, "custom.left"));
        var size = new DimensionsMm(number(pageWidth, "custom.pageWidth"), number(pageHeight, "custom.pageHeight"));
        if (mode.getValue() == PageMode.SHEET) {
            var layout = new SheetLayout(integer(columns, "custom.columns"), integer(rows, "custom.rows"),
                    new DimensionsMm(number(labelWidth, "custom.labelWidth"), number(labelHeight, "custom.labelHeight")),
                    number(horizontalGap, "custom.horizontalGap"), number(verticalGap, "custom.verticalGap"));
            return new PageDefinition(PageMode.SHEET, size, margins, PageOrientation.PORTRAIT, layout);
        }
        return new PageDefinition(mode.getValue(), size, margins, PageOrientation.PORTRAIT, null);
    }

    private void updateMode() {
        boolean sheet = mode.getValue() == PageMode.SHEET;
        columns.setDisable(!sheet); rows.setDisable(!sheet); labelWidth.setDisable(!sheet);
        labelHeight.setDisable(!sheet); horizontalGap.setDisable(!sheet); verticalGap.setDisable(!sheet);
        if (!sheet) { top.setText("0"); right.setText("0"); bottom.setText("0"); left.setText("0"); }
    }

    private double number(TextField field, String nameKey) {
        try { return Double.parseDouble(field.getText().trim()); }
        catch (NumberFormatException exception) { throw new InvalidInput(t("custom.notANumber", t(nameKey))); }
    }
    private int integer(TextField field, String nameKey) {
        try { return Integer.parseInt(field.getText().trim()); }
        catch (NumberFormatException exception) { throw new InvalidInput(t("custom.notAnInteger", t(nameKey))); }
    }
    /**
     * The field checks above already explain themselves in the UI language; the model's own checks are in
     * English, so the common ones are translated and the rest are shown after a translated lead-in.
     */
    private String message(Throwable error) {
        String text = error.getMessage();
        if (text == null) return t("custom.invalid");
        if (error instanceof InvalidInput) return text;
        return switch (text) {
            case "sheet layout does not fit inside the page margins" -> t("custom.doesNotFit");
            case "margins must leave a positive printable area" -> t("custom.noPrintableArea");
            default -> t("custom.invalidDetail", text);
        };
    }

    private String t(String key, Object... arguments) { return texts.get(key, arguments); }

    /** A field that does not hold a number, explained in the UI language. */
    private static final class InvalidInput extends IllegalArgumentException {
        InvalidInput(String message) { super(message); }
    }
    private static TextField field(String value) { TextField field = new TextField(value); field.setPrefColumnCount(9); return field; }
    private static void add(GridPane grid, int row, String label, Node control) { grid.add(new Label(label), 0, row); grid.add(control, 1, row); }
}
