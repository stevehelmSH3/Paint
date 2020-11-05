package paint;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.imageio.ImageIO;

public class Paint extends Application {

    File file;
    ImageView pic = new ImageView();
    Boolean saveMe = true;
    Boolean printNotes = false;
    Boolean active = false;
    Boolean saveFlag = false;
    private double x0;
    private double y0;
    private int numSides;
    private double polyStartX;
    private double polyStartY;
    public static WritableImage tmpSnap;
    public static WritableImage selImg;
    public Label active_tool = new Label("No Tool is active.");
    public Label autosaveLabel = new Label("Autosave off");

    @Override
    public void start(Stage primaryStage) {
        Stack<Shape> undoHistory = new Stack();
        Stack<Shape> redoHistory = new Stack();

        /* ----------Buttons---------- */
        ToggleButton drowbtn = new ToggleButton("Free Draw");
        ToggleButton rubberbtn = new ToggleButton("Eraser");
        ToggleButton linebtn = new ToggleButton("Line");
        ToggleButton rectbtn = new ToggleButton("Rectangle");
        ToggleButton sqrbtn = new ToggleButton("Square");
        ToggleButton tribtn = new ToggleButton("Triangle");
        ToggleButton circlebtn = new ToggleButton("Circle");
        ToggleButton elpslebtn = new ToggleButton("Ellipse");
        ToggleButton polybtn = new ToggleButton("Polygon");
        ToggleButton textbtn = new ToggleButton("Text");
        ToggleButton slcbtn = new ToggleButton("Select");
        ToggleButton movebtn = new ToggleButton("Move");
        ToggleButton copybtn = new ToggleButton("Copy");
        Button autosave = new Button("Autosave");


        /* ------------ TOOL TIPS ----------- */
        drowbtn.setTooltip(new Tooltip("This button allows you to free draw"));
        rectbtn.setTooltip(new Tooltip("Create rectangle."));
        rubberbtn.setTooltip(new Tooltip("Erase with this button."));
        linebtn.setTooltip(new Tooltip("Make a straight line."));
        tribtn.setTooltip(new Tooltip("Place a triangle."));
        textbtn.setTooltip(new Tooltip("Type in anything you want, and click on the picture to add."));
        circlebtn.setTooltip(new Tooltip("Creates a circle."));
        elpslebtn.setTooltip(new Tooltip("Creates a squashed circle."));
        slcbtn.setTooltip(new Tooltip("Select part of the picture."));

        ToggleButton[] toolsArr = {drowbtn, rubberbtn, linebtn, rectbtn, sqrbtn, tribtn,
            circlebtn, elpslebtn, polybtn, textbtn, slcbtn, movebtn, copybtn};

        ToggleGroup tools = new ToggleGroup();

        for (ToggleButton tool : toolsArr) {
            tool.setMinWidth(90);
            tool.setToggleGroup(tools);
            tool.setCursor(Cursor.HAND);
        }

        ColorPicker cpLine = new ColorPicker(Color.BLACK);
        ColorPicker cpFill = new ColorPicker(Color.TRANSPARENT);

        TextArea text = new TextArea();
        text.setPrefRowCount(1);

        Spinner<Integer> polyint = new Spinner<>(1, 10, 1);

        Slider slider = new Slider(1, 50, 3);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);

        Label line_color = new Label("Line Color");
        Label fill_color = new Label("Fill Color");
        Label line_width = new Label("3.0");

        MenuItem undo = new MenuItem("Undo");
        MenuItem redo = new MenuItem("Redo");

        Menu menu = new Menu("File");
        Menu help = new Menu("Help");
        Menu edit = new Menu("Edit");
        MenuItem save = new MenuItem("Save");
        MenuItem open = new MenuItem("Open");
        menu.getItems().add(open);
        menu.getItems().add(save);

        VBox btns = new VBox(2);
        btns.getChildren().addAll(drowbtn, rubberbtn, linebtn, rectbtn, sqrbtn, tribtn, circlebtn, elpslebtn, polybtn,
                polyint, slcbtn, movebtn, copybtn, textbtn, text, autosave, line_color, cpLine, fill_color,
                cpFill, line_width, slider, active_tool, autosaveLabel);
        btns.setPadding(new Insets(5));
        btns.setStyle("-fx-background-color: #999");
        btns.setPrefWidth(100);

        /* ----------Tab Pane ------------*/
        // create a tabpane
        TabPane tp = new TabPane();

        for (int r = 0; r < 7; r++) {

            // create Tab
            Tab tab = new Tab("Image_" + (int) (r + 1));

            // add tab
            tp.getTabs().add(tab);
        }

        /* ----------Draw Canvas---------- */
        Canvas canvas = new Canvas(1500, 1000);
        GraphicsContext gc;
        gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(1);

        Line line = new Line();
        Rectangle rect = new Rectangle();
        Rectangle selRect = new Rectangle();
        Rectangle square = new Rectangle();
        Polygon tri = new Polygon();
        Circle circ = new Circle();
        Ellipse elps = new Ellipse();

        canvas.setOnMouseDragged(e -> {

            if (drowbtn.isSelected()) {
                gc.lineTo(e.getX(), e.getY());
                gc.stroke();
            } else if (rubberbtn.isSelected()) {
                double lineWidth = gc.getLineWidth();
                gc.setStroke(Color.WHITE);
                //line to mouse
                gc.lineTo(e.getX(), e.getY());
                gc.stroke();
            } else if (linebtn.isSelected()) {
                //Line is selected 
                gc.drawImage(tmpSnap, 0, 0);
                line.setEndX(e.getX());
                line.setEndY(e.getY());
                gc.strokeLine(line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY());

            } else if (rectbtn.isSelected()) {
                //Rectangle is selected
                gc.drawImage(tmpSnap, 0, 0);

                rect.setWidth(Math.abs((e.getX() - rect.getX())));
                rect.setHeight(Math.abs((e.getY() - rect.getY())));

                if (rect.getX() > e.getX()) {
                    rect.setX(e.getX());

                }
                if (rect.getY() > e.getY()) {
                    rect.setY(e.getY());
                }
                gc.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                gc.strokeRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());

            } else if (sqrbtn.isSelected()) {
                //Rectangle is selected

                gc.drawImage(tmpSnap, 0, 0);

                square.setWidth(Math.abs((e.getX() - square.getX())));
                square.setHeight(Math.abs((e.getY() - square.getY())));
                if (square.getX() > e.getX()) {
                    square.setX(e.getX());

                }
                if (square.getY() > e.getY()) {
                    square.setY(e.getY());
                }
                gc.fillRect(square.getX(), square.getY(), square.getWidth(), square.getHeight());
                gc.strokeRect(square.getX(), square.getY(), square.getWidth(), square.getHeight());

            } else if (tribtn.isSelected()) {

                gc.drawImage(tmpSnap, 0, 0);
                double point3X = e.getX();
                double point3Y = e.getY();
                double point1X = (x0 + point3X) / 2;
                double point1Y = y0;
                double point2Y = point3Y;
                double point2X = x0;

                double[] xpoints = {point1X, point2X, point3X};
                double[] ypoints = {point1Y, point2Y, point3Y};

                gc.fillPolygon(xpoints, ypoints, 3);
                gc.strokePolygon(xpoints, ypoints, 3);

            } else if (circlebtn.isSelected()) {
                //Circle is selected 
                gc.drawImage(tmpSnap, 0, 0);
                circ.setRadius((Math.abs(e.getX() - circ.getCenterX()) + Math.abs(e.getY() - circ.getCenterY())) / 2);

                if (circ.getCenterX() > e.getX()) {
                    circ.setCenterX(e.getX());
                }
                if (circ.getCenterY() > e.getY()) {
                    circ.setCenterY(e.getY());
                }
                gc.fillOval(circ.getCenterX(), circ.getCenterY(), circ.getRadius(), circ.getRadius());
                gc.strokeOval(circ.getCenterX(), circ.getCenterY(), circ.getRadius(), circ.getRadius());

            } else if (elpslebtn.isSelected()) {
                //Ellipse is selected 
                gc.drawImage(tmpSnap, 0, 0);
                elps.setRadiusX(Math.abs(e.getX() - elps.getCenterX()));
                elps.setRadiusY(Math.abs(e.getY() - elps.getCenterY()));

                if (elps.getCenterX() > e.getX()) {
                    elps.setCenterX(e.getX());
                }
                if (elps.getCenterY() > e.getY()) {
                    elps.setCenterY(e.getY());
                }

                gc.fillOval(elps.getCenterX(), elps.getCenterY(), elps.getRadiusX(), elps.getRadiusY());
                gc.strokeOval(elps.getCenterX(), elps.getCenterY(), elps.getRadiusX(), elps.getRadiusY());

            } else if (polybtn.isSelected()) {
                //take snapshot for shape preview
                gc.drawImage(tmpSnap, 0, 0);
                numSides = (int) polyint.getValue();
                double radius = ((Math.abs(e.getX() - polyStartX) + Math.abs(e.getY() - polyStartY)) / 2);
                //checks if it is dragged the other direction
                if (polyStartX > e.getX()) {
                    polyStartX = e.getX();
                }
                if (polyStartY > e.getY()) {
                    polyStartY = e.getY();
                }
                //new array for sides
                double[] xSides = new double[numSides];
                double[] ySides = new double[numSides];
                //create sides
                for (int i = 0; i < numSides; i++) {
                    xSides[i] = radius * Math.cos(2 * i * Math.PI / numSides) + polyStartX;
                    ySides[i] = radius * Math.sin(2 * i * Math.PI / numSides) + polyStartY;
                }

                gc.strokePolygon(xSides, ySides, numSides);
                gc.fillPolygon(xSides, ySides, numSides);
            } //Move
            else if (slcbtn.isSelected()) {
                gc.drawImage(tmpSnap, 0, 0);
                gc.drawImage(selImg, e.getX(), e.getY());
            }
            saveMe = false;
        });

        canvas.setOnMouseReleased(e -> {
            //Draw is seleceted 
            tmpSnap = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
            canvas.snapshot(null, tmpSnap);
            if (drowbtn.isSelected()) {
                gc.lineTo(e.getX(), e.getY());
                gc.stroke();
                gc.closePath();
            } else if (rubberbtn.isSelected()) {
                // Eraser is selected
                gc.closePath();
            } else if (linebtn.isSelected()) {
                //Line is selected 
                line.setEndX(e.getX());
                line.setEndY(e.getY());
                gc.strokeLine(line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY());

                undoHistory.push(new Line(line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY()));
            } else if (rectbtn.isSelected()) {
                //Rectangle is selected
                rect.setWidth(Math.abs((e.getX() - rect.getX())));
                rect.setHeight(Math.abs((e.getY() - rect.getY())));
                if (rect.getX() > e.getX()) {
                    rect.setX(e.getX());

                }
                if (rect.getY() > e.getY()) {
                    rect.setY(e.getY());
                }

                gc.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                gc.strokeRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());

                undoHistory.push(new Rectangle(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight()));

            } else if (sqrbtn.isSelected()) {
                //Rectangle is selected
                rect.setWidth(Math.abs((e.getX() - rect.getX())));
                rect.setHeight(Math.abs((e.getY() - rect.getY())));
                if (rect.getX() > e.getX()) {
                    rect.setX(e.getX());

                }
                if (rect.getY() > e.getY()) {
                    rect.setY(e.getY());
                }

                gc.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                gc.strokeRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());

                undoHistory.push(new Rectangle(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight()));

            } else if (tribtn.isSelected()) {
                double point3X = e.getX();
                double point3Y = e.getY();
                double point1X = (x0 + point3X) / 2;
                double point1Y = y0;
                double point2Y = point3Y;
                double point2X = x0;

                double[] xpoints = {point1X, point2X, point3X};
                double[] ypoints = {point1Y, point2Y, point3Y};

                gc.fillPolygon(xpoints, ypoints, 3);
                gc.strokePolygon(xpoints, ypoints, 3);

                undoHistory.push(new Polygon(e.getX(), e.getY()));
                redoHistory.push(new Polygon(e.getX(), e.getY()));
            } else if (circlebtn.isSelected()) {
                //Circle is selected 
                circ.setRadius((Math.abs(e.getX() - circ.getCenterX()) + Math.abs(e.getY() - circ.getCenterY())) / 2);

                if (circ.getCenterX() > e.getX()) {
                    circ.setCenterX(e.getX());
                }
                if (circ.getCenterY() > e.getY()) {
                    circ.setCenterY(e.getY());
                }

                gc.fillOval(circ.getCenterX(), circ.getCenterY(), circ.getRadius(), circ.getRadius());
                gc.strokeOval(circ.getCenterX(), circ.getCenterY(), circ.getRadius(), circ.getRadius());

                undoHistory.push(new Circle(circ.getCenterX(), circ.getCenterY(), circ.getRadius()));
            } else if (elpslebtn.isSelected()) {
                //Ellipse is selected 
                elps.setRadiusX(Math.abs(e.getX() - elps.getCenterX()));
                elps.setRadiusY(Math.abs(e.getY() - elps.getCenterY()));

                if (elps.getCenterX() > e.getX()) {
                    elps.setCenterX(e.getX());
                }
                if (elps.getCenterY() > e.getY()) {
                    elps.setCenterY(e.getY());
                }

                gc.strokeOval(elps.getCenterX(), elps.getCenterY(), elps.getRadiusX(), elps.getRadiusY());
                gc.fillOval(elps.getCenterX(), elps.getCenterY(), elps.getRadiusX(), elps.getRadiusY());

                undoHistory.push(new Ellipse(elps.getCenterX(), elps.getCenterY(), elps.getRadiusX(), elps.getRadiusY()));
            } else if (polybtn.isSelected()) {
                numSides = (int) polyint.getValue();
                double radius = ((Math.abs(e.getX() - polyStartX) + Math.abs(e.getY() - polyStartY)) / 2);
                //checks if it is dragged the other direction
                if (polyStartX > e.getX()) {
                    polyStartX = e.getX();
                }
                if (polyStartY > e.getY()) {
                    polyStartY = e.getY();
                }
                //new array for sides
                double[] xSides = new double[numSides];
                double[] ySides = new double[numSides];
                //apply sides to polygon
                for (int i = 0; i < numSides; i++) {
                    xSides[i] = radius * Math.cos(2 * i * Math.PI / numSides) + polyStartX;
                    ySides[i] = radius * Math.sin(2 * i * Math.PI / numSides) + polyStartY;
                }
                //draw polygon
                gc.strokePolygon(xSides, ySides, numSides);
                gc.fillPolygon(xSides, ySides, numSides);
                //push snap for undo
            }//Select
            else if (slcbtn.isSelected()) {
                selRect.setWidth(Math.abs(e.getX() - selRect.getX()));
                selRect.setHeight(Math.abs(e.getY() - selRect.getY()));
                //checks if it is dragged the other direction
                if (selRect.getX() > e.getX()) {
                    selRect.setX(e.getX());
                }
                if (selRect.getY() > e.getY()) {
                    selRect.setY(e.getY());
                }
                //get a new snap
                WritableImage writableImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
                canvas.snapshot(null, writableImage);
                //fill selImg with new selection
                gc.fillRect(selRect.getX(), selRect.getY(), selRect.getWidth(), selRect.getHeight());
                PixelReader pixelReader = writableImage.getPixelReader();
                selImg = new WritableImage(pixelReader, (int) selRect.getX(), (int) selRect.getY(), (int) selRect.getWidth(), (int) selRect.getHeight());
                //clear selected area
                gc.clearRect(selRect.getX(), selRect.getY(), selRect.getWidth(), selRect.getHeight());
                //enable move button
                movebtn.setDisable(false);
                //push snap for undo
            } //Copy
            else if (copybtn.isSelected()) {
                selRect.setWidth(Math.abs(e.getX() - selRect.getX()));
                selRect.setHeight(Math.abs(e.getY() - selRect.getY()));
                //checks if it is dragged the other direction
                if (selRect.getX() > e.getX()) {
                    selRect.setX(e.getX());
                }
                if (selRect.getY() > e.getY()) {
                    selRect.setY(e.getY());
                }
                //get a new snap
                WritableImage writableImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
                canvas.snapshot(null, writableImage);
                //fill selImg with new copy area
                PixelReader pixelReader = writableImage.getPixelReader();
                selImg = new WritableImage(pixelReader, (int) selRect.getX(), (int) selRect.getY(), (int) selRect.getWidth(), (int) selRect.getHeight());
                //enable move button
                movebtn.setDisable(false);
                //push snap for undo
            } else if (movebtn.isSelected()) {
                gc.drawImage(selImg, e.getX(), e.getY());
            }
            saveMe = false;
            redoHistory.clear();
            Shape lastUndo = undoHistory.lastElement();
            lastUndo.setFill(gc.getFill());
            lastUndo.setStroke(gc.getStroke());
            lastUndo.setStrokeWidth(gc.getLineWidth());

        });

        canvas.setOnMousePressed(e -> {
            if (drowbtn.isSelected()) {
                //Draw tool
                active = true;
                checkActivity(drowbtn);
                gc.setStroke(cpLine.getValue());
                gc.beginPath();
                gc.lineTo(e.getX(), e.getY());
            } else if (rubberbtn.isSelected()) {
                active = true;
                checkActivity(rubberbtn);
                gc.setStroke(Color.WHITE);
                //start new path to postition
                gc.beginPath();
                gc.moveTo(e.getX(), e.getY());
                gc.stroke();
            } else if (linebtn.isSelected()) {
                active = true;
                checkActivity(linebtn);
                gc.setStroke(cpLine.getValue());
                line.setStartX(e.getX());
                line.setStartY(e.getY());

            } else if (rectbtn.isSelected()) {
                active = true;
                checkActivity(rectbtn);
                gc.setStroke(cpLine.getValue());
                gc.setFill(cpFill.getValue());
                rect.setX(e.getX());
                rect.setY(e.getY());
            } else if (sqrbtn.isSelected()) {
                active = true;
                checkActivity(sqrbtn);
                gc.setStroke(cpLine.getValue());
                gc.setFill(cpFill.getValue());
                square.setX(e.getX());
                square.setY(e.getY());
            } else if (circlebtn.isSelected()) {
                active = true;
                checkActivity(circlebtn);
                gc.setStroke(cpLine.getValue());
                gc.setFill(cpFill.getValue());
                circ.setCenterX(e.getX());
                circ.setCenterY(e.getY());
            } else if (tribtn.isSelected()) {
                active = true;
                checkActivity(tribtn);
                gc.setStroke(cpLine.getValue());
                gc.setFill(cpFill.getValue());
                x0 = e.getX();
                y0 = e.getY();
            } else if (elpslebtn.isSelected()) {
                active = true;
                checkActivity(elpslebtn);
                gc.setStroke(cpLine.getValue());
                gc.setFill(cpFill.getValue());
                elps.setCenterX(e.getX());
                elps.setCenterY(e.getY());
            } else if (textbtn.isSelected()) {
                active = true;
                checkActivity(textbtn);
                gc.setLineWidth(1);
                gc.setFont(Font.font(slider.getValue()));
                gc.setStroke(cpLine.getValue());
                gc.setFill(cpFill.getValue());
                gc.fillText(text.getText(), e.getX(), e.getY());
                gc.strokeText(text.getText(), e.getX(), e.getY());
            } //Select
            else if (slcbtn.isSelected()) {
                active = true;
                checkActivity(slcbtn);
                selRect.setX(e.getX());
                selRect.setY(e.getY());
            } else if (polybtn.isSelected()) {
                active = true;
                checkActivity(polybtn);
                polyStartX = e.getX();
                polyStartY = e.getY();
            } else if (copybtn.isSelected()) {
                active = true;
                checkActivity(copybtn);
                selRect.setX(e.getX());
                selRect.setY(e.getY());
            }
            saveMe = false;
            redoHistory.clear();
            Shape lastUndo = undoHistory.lastElement();
            lastUndo.setFill(gc.getFill());
            lastUndo.setStroke(gc.getStroke());
            lastUndo.setStrokeWidth(gc.getLineWidth());

        });
        // Color picker
        cpLine.setOnAction(e -> {
            gc.setStroke(cpLine.getValue());
            line_color.setText(cpLine.toString());
        });
        cpFill.setOnAction(e -> {
            gc.setFill(cpFill.getValue());
        });

        // slider
        slider.valueProperty().addListener(e -> {
            double width = slider.getValue();
            if (textbtn.isSelected()) {
                gc.setLineWidth(1);
                gc.setFont(Font.font(slider.getValue()));
                line_width.setText(String.format("%.1f", width));
                return;
            }
            line_width.setText(String.format("%.1f", width));
            gc.setLineWidth(width);
        });

        // Color Dropper
        MenuItem cd = new MenuItem("Color Dropper");

        cd.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent e) {
                        WritableImage written = new WritableImage((int) canvas.getWidth(),
                                (int) canvas.getHeight());
                        SnapshotParameters parameters = new SnapshotParameters();
                        WritableImage snapshot = canvas.snapshot(parameters, written);
                        PixelReader pixel = snapshot.getPixelReader();
                        cpFill.setValue(pixel.getColor((int) e.getX(), (int) e.getY()));
                        cpLine.setValue(pixel.getColor((int) e.getX(), (int) e.getY()));
                    }
                });
            }
        });
        edit.getItems().add(cd);

        /*------- Undo & Redo ------*/
        // Undo
        undo.setOnAction(e -> {
            if (!undoHistory.empty()) {
                gc.clearRect(0, 0, 1080, 790);
                Shape removedShape = undoHistory.lastElement();
                if (removedShape.getClass() == Line.class) {
                    Line tempLine = (Line) removedShape;
                    tempLine.setFill(gc.getFill());
                    tempLine.setStroke(gc.getStroke());
                    tempLine.setStrokeWidth(gc.getLineWidth());
                    redoHistory.push(new Line(tempLine.getStartX(), tempLine.getStartY(), tempLine.getEndX(), tempLine.getEndY()));

                } else if (removedShape.getClass() == Rectangle.class) {
                    Rectangle tempRect = (Rectangle) removedShape;
                    tempRect.setFill(gc.getFill());
                    tempRect.setStroke(gc.getStroke());
                    tempRect.setStrokeWidth(gc.getLineWidth());
                    redoHistory.push(new Rectangle(tempRect.getX(), tempRect.getY(), tempRect.getWidth(), tempRect.getHeight()));
                } else if (removedShape.getClass() == Circle.class) {
                    Circle tempCirc = (Circle) removedShape;
                    tempCirc.setStrokeWidth(gc.getLineWidth());
                    tempCirc.setFill(gc.getFill());
                    tempCirc.setStroke(gc.getStroke());
                    redoHistory.push(new Circle(tempCirc.getCenterX(), tempCirc.getCenterY(), tempCirc.getRadius()));
                } else if (removedShape.getClass() == Ellipse.class) {
                    Ellipse tempElps = (Ellipse) removedShape;
                    tempElps.setFill(gc.getFill());
                    tempElps.setStroke(gc.getStroke());
                    tempElps.setStrokeWidth(gc.getLineWidth());
                    redoHistory.push(new Ellipse(tempElps.getCenterX(), tempElps.getCenterY(), tempElps.getRadiusX(), tempElps.getRadiusY()));
                }
                Shape lastRedo = redoHistory.lastElement();
                lastRedo.setFill(removedShape.getFill());
                lastRedo.setStroke(removedShape.getStroke());
                lastRedo.setStrokeWidth(removedShape.getStrokeWidth());
                undoHistory.pop();

                for (int i = 0; i < undoHistory.size(); i++) {
                    Shape shape = undoHistory.elementAt(i);
                    if (shape.getClass() == Line.class) {
                        Line temp = (Line) shape;
                        gc.setLineWidth(temp.getStrokeWidth());
                        gc.setStroke(temp.getStroke());
                        gc.setFill(temp.getFill());
                        gc.strokeLine(temp.getStartX(), temp.getStartY(), temp.getEndX(), temp.getEndY());
                    } else if (shape.getClass() == Rectangle.class) {
                        Rectangle temp = (Rectangle) shape;
                        gc.setLineWidth(temp.getStrokeWidth());
                        gc.setStroke(temp.getStroke());
                        gc.setFill(temp.getFill());
                        gc.fillRect(temp.getX(), temp.getY(), temp.getWidth(), temp.getHeight());
                        gc.strokeRect(temp.getX(), temp.getY(), temp.getWidth(), temp.getHeight());
                    } else if (shape.getClass() == Circle.class) {
                        Circle temp = (Circle) shape;
                        gc.setLineWidth(temp.getStrokeWidth());
                        gc.setStroke(temp.getStroke());
                        gc.setFill(temp.getFill());
                        gc.fillOval(temp.getCenterX(), temp.getCenterY(), temp.getRadius(), temp.getRadius());
                        gc.strokeOval(temp.getCenterX(), temp.getCenterY(), temp.getRadius(), temp.getRadius());
                    } else if (shape.getClass() == Ellipse.class) {
                        Ellipse temp = (Ellipse) shape;
                        gc.setLineWidth(temp.getStrokeWidth());
                        gc.setStroke(temp.getStroke());
                        gc.setFill(temp.getFill());
                        gc.fillOval(temp.getCenterX(), temp.getCenterY(), temp.getRadiusX(), temp.getRadiusY());
                        gc.strokeOval(temp.getCenterX(), temp.getCenterY(), temp.getRadiusX(), temp.getRadiusY());
                    }
                    saveMe = false;
                }
            } else {
                System.out.println("there is no action to undo");
            }
        });
        edit.getItems().add(undo);
        // Redo
        redo.setOnAction(e -> {
            if (!redoHistory.empty()) {
                Shape shape = redoHistory.lastElement();
                gc.setLineWidth(shape.getStrokeWidth());
                gc.setStroke(shape.getStroke());
                gc.setFill(shape.getFill());

                redoHistory.pop();
                if (shape.getClass() == Line.class) {
                    Line tempLine = (Line) shape;
                    gc.strokeLine(tempLine.getStartX(), tempLine.getStartY(), tempLine.getEndX(), tempLine.getEndY());
                    undoHistory.push(new Line(tempLine.getStartX(), tempLine.getStartY(), tempLine.getEndX(), tempLine.getEndY()));
                } else if (shape.getClass() == Rectangle.class) {
                    Rectangle tempRect = (Rectangle) shape;
                    gc.fillRect(tempRect.getX(), tempRect.getY(), tempRect.getWidth(), tempRect.getHeight());
                    gc.strokeRect(tempRect.getX(), tempRect.getY(), tempRect.getWidth(), tempRect.getHeight());

                    undoHistory.push(new Rectangle(tempRect.getX(), tempRect.getY(), tempRect.getWidth(), tempRect.getHeight()));
                } else if (shape.getClass() == Circle.class) {
                    Circle tempCirc = (Circle) shape;
                    gc.fillOval(tempCirc.getCenterX(), tempCirc.getCenterY(), tempCirc.getRadius(), tempCirc.getRadius());
                    gc.strokeOval(tempCirc.getCenterX(), tempCirc.getCenterY(), tempCirc.getRadius(), tempCirc.getRadius());

                    undoHistory.push(new Circle(tempCirc.getCenterX(), tempCirc.getCenterY(), tempCirc.getRadius()));
                } else if (shape.getClass() == Ellipse.class) {
                    Ellipse tempElps = (Ellipse) shape;
                    gc.fillOval(tempElps.getCenterX(), tempElps.getCenterY(), tempElps.getRadiusX(), tempElps.getRadiusY());
                    gc.strokeOval(tempElps.getCenterX(), tempElps.getCenterY(), tempElps.getRadiusX(), tempElps.getRadiusY());

                    undoHistory.push(new Ellipse(tempElps.getCenterX(), tempElps.getCenterY(), tempElps.getRadiusX(), tempElps.getRadiusY()));
                }
                Shape lastUndo = undoHistory.lastElement();
                lastUndo.setFill(gc.getFill());
                lastUndo.setStroke(gc.getStroke());
                lastUndo.setStrokeWidth(gc.getLineWidth());
            } else {
                System.out.println("there is no action to redo");
            }
        });
        edit.getItems().add(redo);
        /*------- Save, Save As..., & Open ------*/
        // Open
        open.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        open.setOnAction((e) -> {
            FileChooser openFile = new FileChooser();
            openFile.setTitle("Open File");
            openFile.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Documents", "*.*"),
                    new FileChooser.ExtensionFilter("Desktop", "*.*"),
                    new FileChooser.ExtensionFilter("Download", "*.*"),
                    new FileChooser.ExtensionFilter("All Images", "*.*"),
                    new FileChooser.ExtensionFilter("PNG Files", "*.png"),
                    new FileChooser.ExtensionFilter("ICON Files", "*.png"),
                    new FileChooser.ExtensionFilter("JPG Files", "*.jpg")
            );
            File file = openFile.showOpenDialog(primaryStage);

            if (file != null) {
                try {
                    Image picture = new Image(file.toURI().toString());
                    pic.setImage(picture);
                    pic.setPreserveRatio(true);
                    pic.setFitWidth(50);
                    pic.setSmooth(true);
                    pic.setCache(true);

                    gc.drawImage(picture, 0, 0);
                } catch (Exception ex) {
                    System.out.println("Error!");
                    Logger.getLogger(Paint.class.getName()).log(Level.SEVERE, null, ex);
                }
                saveMe = false;
            }
        });

        // Save
        save.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        save.setOnAction((e) -> {
            FileChooser savefile = new FileChooser();
            savefile.setTitle("Save File");

            file = savefile.showSaveDialog(primaryStage);
            if (file != null) {
                try {
                    WritableImage writableImage = new WritableImage(1080, 790);
                    canvas.snapshot(null, writableImage);
                    RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
                    ImageIO.write(renderedImage, "png", file);
                } catch (IOException ex) {
                    System.out.println("Error!");
                }
                saveMe = true;
            }
        });

        //Save As
        MenuItem saveAs = new MenuItem("Save As...");
        // File Chooser to Save as button to work
        saveAs.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHIFT_DOWN, KeyCombination.CONTROL_DOWN));
        saveAs.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                FileChooser fc = new FileChooser();
                fc.setTitle("Save File");
                fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Images", "."), new FileChooser.ExtensionFilter("PNG", "*.png"));
                File save = fc.showSaveDialog(primaryStage);
                if (save != null) {
                    try {
                        WritableImage writableImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
                        canvas.snapshot(null, writableImage);
                        RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
                        ImageIO.write(renderedImage, "png", save);
                        file = save;
                    } catch (IOException ex) {
                        Logger.getLogger(Paint.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    saveMe = true;
                }
            }
        });
        menu.getItems().add(saveAs);

        /* ----------- AUTOSAVE ----------- */
        autosave.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                if (saveFlag == true) {
                    saveFlag = false;
                    autosaveLabel.setText("Autosave off");
                } else {
                    autosaveLabel.setText("Autosave on");

                    if (file != null) {
                        try {
                            WritableImage writableImage = new WritableImage(1080, 790);
                            canvas.snapshot(null, writableImage);
                            RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
                            ImageIO.write(renderedImage, "png", file);
                        } catch (IOException ex) {
                            System.out.println("Error!");
                        }
                        saveMe = true;
                    }
                }

            }

        });

        /* ----------EDIT---------------*/
        // Edit items
        MenuItem cut = new MenuItem("Cut");
        cut.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN));
        MenuItem paste = new MenuItem("Paste");
        paste.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN));
        MenuItem copy = new MenuItem("Copy");
        copy.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
        edit.getItems().add(cut);
        edit.getItems().add(paste);
        edit.getItems().add(copy);

        /* -----------EXIT-----------------------*/
        MenuItem exit = new MenuItem("Exit", null);

        exit.setMnemonicParsing(
                true);
        // Ctrl + X
        exit.setAccelerator(
                new KeyCodeCombination(KeyCode.ESCAPE));
        exit.setOnAction(e -> {
            if (saveMe == true) {
                Platform.exit();
                System.exit(0);
            } else {
                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setTitle("File has not been Saved");
                String prompt = "Would you like to save?";
                alert.setContentText(prompt);

                Optional<ButtonType> show = alert.showAndWait();

                if ((show.isPresent()) && (show.get() == ButtonType.OK)) {
                    //for some reason file is null
                    try {
                        WritableImage written = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
                        canvas.snapshot(null, written);
                        RenderedImage render = SwingFXUtils.fromFXImage(written, null);
                        ImageIO.write(render, "png", file);
                    } catch (IOException ex) {
                        Logger.getLogger(Paint.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    //exits program
                    Platform.exit();
                    System.exit(0);
                } else {
                    //if cancel button is clicked
                    Platform.exit();
                    System.exit(0);
                }
            }
        });
        menu.getItems().add(exit);

        /* ---------HELP----------------*/
        MenuItem about = new MenuItem("About");

        about.setOnAction(new EventHandler<ActionEvent>() {

            public void handle(ActionEvent event) {
                Alert ShortCut = new Alert(Alert.AlertType.INFORMATION);
                ShortCut.setTitle("HELP");
                ShortCut.setHeaderText("Quick Cuts");
                ShortCut.setContentText("Open: Ctrl + O\nSave: Ctrl + S \n"
                        + "Cut: Ctrl + X\nPaste: Ctrl + V\nCopy: Ctrl + C\n"
                        + "Undo: Ctrl + Z\nRedo: Ctrl + Y");
                ShortCut.showAndWait();

            }
        }
        );
        help.getItems().add(about);

        /* ---------RELEASE NOTES--------------*/
        MenuItem rNotes = new MenuItem("Release Notes");
        rNotes.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                FileChooser fc = new FileChooser();
                fc.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("TXT files (*.TXT)", "*.TXT"),
                        new FileChooser.ExtensionFilter("txt files (*.txt)", "*.txt"));
                File file = fc.showOpenDialog(null);
                if (file != null) {
                    try {
                        Files.lines(file.toPath()).forEach(System.out::println);
                    } catch (Exception ex) {
                        Logger.getLogger(JavaFXReadTextFile.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        help.getItems().add(rNotes);

        /* -----------HOW TO-------------------*/
        MenuItem howTo = new MenuItem("How To");

        howTo.setOnAction(new EventHandler<ActionEvent>() {

            public void handle(ActionEvent event) {
                Alert ht = new Alert(Alert.AlertType.INFORMATION);
                ht.setTitle("HELP");
                ht.setHeaderText("How To Use");
                ht.setContentText("* Drawing: Each button (Draw, Rectangle, etc.) is a toggle button. Click on these to start use, "
                        + "then click and drag to draw your shape. Click the button again to deactivate use. \n"
                        + "* Opening a Picture: Click 'File' and 'Open' to upload a picture for you to draw on.\n"
                        + "* Using the Slider: The slider will allow you to resize the thickness of your drawing tool!\n"
                        + "* Undo and Redo: Click these buttons to undo or redo the progress you have made.\n"
                        + "* Adding Text: Click on the 'Text' button. Click on the text box to initiate the abililty to type. Then"
                        + " to add the text to your drawing, click on your drawing. To increase or decrease the size, use the slider.\n"
                        + "* Help Button: If you would like to see hotkeys, or see the features of this release, click on"
                        + "the 'Help' menu and select 'Release Notes' or 'About'.\n");
                ht.showAndWait();

            }
        }
        );
        help.getItems().add(howTo);

        /* -------------ZOOM----------------*/
        MenuItem zoom = new MenuItem("Zoom");
        zoom.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                TextInputDialog zoom = new TextInputDialog("");
                zoom.setHeaderText("Enter zoom in or zoom out precentage");
                zoom.showAndWait();
                String zString = zoom.getEditor().getText();
                int zoomRatio = Integer.parseInt(zString);
                canvas.setScaleX(zoomRatio);
                canvas.setScaleY(zoomRatio);
            }
        });
        menu.getItems().add(zoom);
        /* ----------STAGE & SCENE---------- */
        //Scroll Bar
        Group bar = new Group();
        ScrollPane scroll = new ScrollPane(canvas);
        scroll.setPrefSize(700, 700);
        scroll.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.hbarPolicyProperty().setValue(ScrollPane.ScrollBarPolicy.ALWAYS);
        scroll.vbarPolicyProperty().setValue(ScrollPane.ScrollBarPolicy.ALWAYS);
        scroll.setStyle("-fx-focus-color: transparent;");
        bar.getChildren().add(scroll);

        BorderPane pane = new BorderPane();
        pane.setRight(btns);
        pane.setCenter(scroll);

        MenuBar mb = new MenuBar();
        mb.getMenus().add(menu);
        mb.getMenus().add(help);
        mb.getMenus().add(edit);
        pane.setTop(mb);
        pane.setBottom(tp);

        Scene scene = new Scene(pane, 1000, 700);

        primaryStage.setTitle("Paint");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void checkActivity(ToggleButton selected) {
        if (active == true) {
            active_tool.setText(selected.getText() + " is active.");
        }
    }

    public void checkActivity2(ToggleButton s1, ToggleButton s2, ToggleButton s3, ToggleButton s4,
            ToggleButton s5, ToggleButton s6, ToggleButton s7, ToggleButton s8, ToggleButton s9,
            ToggleButton s10, ToggleButton s11, ToggleButton s12, ToggleButton s13) {
        if (s1.isDisabled() && s2.isDisable() && s3.isDisabled() && s4.isDisabled() && s5.isDisabled()
                && s6.isDisabled() && s7.isDisabled() && s8.isDisabled() && s9.isDisabled() && s10.isDisabled()
                && s11.isDisabled() && s12.isDisabled() && s13.isDisabled()) {
            active = false;
            active_tool.setText("No tool is active.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
