package com.mohamed.barcodelabel.fx.designer;

import com.mohamed.barcodelabel.data.TemplateJsonService;
import com.mohamed.barcodelabel.fx.designer.model.EditorAlignment;
import com.mohamed.barcodelabel.fx.designer.model.TemplateEditSession;
import com.mohamed.barcodelabel.model.LabelTemplate;
import com.mohamed.barcodelabel.model.element.BarcodeElement;
import com.mohamed.barcodelabel.model.element.ElementFrame;
import com.mohamed.barcodelabel.model.element.ImageElement;
import com.mohamed.barcodelabel.model.element.ImageFit;
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
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public final class LabelDesignerPane extends BorderPane {
    private final ReadOnlyObjectWrapper<LabelTemplate> template = new ReadOnlyObjectWrapper<>(this, "template");
    private final VBox propertiesPanel = new VBox(10);
    private final Label selectionTitle = new Label("لا يوجد عنصر محدد");
    private final TextField xField = numberField();
    private final TextField yField = numberField();
    private final TextField widthField = numberField();
    private final TextField heightField = numberField();
    private final TextField rotationField = numberField();
    private final TextField contentField = new TextField();
    private final Button browseImageButton = new Button("استعراض...");
    private final ComboBox<ImageFit> imageFitField = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(ImageFit.values()));
    private final ComboBox<String> fontFamilyField = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(
            "Arial", "Tahoma", "Verdana", "Times New Roman", "Courier New", "Segoe UI"));
    private final TextField fontSizeField = numberField();
    private final ColorPicker colorField = new ColorPicker();
    private final ToggleButton boldToggle = new ToggleButton("B");
    private final ToggleButton italicToggle = new ToggleButton("I");
    private final ToggleGroup textAlignGroup = new ToggleGroup();
    private final ToggleButton alignLeftToggle = new ToggleButton("يسار");
    private final ToggleButton alignCenterToggle = new ToggleButton("توسيط");
    private final ToggleButton alignRightToggle = new ToggleButton("يمين");
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

    private void openFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("افتح قالب");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("قالب JSON", "*.json"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file == null) return;
        try {
            setTemplate(new TemplateJsonService().read(file.toPath()));
            status.setText("تم فتح القالب: " + file.getName());
        } catch (IOException exception) {
            status.setText("فشل فتح الملف: " + exception.getMessage());
        }
    }

    private void saveFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("حفظ القالب");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("قالب JSON", "*.json"));
        chooser.setInitialFileName(getTemplate().name() + ".json");
        File file = chooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file == null) return;
        try {
            save(file.toPath());
        } catch (IOException exception) {
            status.setText("فشل حفظ الملف: " + exception.getMessage());
        }
    }

    private ToolBar createToolbar() {
        Button open = button("فتح...", this::openFile);
        Button saveAs = button("حفظ باسم...", this::saveFile);
        undoButton.setOnAction(event -> session.undo());
        redoButton.setOnAction(event -> session.redo());
        Button copy = button("نسخ", () -> session.copySelected());
        Button cut = button("قص", () -> { session.copySelected(); session.deleteSelected(); });
        Button paste = button("لصق", () -> session.pasteClipboard());
        Button duplicate = button("تكرار", () -> session.duplicateSelected(2));
        Button delete = button("حذف", () -> session.deleteSelected());
        Button left = button("يسار", () -> session.align(EditorAlignment.LEFT));
        Button center = button("توسيط أفقي", () -> session.align(EditorAlignment.HORIZONTAL_CENTER));
        Button right = button("يمين", () -> session.align(EditorAlignment.RIGHT));
        Button top = button("أعلى", () -> session.align(EditorAlignment.TOP));
        Button middle = button("توسيط رأسي", () -> session.align(EditorAlignment.VERTICAL_CENTER));
        Button bottom = button("أسفل", () -> session.align(EditorAlignment.BOTTOM));
        Button toFront = button("إحضار للمقدمة", () -> session.bringToFront(session.primarySelection()));
        Button forward = button("طبقة للأمام", () -> session.bringForward(session.primarySelection()));
        Button backward = button("طبقة للخلف", () -> session.sendBackward(session.primarySelection()));
        Button toBack = button("إرسال للخلفية", () -> session.sendToBack(session.primarySelection()));
        return new ToolBar(open, saveAs, new Separator(), undoButton, redoButton,
                new Separator(), cut, copy, paste, duplicate, delete,
                new Separator(), left, center, right, top, middle, bottom,
                new Separator(), toFront, forward, backward, toBack);
    }

    private VBox createToolbox() {
        Label title = new Label("العناصر");
        title.getStyleClass().add("panel-title");
        Button text = button("+ نص", this::addText);
        Button barcode = button("+ باركود", () -> addBarcode(Symbology.CODE_128));
        Button qr = button("+ QR Code", () -> addBarcode(Symbology.QR_CODE));
        Button rectangle = button("+ مستطيل", this::addRectangle);
        Button image = button("+ صورة", this::addImage);
        for (Button button : List.of(text, barcode, qr, rectangle, image)) button.setMaxWidth(Double.MAX_VALUE);
        VBox toolbox = new VBox(8, title, text, barcode, qr, rectangle, image, new Separator(),
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
        addPropertyRow(grid, 4, "الدوران (°)", rotationField);
        HBox contentRow = new HBox(6, contentField, browseImageButton);
        HBox.setHgrow(contentField, Priority.ALWAYS);
        browseImageButton.setOnAction(event -> browseImage());
        grid.add(new Label("النص/البيانات/المسار"), 0, 5);
        grid.add(contentRow, 1, 5);
        GridPane.setHgrow(contentRow, Priority.ALWAYS);
        addPropertyRow(grid, 6, "نمط الصورة", imageFitField);

        Label textTitle = new Label("تنسيق النص");
        textTitle.getStyleClass().add("panel-title");
        GridPane textGrid = new GridPane();
        textGrid.setHgap(8);
        textGrid.setVgap(8);
        fontFamilyField.setEditable(true);
        addPropertyRow(textGrid, 0, "الخط", fontFamilyField);
        addPropertyRow(textGrid, 1, "الحجم", fontSizeField);
        addPropertyRow(textGrid, 2, "اللون", colorField);
        alignLeftToggle.setToggleGroup(textAlignGroup);
        alignCenterToggle.setToggleGroup(textAlignGroup);
        alignRightToggle.setToggleGroup(textAlignGroup);
        HBox style = new HBox(6, boldToggle, italicToggle, new Separator(),
                alignLeftToggle, alignCenterToggle, alignRightToggle);

        HBox state = new HBox(12, visible, locked);
        Button apply = button("تطبيق الخصائص", this::applyProperties);
        apply.getStyleClass().add("primary-button");
        apply.setMaxWidth(Double.MAX_VALUE);
        propertiesPanel.getChildren().setAll(title, selectionTitle, grid,
                textTitle, textGrid, style, state, apply);
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

    private void addImage() {
        File file = chooseImageFile();
        if (file == null) return;
        session.addElement(new ImageElement(ElementFrame.create("Image", new RectMm(5, 5, 30, 20), nextZ()),
                file.toURI().toString(), ImageFit.CONTAIN));
    }

    private void browseImage() {
        File file = chooseImageFile();
        if (file != null) contentField.setText(file.toURI().toString());
    }

    private File chooseImageFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("اختر صورة");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("صور", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        return chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
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
                for (TextField field : List.of(xField, yField, widthField, heightField, rotationField, contentField, fontSizeField)) field.clear();
                setTextControlsDisabled(true);
                browseImageButton.setDisable(true);
                imageFitField.setDisable(true);
                return;
            }
            selectionTitle.setText(element.frame().name());
            RectMm b = element.frame().bounds();
            xField.setText(format(b.x())); yField.setText(format(b.y()));
            widthField.setText(format(b.width())); heightField.setText(format(b.height()));
            rotationField.setText(format(element.frame().rotationDegrees()));
            contentField.setText(switch (element) {
                case TextElement text -> text.textTemplate();
                case BarcodeElement barcode -> barcode.dataTemplate();
                case ImageElement image -> image.source();
                default -> "";
            });
            boolean isImage = element instanceof ImageElement;
            contentField.setDisable(!(element instanceof TextElement || element instanceof BarcodeElement || isImage));
            browseImageButton.setDisable(!isImage);
            imageFitField.setDisable(!isImage);
            if (element instanceof ImageElement image) imageFitField.setValue(image.fit());
            locked.setSelected(element.frame().locked());
            visible.setSelected(element.frame().visible());

            boolean isText = element instanceof TextElement;
            setTextControlsDisabled(!isText);
            if (element instanceof TextElement text) {
                fontFamilyField.setValue(text.fontFamily());
                fontSizeField.setText(format(text.fontSizePoints()));
                colorField.setValue(parseColor(text.color()));
                boldToggle.setSelected(text.bold());
                italicToggle.setSelected(text.italic());
                textAlignGroup.selectToggle(switch (text.alignment()) {
                    case LEFT -> alignLeftToggle;
                    case CENTER -> alignCenterToggle;
                    case RIGHT -> alignRightToggle;
                });
            }
        } finally {
            refreshingProperties = false;
        }
    }

    private void setTextControlsDisabled(boolean disabled) {
        fontFamilyField.setDisable(disabled);
        fontSizeField.setDisable(disabled);
        colorField.setDisable(disabled);
        boldToggle.setDisable(disabled);
        italicToggle.setDisable(disabled);
        alignLeftToggle.setDisable(disabled);
        alignCenterToggle.setDisable(disabled);
        alignRightToggle.setDisable(disabled);
    }

    private void applyProperties() {
        if (refreshingProperties) return;
        LabelElement element = session.find(session.primarySelection());
        if (element == null) return;
        try {
            // Read every field before changing anything: each change refreshes this panel from the element,
            // which would overwrite the fields that have not been applied yet.
            UUID id = element.frame().id();
            RectMm bounds = new RectMm(parse(xField), parse(yField), parse(widthField), parse(heightField));
            double rotation = parse(rotationField);
            String content = contentField.getText();
            ImageFit fit = imageFitField.getValue() != null ? imageFitField.getValue() : ImageFit.CONTAIN;
            boolean isText = element instanceof TextElement;
            String fontFamily = fontFamilyField.getValue();
            double fontSize = isText ? parse(fontSizeField) : 0;
            String color = toHex(colorField.getValue());
            boolean bold = boldToggle.isSelected();
            boolean italic = italicToggle.isSelected();
            TextAlignment alignment = alignCenterToggle.isSelected() ? TextAlignment.CENTER
                    : alignRightToggle.isSelected() ? TextAlignment.RIGHT
                    : TextAlignment.LEFT;
            boolean isLocked = locked.isSelected();
            boolean isVisible = visible.isSelected();

            session.setBounds(id, bounds);
            session.setRotation(id, rotation);
            if (isText || element instanceof BarcodeElement) {
                session.setContent(id, content);
            }
            if (element instanceof ImageElement) {
                session.setImage(id, content, fit);
            }
            if (isText) {
                session.setTextStyle(id, fontFamily, fontSize, color, bold, italic, alignment);
            }
            session.setLockedVisible(id, isLocked, isVisible);
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

    private static void addPropertyRow(GridPane grid, int row, String label, javafx.scene.control.Control field) {
        grid.add(new Label(label), 0, row);
        grid.add(field, 1, row);
        field.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private static double parse(TextField field) { return Double.parseDouble(field.getText().trim()); }
    private static String format(double value) { return String.format(java.util.Locale.ROOT, "%.2f", value); }

    private static Color parseColor(String hex) {
        try {
            return Color.web(hex);
        } catch (IllegalArgumentException exception) {
            return Color.BLACK;
        }
    }

    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }

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

