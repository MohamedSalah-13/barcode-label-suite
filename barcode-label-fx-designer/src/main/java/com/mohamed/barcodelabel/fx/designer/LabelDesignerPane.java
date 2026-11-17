package com.mohamed.barcodelabel.fx.designer;

import com.mohamed.barcodelabel.data.TemplateJsonService;
import com.mohamed.barcodelabel.fx.designer.model.EditorAlignment;
import com.mohamed.barcodelabel.fx.designer.model.TemplateEditSession;
import com.mohamed.barcodelabel.fx.i18n.Counted;
import com.mohamed.barcodelabel.fx.i18n.UiLanguage;
import com.mohamed.barcodelabel.fx.i18n.UiTexts;
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
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public final class LabelDesignerPane extends BorderPane {
    private final UiTexts texts;
    private final ReadOnlyObjectWrapper<LabelTemplate> template = new ReadOnlyObjectWrapper<>(this, "template");
    private final VBox propertiesPanel = new VBox(10);
    private final Label selectionTitle = new Label();
    private final TextField xField = numberField();
    private final TextField yField = numberField();
    private final TextField widthField = numberField();
    private final TextField heightField = numberField();
    private final TextField rotationField = numberField();
    private final Label contentLabel = caption("");
    private final TextField contentField = new TextField();
    private final Button browseImageButton = new Button("", new FontIcon(Feather.FOLDER));
    private final ComboBox<ImageFit> imageFitField = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(ImageFit.values()));
    private final ComboBox<String> fontFamilyField = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(
            "Arial", "Tahoma", "Verdana", "Times New Roman", "Courier New", "Segoe UI"));
    private final TextField fontSizeField = numberField();
    private final ColorPicker colorField = new ColorPicker();
    private final ToggleButton boldToggle = new ToggleButton("B");
    private final ToggleButton italicToggle = new ToggleButton("I");
    private final ToggleGroup textAlignGroup = new ToggleGroup();
    private final ToggleButton alignLeftToggle = new ToggleButton();
    private final ToggleButton alignCenterToggle = new ToggleButton();
    private final ToggleButton alignRightToggle = new ToggleButton();
    private final CheckBox locked = new CheckBox();
    private final CheckBox visible = new CheckBox();
    private final Button undoButton = new Button();
    private final Button redoButton = new Button();
    private final Label status = new Label();
    // The elements and properties panels sit beside the label in a split pane: their dividers resize them, and the
    // two check boxes in the status bar hide or show them. Each keeps its width while hidden and when the window
    // is resized; the label's workspace takes the rest.
    private final SplitPane body = new SplitPane();
    private final CheckBox showElements = new CheckBox();
    private final CheckBox showProperties = new CheckBox();
    private VBox toolbox;
    private ScrollPane propertiesScroll;
    private double toolboxWidth = 190;
    private double propertiesWidth = 300;
    private boolean placingPanels;
    // One workspace for the pane's lifetime: a new template only replaces the canvas inside it, so the split pane
    // and its dividers are left alone.
    private final ScrollPane workspace = new ScrollPane();
    private TemplateEditSession session;
    private DesignerCanvas canvas;
    private boolean refreshingProperties;

    /** An Arabic designer for a new, empty A4 label. */
    public LabelDesignerPane() {
        this(UiLanguage.ARABIC);
    }

    /** A designer for a new, empty A4 label, in this language. */
    public LabelDesignerPane(UiLanguage language) {
        this(defaultTemplate(UiTexts.load(LabelDesignerPane.class, "messages", language)), language);
    }

    /** An Arabic designer. */
    public LabelDesignerPane(LabelTemplate initialTemplate) {
        this(initialTemplate, UiLanguage.ARABIC);
    }

    /**
     * A designer in this language. Arabic lays the pane out right to left and English left to right; the label
     * surface is left to right in both, as the label prints.
     */
    public LabelDesignerPane(LabelTemplate initialTemplate, UiLanguage language) {
        texts = UiTexts.load(LabelDesignerPane.class, "messages", language);
        setNodeOrientation(language.orientation());
        selectionTitle.setText(t("designer.noSelection"));
        browseImageButton.setTooltip(new Tooltip(t("designer.browse")));
        alignLeftToggle.setText(t("designer.textLeft"));
        alignCenterToggle.setText(t("designer.textCenter"));
        alignRightToggle.setText(t("designer.textRight"));
        locked.setText(t("designer.locked"));
        visible.setText(t("designer.visible"));
        status.setText(t("designer.ready"));
        imageFitField.setConverter(new StringConverter<>() {
            @Override public String toString(ImageFit fit) { return fit == null ? "" : t("fit." + fit.name()); }
            @Override public ImageFit fromString(String text) { return null; }
        });
        getStyleClass().add("label-designer-pane");
        setTop(createToolbar());
        createSidePanels();
        setCenter(body);
        setBottom(createStatusBar());
        setTemplate(initialTemplate);
        loadStylesheet();
    }

    public UiLanguage language() { return texts.language(); }
    public LabelTemplate getTemplate() { return template.get(); }
    public ReadOnlyObjectProperty<LabelTemplate> templateProperty() { return template.getReadOnlyProperty(); }

    public void setTemplate(LabelTemplate value) {
        session = new TemplateEditSession(value);
        canvas = new DesignerCanvas(session, texts);
        // The canvas is left to right; the holder around it inherits this pane's orientation, so in Arabic the
        // workspace places the label on its right, like the rest of the right-to-left UI. A layout pane, not a
        // Group: a Group's size would include the canvas's drop shadow and push the label off the padding.
        workspace.setContent(new StackPane(canvas));
        // A double click on an element jumps to the field that edits its text or barcode data. A filter, because the
        // canvas consumes the element's mouse events; it runs after the press has already selected the element.
        canvas.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getClickCount() == 2 && !contentField.isDisabled()) {
                showProperties.setSelected(true);
                contentField.requestFocus();
                contentField.selectAll();
            }
        });
        session.addListener(() -> {
            template.set(session.template());
            updateHistoryButtons();
            refreshProperties();
            int selected = session.selection().size();
            status.setText(selected == 0 ? t("designer.noSelection")
                    : t("designer.selected", texts.count(Counted.ELEMENTS, selected)));
        });
        template.set(value);
        updateHistoryButtons();
        refreshProperties();
    }

    /** The panel that edits the selected element, inside its scroll pane. */
    VBox propertiesPanel() { return propertiesPanel; }

    /** The scroll pane around the label being designed. */
    ScrollPane workspace() { return workspace; }

    /** The split pane's items, in order: the elements panel, the workspace and the properties panel, when shown. */
    List<javafx.scene.Node> bodyItems() { return List.copyOf(body.getItems()); }

    private void createSidePanels() {
        workspace.getStyleClass().add("designer-workspace");
        workspace.setPannable(true);
        workspace.setMinWidth(160);
        toolbox = createToolbox();
        toolbox.setMinWidth(120);
        toolbox.setMaxWidth(500);
        propertiesScroll = new ScrollPane(createPropertiesPanel());
        propertiesScroll.setFitToWidth(true);
        propertiesScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        propertiesScroll.getStyleClass().add("designer-properties-scroll");
        propertiesScroll.setMinWidth(200);
        propertiesScroll.setMaxWidth(700);
        SplitPane.setResizableWithParent(toolbox, false);
        SplitPane.setResizableWithParent(propertiesScroll, false);
        body.getStyleClass().add("designer-body");
        // A dragged divider is remembered, so hiding and showing a panel brings it back at the width it had.
        toolbox.widthProperty().addListener((observable, oldValue, width) -> {
            if (!placingPanels && body.getItems().contains(toolbox) && width.doubleValue() > 0) toolboxWidth = width.doubleValue();
        });
        propertiesScroll.widthProperty().addListener((observable, oldValue, width) -> {
            if (!placingPanels && body.getItems().contains(propertiesScroll) && width.doubleValue() > 0) propertiesWidth = width.doubleValue();
        });
        body.widthProperty().addListener((observable, oldValue, width) -> {
            if (oldValue.doubleValue() <= 0 && width.doubleValue() > 0) placeDividers();
        });
        showElements.setText(t("designer.showElements"));
        showProperties.setText(t("designer.showProperties"));
        showElements.setSelected(true);
        showProperties.setSelected(true);
        showElements.selectedProperty().addListener((observable, oldValue, shown) -> layoutBody());
        showProperties.selectedProperty().addListener((observable, oldValue, shown) -> layoutBody());
        layoutBody();
    }

    /** Puts the shown panels and the workspace into the split pane, then the dividers at the panels' widths. */
    private void layoutBody() {
        placingPanels = true;
        try {
            List<javafx.scene.Node> items = new java.util.ArrayList<>();
            if (showElements.isSelected()) items.add(toolbox);
            items.add(workspace);
            if (showProperties.isSelected()) items.add(propertiesScroll);
            body.getItems().setAll(items);
            placeDividers();
        } finally {
            placingPanels = false;
        }
    }

    /**
     * Sets the dividers from the panels' widths in pixels. The positions are fractions of the split pane's width,
     * so this waits for its first layout; in Arabic the pane is mirrored and the fractions count from the right.
     */
    private void placeDividers() {
        double width = body.getWidth();
        if (width <= 0) return;
        boolean elements = body.getItems().contains(toolbox);
        boolean properties = body.getItems().contains(propertiesScroll);
        double start = Math.min(toolboxWidth, width / 3) / width;
        double end = 1 - Math.min(propertiesWidth, width / 2) / width;
        if (elements && properties) body.setDividerPositions(start, Math.max(start, end));
        else if (elements) body.setDividerPositions(start);
        else if (properties) body.setDividerPositions(end);
    }

    public void save(Path path) throws IOException {
        new TemplateJsonService().write(path, getTemplate());
        status.setText(t("designer.saved", path.getFileName()));
    }

    private void openFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(t("designer.openTitle"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(t("designer.templateFilter"), "*.json"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file == null) return;
        try {
            setTemplate(new TemplateJsonService().read(file.toPath()));
            status.setText(t("designer.opened", file.getName()));
        } catch (IOException exception) {
            status.setText(t("designer.openFailed", exception.getMessage()));
        }
    }

    private void saveFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(t("designer.saveTitle"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(t("designer.templateFilter"), "*.json"));
        chooser.setInitialFileName(getTemplate().name() + ".json");
        File file = chooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file == null) return;
        try {
            save(file.toPath());
        } catch (IOException exception) {
            status.setText(t("designer.saveFailed", exception.getMessage()));
        }
    }

    private ToolBar createToolbar() {
        Button open = button(t("designer.open"), this::openFile);
        Button saveAs = button(t("designer.saveAs"), this::saveFile);
        undoButton.setOnAction(event -> session.undo());
        redoButton.setOnAction(event -> session.redo());
        Button copy = button(t("designer.copy"), () -> session.copySelected());
        Button cut = button(t("designer.cut"), () -> { session.copySelected(); session.deleteSelected(); });
        Button paste = button(t("designer.paste"), () -> session.pasteClipboard());
        Button duplicate = button(t("designer.duplicate"), () -> session.duplicateSelected(2));
        Button delete = button(t("designer.delete"), () -> session.deleteSelected());
        Button left = button(t("designer.alignLeft"), () -> session.align(EditorAlignment.LEFT));
        Button center = button(t("designer.alignHorizontalCenter"), () -> session.align(EditorAlignment.HORIZONTAL_CENTER));
        Button right = button(t("designer.alignRight"), () -> session.align(EditorAlignment.RIGHT));
        Button top = button(t("designer.alignTop"), () -> session.align(EditorAlignment.TOP));
        Button middle = button(t("designer.alignVerticalCenter"), () -> session.align(EditorAlignment.VERTICAL_CENTER));
        Button bottom = button(t("designer.alignBottom"), () -> session.align(EditorAlignment.BOTTOM));
        Button toFront = button(t("designer.bringToFront"), () -> session.bringToFront(session.primarySelection()));
        Button forward = button(t("designer.bringForward"), () -> session.bringForward(session.primarySelection()));
        Button backward = button(t("designer.sendBackward"), () -> session.sendBackward(session.primarySelection()));
        Button toBack = button(t("designer.sendToBack"), () -> session.sendToBack(session.primarySelection()));
        return new ToolBar(open, saveAs, new Separator(), undoButton, redoButton,
                new Separator(), cut, copy, paste, duplicate, delete,
                new Separator(), left, center, right, top, middle, bottom,
                new Separator(), toFront, forward, backward, toBack);
    }

    private VBox createToolbox() {
        Label title = new Label(t("designer.elements"));
        title.getStyleClass().add("panel-title");
        Button text = button(t("designer.addText"), this::addText);
        Button barcode = button(t("designer.addBarcode"), () -> addBarcode(Symbology.CODE_128));
        Button qr = button(t("designer.addQr"), () -> addBarcode(Symbology.QR_CODE));
        Button rectangle = button(t("designer.addRectangle"), this::addRectangle);
        Button image = button(t("designer.addImage"), this::addImage);
        for (Button button : List.of(text, barcode, qr, rectangle, image)) button.setMaxWidth(Double.MAX_VALUE);
        VBox toolbox = new VBox(8, title, text, barcode, qr, rectangle, image, new Separator(),
                hint(t("designer.hintMultiSelect")),
                hint(t("designer.hintArrows")),
                hint(t("designer.hintShiftArrows")));
        toolbox.getStyleClass().add("designer-toolbox");
        toolbox.setPrefWidth(190);
        return toolbox;
    }

    private VBox createPropertiesPanel() {
        Label title = new Label(t("designer.properties"));
        title.getStyleClass().add("panel-title");
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        addPropertyRow(grid, 0, t("designer.x"), xField);
        addPropertyRow(grid, 1, t("designer.y"), yField);
        addPropertyRow(grid, 2, t("designer.width"), widthField);
        addPropertyRow(grid, 3, t("designer.height"), heightField);
        addPropertyRow(grid, 4, t("designer.rotation"), rotationField);
        HBox contentRow = new HBox(6, contentField, browseImageButton);
        HBox.setHgrow(contentField, Priority.ALWAYS);
        browseImageButton.setOnAction(event -> browseImage());
        browseImageButton.setMinWidth(Region.USE_PREF_SIZE);
        contentField.setTooltip(new Tooltip(t("designer.contentHint")));
        grid.add(contentLabel, 0, 5);
        grid.add(contentRow, 1, 5);
        GridPane.setHgrow(contentRow, Priority.ALWAYS);
        addPropertyRow(grid, 6, t("designer.imageFit"), imageFitField);

        Label textTitle = new Label(t("designer.textFormat"));
        textTitle.getStyleClass().add("panel-title");
        GridPane textGrid = new GridPane();
        textGrid.setHgap(8);
        textGrid.setVgap(8);
        fontFamilyField.setEditable(true);
        addPropertyRow(textGrid, 0, t("designer.font"), fontFamilyField);
        addPropertyRow(textGrid, 1, t("designer.fontSize"), fontSizeField);
        addPropertyRow(textGrid, 2, t("designer.color"), colorField);
        alignLeftToggle.setToggleGroup(textAlignGroup);
        alignCenterToggle.setToggleGroup(textAlignGroup);
        alignRightToggle.setToggleGroup(textAlignGroup);
        // Two rows, so the alignment buttons keep their full names in a narrow panel.
        HBox style = new HBox(6, boldToggle, italicToggle);
        HBox alignment = new HBox(6, alignLeftToggle, alignCenterToggle, alignRightToggle);

        HBox state = new HBox(12, visible, locked);
        Button apply = button(t("designer.apply"), this::applyProperties);
        apply.getStyleClass().add("primary-button");
        apply.setMaxWidth(Double.MAX_VALUE);
        propertiesPanel.getChildren().setAll(title, selectionTitle, grid,
                textTitle, textGrid, style, alignment, state, apply);
        propertiesPanel.getStyleClass().add("designer-properties");
        propertiesPanel.setPrefWidth(300);
        // Enter in any field applies the panel, like the button: without it a typed value silently waits for a click.
        for (TextField field : List.of(xField, yField, widthField, heightField, rotationField, contentField, fontSizeField)) {
            field.setOnAction(event -> applyProperties());
        }
        return propertiesPanel;
    }

    private HBox createStatusBar() {
        CheckBox snap = new CheckBox(t("designer.snap"));
        snap.setSelected(true);
        snap.selectedProperty().addListener((observable, oldValue, newValue) -> canvas.setSnapEnabled(newValue));
        Slider zoom = new Slider(0.5, 2.5, 1);
        zoom.setPrefWidth(180);
        zoom.valueProperty().addListener((observable, oldValue, newValue) -> canvas.setZoom(newValue.doubleValue()));
        HBox bar = new HBox(12, status, new Separator(), showElements, showProperties, new Separator(),
                snap, new Label(t("designer.zoom")), zoom);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 12, 8, 12));
        HBox.setHgrow(status, Priority.ALWAYS);
        return bar;
    }

    private void addText() {
        int z = nextZ();
        session.addElement(new TextElement(ElementFrame.create(t("element.text"), new RectMm(5, 5, 30, 7), z),
                t("element.newText"), "Arial", 11, "#111827", false, false, TextAlignment.CENTER));
    }

    private void addBarcode(Symbology symbology) {
        int z = nextZ();
        String value = symbology == Symbology.QR_CODE ? "{{qr_value}}" : "{{barcode}}";
        RectMm bounds = symbology == Symbology.QR_CODE ? new RectMm(5, 5, 22, 22) : new RectMm(5, 12, 50, 17);
        session.addElement(new BarcodeElement(ElementFrame.create(t(symbology == Symbology.QR_CODE ? "element.qr" : "element.barcode"), bounds, z),
                value, symbology, 0.25, 2.5, symbology != Symbology.QR_CODE));
    }

    private void addRectangle() {
        session.addElement(new ShapeElement(ElementFrame.create(t("element.rectangle"), new RectMm(4, 4, 40, 20), nextZ()),
                ShapeType.ROUNDED_RECTANGLE, "#64748B", "transparent", 0.3));
    }

    private void addImage() {
        File file = chooseImageFile();
        if (file == null) return;
        session.addElement(new ImageElement(ElementFrame.create(t("element.image"), new RectMm(5, 5, 30, 20), nextZ()),
                file.toURI().toString(), ImageFit.CONTAIN));
    }

    private void browseImage() {
        File file = chooseImageFile();
        if (file != null) contentField.setText(file.toURI().toString());
    }

    private File chooseImageFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(t("designer.imageTitle"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(t("designer.imageFilter"), "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
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
                selectionTitle.setText(t("designer.noSelection"));
                contentLabel.setText(contentCaption(null));
                for (TextField field : List.of(xField, yField, widthField, heightField, rotationField, contentField, fontSizeField)) field.clear();
                setTextControlsDisabled(true);
                browseImageButton.setDisable(true);
                imageFitField.setDisable(true);
                return;
            }
            selectionTitle.setText(element.frame().name());
            contentLabel.setText(contentCaption(element));
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
                colorField.setValue(ElementViewFactory.parseColor(text.color()));
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

    /** What the content field holds for {@code element}: its text, its barcode data or its image file. */
    String contentCaption(LabelElement element) {
        return t(switch (element) {
            case TextElement text -> "designer.content.text";
            case BarcodeElement barcode -> "designer.content.barcode";
            case ImageElement image -> "designer.content.image";
            case null, default -> "designer.content";
        });
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
            status.setText(t("designer.applied"));
        } catch (NumberFormatException exception) {
            status.setText(t("designer.notANumber"));
        } catch (RuntimeException exception) {
            status.setText(t("designer.invalidValue", exception.getMessage()));
        }
    }

    private void updateHistoryButtons() {
        undoButton.setDisable(!session.canUndo());
        redoButton.setDisable(!session.canRedo());
        undoButton.setText(session.canUndo() ? t("designer.undoAction", t("edit." + session.undoAction().name())) : t("designer.undo"));
        redoButton.setText(session.canRedo() ? t("designer.redoAction", t("edit." + session.redoAction().name())) : t("designer.redo"));
    }

    private String t(String key, Object... arguments) { return texts.get(key, arguments); }

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

    /** A property name that is never cut short: the field beside it gives up the width instead. */
    private static Label caption(String text) {
        Label label = new Label(text);
        label.setMinWidth(Region.USE_PREF_SIZE);
        return label;
    }

    private static Label hint(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        return label;
    }

    private static void addPropertyRow(GridPane grid, int row, String label, javafx.scene.control.Control field) {
        grid.add(caption(label), 0, row);
        grid.add(field, 1, row);
        field.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private static double parse(TextField field) { return Double.parseDouble(field.getText().trim()); }
    private static String format(double value) { return String.format(java.util.Locale.ROOT, "%.2f", value); }

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

    private static LabelTemplate defaultTemplate(UiTexts texts) {
        var layout = new SheetLayout(3, 7, new DimensionsMm(63.5, 38.1), 2.5, 0);
        return LabelTemplate.create(texts.get("template.new"),
                PageDefinition.a4Sheet(layout, new InsetsMm(15.15, 7.25, 15.15, 7.25)), List.of());
    }
}

