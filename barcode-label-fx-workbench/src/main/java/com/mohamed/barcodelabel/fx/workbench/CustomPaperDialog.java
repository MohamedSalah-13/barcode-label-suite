package com.mohamed.barcodelabel.fx.workbench;

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

final class CustomPaperDialog extends Dialog<PageDefinition> {
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
        setTitle("مقاس ورق مخصص");
        setHeaderText("جميع القياسات بالمليمتر");
        mode.getItems().setAll(PageMode.SHEET, PageMode.ROLL, PageMode.SINGLE);
        mode.setValue(PageMode.SHEET);
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8); grid.setPadding(new Insets(8));
        int row = 0;
        add(grid, row++, "نوع الورق", mode);
        add(grid, row++, "عرض الصفحة", pageWidth); add(grid, row++, "ارتفاع الصفحة", pageHeight);
        add(grid, row++, "الهامش العلوي", top); add(grid, row++, "الهامش الأيمن", right);
        add(grid, row++, "الهامش السفلي", bottom); add(grid, row++, "الهامش الأيسر", left);
        add(grid, row++, "عدد الأعمدة", columns); add(grid, row++, "عدد الصفوف", rows);
        add(grid, row++, "عرض الملصق", labelWidth); add(grid, row++, "ارتفاع الملصق", labelHeight);
        add(grid, row++, "المسافة الأفقية", horizontalGap); add(grid, row++, "المسافة الرأسية", verticalGap);
        error.getStyleClass().add("validation-error"); error.setWrapText(true);
        grid.add(error, 0, row, 2, 1);
        getDialogPane().setContent(grid);
        ButtonType apply = new ButtonType("تطبيق", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(apply, ButtonType.CANCEL);
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
        var margins = new InsetsMm(number(top, "الهامش العلوي"), number(right, "الهامش الأيمن"),
                number(bottom, "الهامش السفلي"), number(left, "الهامش الأيسر"));
        var size = new DimensionsMm(number(pageWidth, "عرض الصفحة"), number(pageHeight, "ارتفاع الصفحة"));
        if (mode.getValue() == PageMode.SHEET) {
            var layout = new SheetLayout(integer(columns, "عدد الأعمدة"), integer(rows, "عدد الصفوف"),
                    new DimensionsMm(number(labelWidth, "عرض الملصق"), number(labelHeight, "ارتفاع الملصق")),
                    number(horizontalGap, "المسافة الأفقية"), number(verticalGap, "المسافة الرأسية"));
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

    private static double number(TextField field, String name) {
        try { return Double.parseDouble(field.getText().trim()); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException(name + " يجب أن يكون رقمًا صحيحًا"); }
    }
    private static int integer(TextField field, String name) {
        try { return Integer.parseInt(field.getText().trim()); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException(name + " يجب أن يكون عددًا صحيحًا"); }
    }
    private static String message(Throwable error) { return error.getMessage() == null ? "قيم الورق غير صالحة" : error.getMessage(); }
    private static TextField field(String value) { TextField field = new TextField(value); field.setPrefColumnCount(9); return field; }
    private static void add(GridPane grid, int row, String label, Node control) { grid.add(new Label(label), 0, row); grid.add(control, 1, row); }
}
