package com.mohamed.barcodelabel.fx.workbench;

import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.PageMode;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.mohamed.barcodelabel.fx.workbench.FxSupport.runFx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomPaperDialogTest {
    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        FxSupport.startToolkit();
    }

    @Test
    void theDefaultsDescribeAThreeBySevenA4Sheet() {
        runFx(() -> {
            CustomPaperDialog dialog = new CustomPaperDialog();
            PageDefinition page = applied(dialog);
            assertEquals(PageMode.SHEET, page.mode());
            assertEquals(210, page.pageSize().width(), 1e-9);
            assertEquals(297, page.pageSize().height(), 1e-9);
            assertEquals(21, page.sheetLayout().labelsPerPage());
            assertEquals(63.5, page.labelSize().width(), 1e-9);
            assertNull(dialog.getResultConverter().call(ButtonType.CANCEL), "cancel gives no paper");
        });
    }

    @Test
    void aRollHasNoSheetLayoutAndNoMargins() {
        runFx(() -> {
            CustomPaperDialog dialog = new CustomPaperDialog();
            mode(dialog).setValue(PageMode.ROLL);
            assertTrue(field(dialog, "عدد الأعمدة").isDisabled());
            assertEquals("0", field(dialog, "الهامش العلوي").getText());
            field(dialog, "عرض الصفحة").setText("50");
            field(dialog, "ارتفاع الصفحة").setText("30");

            PageDefinition page = applied(dialog);
            assertEquals(PageMode.ROLL, page.mode());
            assertNull(page.sheetLayout());
            assertEquals(50, page.labelSize().width(), 1e-9);

            mode(dialog).setValue(PageMode.SHEET);
            assertFalse(field(dialog, "عدد الأعمدة").isDisabled());
        });
    }

    @Test
    void invalidInputIsExplainedInArabicAndKeepsTheDialogOpen() {
        runFx(() -> {
            CustomPaperDialog dialog = new CustomPaperDialog();
            field(dialog, "عرض الصفحة").setText("abc");
            assertTrue(pressApply(dialog), "the click is consumed, so the dialog stays open");
            assertEquals("عرض الصفحة يجب أن يكون رقمًا، مثل 63.5", error(dialog).getText());

            field(dialog, "عرض الصفحة").setText("210");
            field(dialog, "عدد الأعمدة").setText("3.5");
            pressApply(dialog);
            assertEquals("عدد الأعمدة يجب أن يكون عددًا صحيحًا", error(dialog).getText());

            field(dialog, "عدد الأعمدة").setText("3");
            field(dialog, "عرض الملصق").setText("300");
            pressApply(dialog);
            assertEquals("الملصقات لا تتسع داخل هوامش الصفحة", error(dialog).getText());

            field(dialog, "عرض الملصق").setText("0");
            pressApply(dialog);
            assertTrue(error(dialog).getText().startsWith("قيم الورق غير صالحة: "), error(dialog).getText());

            field(dialog, "عرض الملصق").setText("63.5");
            assertFalse(pressApply(dialog), "valid values go through");
            assertEquals("", error(dialog).getText());
        });
    }

    private static PageDefinition applied(CustomPaperDialog dialog) {
        return dialog.getResultConverter().call(applyType(dialog));
    }

    private static ButtonType applyType(CustomPaperDialog dialog) {
        return dialog.getDialogPane().getButtonTypes().stream()
                .filter(type -> type.getButtonData() == ButtonBar.ButtonData.OK_DONE).findFirst().orElseThrow();
    }

    /** Clicks Apply and says whether the dialog's validation consumed the click. */
    private static boolean pressApply(CustomPaperDialog dialog) {
        Button apply = (Button) dialog.getDialogPane().lookupButton(applyType(dialog));
        boolean[] reachedButton = {false};
        apply.addEventHandler(javafx.event.ActionEvent.ACTION, event -> reachedButton[0] = true);
        apply.fire();
        return !reachedButton[0];
    }

    private static GridPane grid(CustomPaperDialog dialog) {
        return (GridPane) dialog.getDialogPane().getContent();
    }

    private static Node control(CustomPaperDialog dialog, String label) {
        GridPane grid = grid(dialog);
        Node caption = grid.getChildren().stream()
                .filter(node -> node instanceof Label l && l.getText().equals(label)).findFirst().orElseThrow();
        Integer row = GridPane.getRowIndex(caption);
        return grid.getChildren().stream()
                .filter(node -> node != caption && row.equals(GridPane.getRowIndex(node))).findFirst().orElseThrow();
    }

    private static TextField field(CustomPaperDialog dialog, String label) {
        return (TextField) control(dialog, label);
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<PageMode> mode(CustomPaperDialog dialog) {
        return (ComboBox<PageMode>) control(dialog, "نوع الورق");
    }

    private static Label error(CustomPaperDialog dialog) {
        return grid(dialog).getChildren().stream()
                .filter(node -> node.getStyleClass().contains("validation-error")).map(Label.class::cast)
                .findFirst().orElseThrow();
    }
}
