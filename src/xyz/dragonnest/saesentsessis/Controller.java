package xyz.dragonnest.saesentsessis;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

public class Controller {

    @FXML private ResourceBundle resources;
    @FXML private URL location;
    @FXML private Pane backpane;
    @FXML private Canvas canvas;
    @FXML private Slider slider;
    @FXML private TextField inputThreads;
    @FXML private TextField inputSteps;
    @FXML private Button calcButton;

    private double result, sVal;
    private long executionTime = 0, steps = -1;
    private data[] threadInfo;
    private int threads = -1, finishedThreads = 0;
    private boolean onSlider = false, inProcess = false, needRefresh = false;
    private String dictionary = "1234567890";
    private Color busyCol = new Color(0,0,0,0.2);

    @FXML
    void initialize() {
        assert backpane != null : "fx:id=\"backpane\" was not injected: check your FXML file 'sample.fxml'.";
        assert canvas != null : "fx:id=\"canvas\" was not injected: check your FXML file 'sample.fxml'.";
        assert slider != null : "fx:id=\"slider\" was not injected: check your FXML file 'sample.fxml'.";
        assert inputThreads != null : "fx:id=\"inputThreads\" was not injected: check your FXML file 'sample.fxml'.";
        assert inputSteps != null : "fx:id=\"inputSteps\" was not injected: check your FXML file 'sample.fxml'.";
        assert calcButton != null : "fx:id=\"calcButton\" was not injected: check your FXML file 'sample.fxml'.";
        inputThreads.setOnKeyReleased(event -> {
            String raw = inputThreads.getText();
            if (raw.length()<1) return;
            for (int i = 0; i < raw.length(); i++) {
                boolean found = false;
                for (int j = 0; j < dictionary.length(); j++) {
                    if (raw.charAt(i) == dictionary.charAt(j)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {threads = -1; return;}
            }
            int buff = Integer.parseInt(raw);
            if (buff <= 0) {threads = -1; return;}
            threads = buff;
            threadInfo = null;
            threadInfo = new data[threads];
            for (int i = 0; i < threads; i++) {
                threadInfo[i] = new data();
            }
        });
        inputSteps.setOnKeyReleased(event -> {
            String raw = inputSteps.getText();
            if (raw.length()<1) return;
            for (int i = 0; i < raw.length(); i++) {
                boolean found = false;
                for (int j = 0; j < dictionary.length(); j++) {
                    if (raw.charAt(i) == dictionary.charAt(j)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {steps = -1; return;}
            }
            long buff = Long.parseLong(raw);
            if (buff <= 0) {steps = -1; return;}
            steps = buff;
        });
        calcButton.setOnAction(event -> {
            needRefresh = true;
            run();
        });
        slider.setOnMouseEntered(event -> {
            onSlider = true;
        });
        slider.setOnMouseExited(event -> {
            onSlider = false;
        });
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(20), event -> Update()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void run() {
        finishedThreads = 0;
        result = 0;
        slider.setValue(slider.getMax());
        long start = System.currentTimeMillis();
        double a = 0, b = Math.PI/2; boolean notFullNumber = false;
        double part = (b-a)/threads, buff_ = 0;
        double rawPartSteps = (double)steps/threads;
        long partSteps = (long)rawPartSteps, buff = 0;
        if (rawPartSteps > partSteps) {notFullNumber = true; part = (double)partSteps/steps;}
        for (int i = 0; i < threads; i++) {
            if (notFullNumber) {
                buff+=partSteps; buff_ += part;
                if (i == threads-1) {partSteps = steps-buff+partSteps; part = b-a-buff_+part;}
            }
            double thisA = a+i*part, thisB = a + (i+1)*part;
            threadInfo[i].stepsMade = partSteps;
            threadInfo[i].rangeOccupied = thisB-thisA;
            ThreadProcessor tp = new ThreadProcessor(this, thisA, thisB, (int)partSteps, this::calc);
            new Thread(tp).start();
        }
        try {
            synchronized (this) {
                while (finishedThreads < threads) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        executionTime = System.currentTimeMillis()-start;
        inProcess = false;
        needRefresh = true;
    }

    private void Update() {
        updateSlider();
        updateButton();
        if (slider.getValue() != sVal) {needRefresh = true; sVal = slider.getValue();}
        draw();
    }

    private void draw() {
        if (!needRefresh) return;
        needRefresh = false;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0,0,canvas.getWidth(), canvas.getHeight());
        if (inProcess) {gc.setFill(busyCol); return;}
        double yOff = (slider.getMax()-slider.getValue())/slider.getMax()*canvas.getHeight();
        int yTopOff = 46, xOff = 10, xWidth = (int)(canvas.getWidth()*1.9f)-xOff*2, fontSize = 16, maxPixel = (int)(threads*fontSize*2+yTopOff*1.5-yOff+threads*10);
        if (maxPixel > canvas.getHeight()) {
            slider.setDisable(false);
        } else {
            slider.setDisable(true);
        }
        yOff*=(double)maxPixel/canvas.getHeight();
        gc.setFont(new Font(fontSize));
        gc.setFill(Color.GREY);
        int partOffset = 0;
        for (int i = 0; i < threads; i++) {
            partOffset += 10;
            double currentYOffset = i * fontSize * 2 + yTopOff * 1.5 - yOff + partOffset;
            if (currentYOffset < canvas.getHeight() + 100 && currentYOffset > -50) {
                gc.fillText("Core #" + i + ", steps: " + threadInfo[i].stepsMade + ", range: " + threadInfo[i].rangeOccupied + "\nParralel Execution Time: " + (float) threadInfo[i].timeExecuted / 1000 + "s", xOff, currentYOffset, xWidth);
            }
        }
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), yTopOff);
        gc.setStroke(Color.WHITESMOKE);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.strokeText("Program execution time:"+(float)executionTime/1000+"s. Result: "+result, canvas.getWidth()/2, (double)yTopOff/2+(double)fontSize/2, xWidth);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void updateSlider() {
        if (onSlider) {
            if (slider.getOpacity() < 1) {
                slider.setOpacity(slider.getOpacity()+0.02);
            }
        } else {
            if (slider.getOpacity() > 0.5) {
                slider.setOpacity(slider.getOpacity()-0.02);
            }
        }
    }

    private void updateButton() {
        if (threads > 0 && steps > 0 && !inProcess) {
            calcButton.setDisable(false);
            calcButton.setOpacity(1);
        } else {
            calcButton.setDisable(true);
            calcButton.setOpacity(0.7);
        }
    }

    public synchronized void sendResult(double result, long time) {
        finishedThreads++;
        this.result+=result;
        threadInfo[finishedThreads-1].timeExecuted = System.currentTimeMillis()-time;
        notify();
    }

    public double calc(double t) {
        return Math.sin(2*t)*Math.cos(3*t);
    }

    static class data {
        public long timeExecuted;
        public long stepsMade;
        public double rangeOccupied;
    }
}

