package com.mohamed.barcodelabel.fx.workbench;

import com.mohamed.barcodelabel.fx.i18n.UiTexts;
import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.print.PrintOffset;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.math.BigDecimal;
import java.util.prefs.BackingStoreException;

final class CalibrationDialog extends Dialog<Void> {
    final Button testSheet = new Button();
    final Label jobStatus = new Label();

    CalibrationDialog(UiTexts texts, String printer, DimensionsMm size, CalibrationStore store) {
        setTitle(texts.get("calibration.title"));
        setHeaderText(texts.get("calibration.stock", printer, mm(size.width()), mm(size.height())));
        getDialogPane().setNodeOrientation(texts.language().orientation());
        var saved = store.load(printer, size);
        TextField horizontal = field(saved.horizontalMm());
        TextField vertical = field(saved.verticalMm());
        Label hint = new Label(texts.get("calibration.hint", mm(CalibrationSheet.referenceMm(size.width())),
                mm(CalibrationSheet.referenceMm(size.height()))));
        hint.setWrapText(true);
        hint.setPrefWidth(420);
        testSheet.setText(texts.get("calibration.test"));
        jobStatus.setWrapText(true);
        Label error = new Label();
        error.setWrapText(true);
        error.getStyleClass().add("validation-error");
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(8)); grid.setHgap(12); grid.setVgap(8);
        grid.add(testSheet, 0, 0, 2, 1);
        grid.add(hint, 0, 1, 2, 1);
        grid.addRow(2, new Label(texts.get("calibration.horizontal")), horizontal);
        grid.addRow(3, new Label(texts.get("calibration.vertical")), vertical);
        grid.add(jobStatus, 0, 4, 2, 1);
        grid.add(error, 0, 5, 2, 1);
        getDialogPane().setContent(grid);
        ButtonType save = new ButtonType(texts.get("calibration.save"), ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().setAll(save, new ButtonType(texts.get("dialog.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));
        getDialogPane().lookupButton(save).addEventFilter(ActionEvent.ACTION, event -> {
            try {
                store.save(printer, size, new PrintOffset(parse(horizontal), parse(vertical)));
            } catch (IllegalArgumentException invalid) {
                error.setText(texts.get("calibration.invalid"));
                event.consume();
            } catch (BackingStoreException | SecurityException failure) {
                error.setText(texts.get("calibration.saveFailed", failure.getMessage()));
                event.consume();
            }
        });
        setResultConverter(button -> null);
    }

    private static TextField field(double value) {
        TextField field = new TextField(mm(value));
        field.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        field.setPrefColumnCount(8);
        return field;
    }

    private static double parse(TextField field) {
        String input = field.getText().strip().replace('٫', '.').replace(',', '.').replace('−', '-');
        return new BigDecimal(input).doubleValue();
    }

    private static String mm(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
