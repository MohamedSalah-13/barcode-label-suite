package com.mohamed.barcodelabel.fx.designer;

import com.mohamed.barcodelabel.data.TemplateJsonService;
import com.mohamed.barcodelabel.fx.designer.model.EditorAlignment;
import com.mohamed.barcodelabel.fx.designer.model.TemplateEditSession;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.LabelElement;
import com.mohamed.barcodelabel.model.element.ShapeElement;
import com.mohamed.barcodelabel.model.element.ShapeType;
import com.mohamed.barcodelabel.model.element.Symbology;
import com.mohamed.barcodelabel.model.element.TextAlignment;
import com.mohamed.barcodelabel.model.element.TextElement;
import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;
import com.mohamed.barcodelabel.model.geometry.RectMm;
import com.mohamed.barcodelabel.model.page.PageDefinition;
import com.mohamed.barcodelabel.model.page.SheetLayout;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class LabelDesignerPane extends BorderPane {
    private final ReadOnlyObjectWrapper<LabelTemplate> template = new ReadOnlyObjectWrapper<>(this, "template");
    private final VBox propertiesPanel = new VBox(10);
    private final Label selectionTitle = new Label("لا يوجد عنصر محدد");
    private final TextField xField = numberField();
    private final TextField yField = numberField();
    private final TextField widthField = numberField();
    private final TextField heightField = numberField();
    private final TextField contentField = new TextField();
    private final CheckBox locked = new CheckBox("مقفل");
    private final CheckBox visible = new CheckBox("ظاهر");
    private final Button undoButton = new Button("تراجع");
    private final Button redoButton = new Button("إعادة");
    private final Label status = new Label("جاهز");
    private TemplateEditSession session;
    private DesignerCanvas canvas;
    private ScrollPane workspace;
    private boolean refreshingProperties;

    public LabelDesignerPane() {
        this(defaultTemplate());
    }

    public LabelDesignerPane(LabelTemplate initialTemplate) {
        setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        getStyleClass().add("label-designer-pane");
        setTop(createToolbar());
        setLeft(createToolbox());
        setRight(createPropertiesPanel());
        setBottom(createStatusBar());
        setTemplate(initialTemplate);
        loadStylesheet();
    }

    public LabelTemplate getTemplate() { return template.get(); }
    public ReadOnlyObjectProperty<LabelTemplate> templateProperty() { return template.getReadOnlyProperty(); }

    public void setTemplate(LabelTemplate value) {
        session = new TemplateEditSession(value);
        canvas = new DesignerCanvas(session);
        workspace = new ScrollPane(canvas);
        workspace.getStyleClass().add("designer-workspace");
        workspace.setPannable(true);
        setCenter(workspace);
        session.addListener(() -> {
            template.set(session.template());
            updateHistoryButtons();
            refreshProperties();
            status.setText(session.selection().size() + " عنصر محدد");
        });
        template.set(value);
        updateHistoryButtons();
        refreshProperties();
    }

    public void save(Path path) throws IOException {
        new TemplateJsonService().write(path, getTemplate());
        status.setText("تم حفظ القالب: " + path.getFileName());
    }

    private ToolBar createToolbar() {
        undoButton.setOnAction(event -> session.undo());
        redoButton.setOnAction(event -> session.redo());
        Button duplicate = button("تكرار", () -> session.duplicateSelected(2));
        Button delete = button("حذف", session::deleteSelected);
        Button left = button("يسار", () -> session.align(EditorAlignment.LEFT));
        Button center = button("توسيط أفقي", () -> session.align(EditorAlignment.HORIZONTAL_CENTER));
        Button right = button("يمين", () -> session.align(EditorAlignment.RIGHT));
        Button top = button("أعلى", () -> session.align(EditorAlignment.TOP));
        Button middle = button("توسيط رأسي", () -> session.align(EditorAlignment.VERTICAL_CENTER));
        Button bottom = button("أسفل", () -> session.align(EditorAlignment.BOTTOM));
        return new ToolBar(undoButton, redoButton, new Separator(), duplicate, delete,
                new Separator(), left, center, right, top, middle, bottom);
    }

    private VBox createToolbox() {
        Label title = new Label("العناصر");
        title.getStyleClass().add("panel-title");
        Button text = button("+ نص", this::addText);
        Button barcode = button("+ باركود", () -> addBarcode(Symbology.CODE_128));
        Button qr = button("+ QR Code", () -> addBarcode(Symbology.QR_CODE));
        Button rectangle = button("+ مستطيل", this::addRectangle);
        for (Button button : List.of(text, barcode, qr, rectangle)) button.setMaxWidth(Double.MAX_VALUE);
        VBox toolbox = new VBox(8, title, text, barcode, qr, rectangle, new Separator(),
                new Label("Ctrl + Click للتحديد المتعدد"),
                new Label("الأسهم: 0.1 مم"),
                new Label("Shift + الأسهم: 1 مم"));
        toolbox.getStyleClass().add("designer-toolbox");
        toolbox.setPrefWidth(190);
        return toolbox;
    }

    private VBox createPropertiesPanel() {
        Label title = new Label("الخصائص");
        title.getStyleClass().add("panel-title");
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        addPropertyRow(grid, 0, "X (mm)", xField);
        addPropertyRow(grid, 1, "Y (mm)", yField);
        addPropertyRow(grid, 2, "العرض", widthField);
        addPropertyRow(grid, 3, "الارتفاع", heightField);
        addPropertyRow(grid, 4, "النص/البيانات", contentField);
        HBox state = new HBox(12, visible, locked);
        Button apply = button("تطبيق الخصائص", this::applyProperties);
        apply.getStyleClass().add("primary-button");
        apply.setMaxWidth(Double.MAX_VALUE);
        propertiesPanel.getChildren().setAll(title, selectionTitle, grid, state, apply);
        propertiesPanel.getStyleClass().add("designer-properties");
        propertiesPanel.setPrefWidth(260);
        return propertiesPanel;
    }

    private HBox createStatusBar() {
        CheckBox snap = new CheckBox("التصاق بالشبكة");
        snap.setSelected(true);
        snap.selectedProperty().addListener((observable, oldValue, newValue) -> canvas.setSnapEnabled(newValue));
        Slider zoom = new Slider(0.5, 2.5, 1);
        zoom.setPrefWidth(180);
        zoom.valueProperty().addListener((observable, oldValue, newValue) -> canvas.setZoom(newValue.doubleValue()));
        HBox bar = new HBox(12, status, new Separator(), snap, new Label("التكبير"), zoom);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(8, 12, 8, 12));
        HBox.setHgrow(status, Priority.ALWAYS);
        return bar;
    }

    private void addText() {
        int z = nextZ();
        session.addElement(new TextElement(ElementFrame.create("Text", new RectMm(5, 5, 30, 7), z),
                "نص جديد", "Arial", 11, "#111827", false, false, TextAlignment.CENTER));
    }

    private void addBarcode(Symbology symbology) {
        int z = nextZ();
        String value = symbology == Symbology.QR_CODE ? "{{qr_value}}" : "{{barcode}}";
        RectMm bounds = symbology == Symbology.QR_CODE ? new RectMm(5, 5, 22, 22) : new RectMm(5, 12, 50, 17);
        session.addElement(new BarcodeElement(ElementFrame.create(symbology.name(), bounds, z),
                value, symbology, 0.25, 2.5, symbology != Symbology.QR_CODE));
    }

    private void addRectangle() {
        session.addElement(new ShapeElement(ElementFrame.create("Rectangle", new RectMm(4, 4, 40, 20), nextZ()),
                ShapeType.ROUNDED_RECTANGLE, "#64748B", "transparent", 0.3));
    }

    private int nextZ() {
        return session.template().elements().stream().mapToInt(element -> element.frame().zIndex()).max().orElse(-1) + 1;
    }

    private void refreshProperties() {
        refreshingProperties = true;
        try {
            LabelElement element = session.find(session.primarySelection());
            boolean disabled = element == null;
            propertiesPanel.lookupAll(".text-field").forEach(node -> node.setDisable(disabled));
            locked.setDisable(disabled);
            visible.setDisable(disabled);
            if (element == null) {
                selectionTitle.setText("لا يوجد عنصر محدد");
                for (TextField field : List.of(xField, yField, widthField, heightField, contentField)) field.clear();
                return;
            }
            selectionTitle.setText(element.frame().name());
            RectMm b = element.frame().bounds();
            xField.setText(format(b.x())); yField.setText(format(b.y()));
            widthField.setText(format(b.width())); heightField.setText(format(b.height()));
            contentField.setText(switch (element) {
                case TextElement text -> text.textTemplate();
                case BarcodeElement barcode -> barcode.dataTemplate();
                default -> "";
            });
            contentField.setDisable(!(element instanceof TextElement || element instanceof BarcodeElement));
            locked.setSelected(element.frame().locked());
            visible.setSelected(element.frame().visible());
        } finally {
            refreshingProperties = false;
        }
    }

    private void applyProperties() {
        if (refreshingProperties) return;
        LabelElement element = session.find(session.primarySelection());
        if (element == null) return;
        try {
            RectMm bounds = new RectMm(parse(xField), parse(yField), parse(widthField), parse(heightField));
            session.setBounds(element.frame().id(), bounds);
            if (element instanceof TextElement || element instanceof BarcodeElement) {
                session.setContent(element.frame().id(), contentField.getText());
            }
            session.setLockedVisible(element.frame().id(), locked.isSelected(), visible.isSelected());
            status.setText("تم تطبيق الخصائص");
        } catch (RuntimeException exception) {
            status.setText("قيمة غير صحيحة: " + exception.getMessage());
        }
    }

    private void updateHistoryButtons() {
        undoButton.setDisable(!session.canUndo());
        redoButton.setDisable(!session.canRedo());
        undoButton.setText(session.canUndo() ? "تراجع: " + session.undoDescription() : "تراجع");
        redoButton.setText(session.canRedo() ? "إعادة: " + session.redoDescription() : "إعادة");
    }

    private static Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.setOnAction(event -> action.run());
        return button;
    }

    private static TextField numberField() {
        TextField field = new TextField();
        field.setPrefColumnCount(7);
        return field;
    }

    private static void addPropertyRow(GridPane grid, int row, String label, TextField field) {
        grid.add(new Label(label), 0, row);
        grid.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private static double parse(TextField field) { return Double.parseDouble(field.getText().trim()); }
    private static String format(double value) { return String.format(java.util.Locale.ROOT, "%.2f", value); }

    private void loadStylesheet() {
        var resource = LabelDesignerPane.class.getResource("label-designer-pane.css");
        if (resource != null) getStylesheets().add(resource.toExternalForm());
    }

    private static LabelTemplate defaultTemplate() {
        var layout = new SheetLayout(3, 7, new DimensionsMm(63.5, 38.1), 2.5, 0);
        return LabelTemplate.create("New label",
                PageDefinition.a4Sheet(layout, new InsetsMm(15.15, 7.25, 15.15, 7.25)), List.of());
    }
}

