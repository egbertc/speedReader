/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uchicago.egbertc.profinaladv;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

public class RsvpFXMLController {

    private Task<Void> readerTask;
    //private String readString;
    private String[] wordSplit;
    public int wpm = 500;
    public ExecutorService executor;
    //PDDocument doc = new PDDocument();

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private TableView<FileDownTask> tblFiles;

    @FXML
    private Tab tabWelcome01;

    @FXML
    private Label lblReader;

    @FXML
    private Tab tabReader;

    @FXML
    private ToggleButton btnToggleRead;

    @FXML
    private Button btnFileFinder;

    @FXML
    private Button btnNext;

    @FXML
    private ProgressBar readProgress;

    @FXML
    private Label lblWPM;
    
    @FXML
    private Label lblLoadMessage;
    
    @FXML
    private Label lblFileTitle;

    @FXML
    private ListView<File> lstFiles;

    @FXML
    private TextField txtSpeed;

    @FXML
    private ProgressIndicator splitProgress;

    @FXML
    void updateSpeed(ActionEvent event) {
        wpm = Integer.parseInt(txtSpeed.getText());
        System.out.println(txtSpeed.getText());
        txtSpeed.setText("");
        lblWPM.setText(wpm + " words/minute");
    }

    @FXML
    void goToReader(ActionEvent event) {
        tabWelcome01.getTabPane().getSelectionModel().selectNext();
        System.out.println("X: "+lblReader.getTranslateX());
        System.out.println("Y: "+lblReader.getTranslateY());
        System.out.println("BaselineOff: "+lblReader.getBaselineOffset());
        //System.out.println("Y: "+lblReader.
    }

    @FXML
    void fileDrag(ActionEvent event) {
        System.out.println("FILE DRAG");
    }

    @FXML
    void fileFinder(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Files");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("Text Files", "*.txt", "*.pdf"));

        //lstFiles.getSelectionModel().clearSelection();
        
        List<File> selected = fileChooser.showOpenMultipleDialog(null);
        ObservableList<File> files = FXCollections.observableArrayList();

        for (File f : selected) {
            files.add(f);
        }

        lstFiles.getItems().addAll(files);
        
        

        lstFiles.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<File>() {
            @Override
            public void changed(ObservableValue<? extends File> observable, File oldValue, File newValue) {
                if(newValue != null && newValue.exists() )
                {    
                    System.out.println("SELECTED: " + newValue.getName());
                    final Task<String[]> task = new FileDownTask(newValue);

                    task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                        @Override
                        public void handle(WorkerStateEvent t) {
                            wordSplit = task.getValue();
                            System.out.println("LOADED!");
                            System.out.println("Words: " + wordSplit.length);
                            setupReader();
                        }

                    });
                
                    splitProgress.progressProperty().bind(task.progressProperty());
                    lblLoadMessage.textProperty().bind(task.messageProperty());
                    lblFileTitle.textProperty().bind(task.titleProperty());
                    //splitProgress.set
                    executor = Executors.newFixedThreadPool(1, new ThreadFactory() {
                        @Override
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r);
                            t.setDaemon(true);
                            return t;
                        }
                    });

                    executor.execute(task);

                }
            }
        });
        
        if(lstFiles.getItems().size() == 1)
            lstFiles.getSelectionModel().clearAndSelect(0);
        
    }

    @FXML
    void toggleRead(ActionEvent event) {
        System.out.println("BUTTON is Selected: " + btnToggleRead.isSelected());

        if (!btnToggleRead.isSelected()) {
            btnToggleRead.setText("Read");
            if (readerTask.isRunning()) {
                readerTask.cancel();
            }
        } else {
            btnToggleRead.setText("Stop");
            initRead();
        }
    }

    @FXML
    void initialize() {
        assert tabWelcome01 != null : "fx:id=\"tabWelcome01\" was not injected: check your FXML file 'RsvpFXML.fxml'.";
        assert lblReader != null : "fx:id=\"lblReader\" was not injected: check your FXML file 'RsvpFXML.fxml'.";
        assert tabReader != null : "fx:id=\"tabReader\" was not injected: check your FXML file 'RsvpFXML.fxml'.";
        assert btnToggleRead != null : "fx:id=\"btnToggleRead\" was not injected: check your FXML file 'RsvpFXML.fxml'.";

        lstFiles.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        
    }

    private void initRead() {

        Executor readExec = Executors.newFixedThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });

        readerTask = new Task<Void>() {
                int letWidth = 37;
                int x = 0;
                int y = 0;
                int moveX = 0;
                int prevX = 0;
                int anchorX = 0;
                
            private final IntegerProperty speed = new SimpleIntegerProperty(wpm);
            public final void updateSpeed()
            {
                speed.set(wpm);
            }
            public final int getSpeed() {
                //speed.set(wpm);
                return speed.get();
            }

            public final void setSpeed(int spd) {
                this.speed.set(spd);
            }

            public IntegerProperty speedProperty() {
                return speed;
            }

            @Override
            protected Void call() throws Exception {
                int current = 0;
//                for(int j = 0; j<200;j++)
//                    System.out.println("WORD #" +(j+1) + ": " + wordSplit[j]);
                
                int totCount = wordSplit.length;
                for (final String str : wordSplit) {  
                    final String cleanStr = str.trim();
                    if(!str.isEmpty())
                    {
                        
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                
                                lblReader.setTranslateX(calcPlacement(cleanStr));
                                
                                lblReader.setText(cleanStr);
                                //System.out.println("Width: " + lblReader.getWidth());
                            }
                        });
                        updateSpeed();
                        Thread.sleep((60 * 1000 / getSpeed())+(punctuationDelay(cleanStr)*getSpeed()/20));
                        current++;
                        this.updateProgress(current, totCount);
                    }
                }
                
                

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        //.setDisable(false);
                        //spd.set(Integer.parseInt(txtSpeed.getText()));
                        System.out.println("SPEED: " + getSpeed());
                    }
                });
                
                
                
                return null;
            }
            private double calcPlacement(String str) {
                int anchPrev = anchorX;
                int thirds = str.length()/3;
                if(str.length()<=5)
                    thirds = 2;
                if(str.length() == 1)
                    thirds = 1;
                
                moveX = ((2-thirds)-anchPrev)*letWidth;
                anchorX = 2-thirds;
                //moveX += anchTemp;
                System.out.println("len: " +str.length()+ " | thirds: " +thirds + " | prev-anchor: " + anchPrev +  " | curr-anchor: " + anchorX +" | move: " + moveX + " | " + str);
                return moveX;
            }
            
            private int punctuationDelay(String s) {
                char endChar = s.charAt(s.length() - 1);
                switch (endChar) {
                    case '.':
                        return 5;
                    case ',':
                        return 3;
                    case '?':
                        return 5;
                    case '!':
                        return 2;
                    case ':':
                        return 3;
                    default:
                        return 0;
                }

            }

        };
//Slider slider = new Slider();
////slider.
//        readerTask.speedProperty().bind(slider.valueProperty());
        readProgress.progressProperty().bind(readerTask.progressProperty());
        readExec.execute(readerTask);
    }

    private void setupReader() {
        btnNext.setDisable(false);
        tabReader.setDisable(false);
    }

    

    class FileDownTask extends Task<String[]> {

        private final File loadFile;

        public FileDownTask(File f) {
            loadFile = f;
            this.updateTitle(loadFile.getName().substring(0,loadFile.getName().length()-4));
            this.updateMessage("standing by");
            this.updateProgress(0, 10);
        }

        @Override
        protected String[] call() throws Exception {
            if (loadFile.getAbsolutePath().endsWith(".txt")) {
                this.updateMessage(loadFile.getName() + "loading txt file");
                return breakApart(parseTextFile());
            } else if (loadFile.getAbsolutePath().endsWith(".pdf")) {
                this.updateMessage(loadFile.getName() + "loading pdf file");
                return breakApart(parsePdfFile());
            }

            return null;
        }

        private String parsePdfFile() {
            try {
                this.updateProgress(0, 10);
                PDDocument pdfDoc = PDDocument.load(loadFile);
                this.updateProgress(2, 10);
                PDFTextStripper stripper = new PDFTextStripper();
                String data = "";
                int pages = stripper.getEndPage();
                this.updateProgress(4, 10);
                data = stripper.getText(pdfDoc);
                
                this.updateProgress(6, 10);
//                int currentPage = 1;
//                int interval = pages/100;
//                if(interval < 1)
//                    interval = 1;
//                
//                while(currentPage < pages)
//                {
//                    stripper.setStartPage(currentPage);
//                    
//                    if(currentPage + interval <= pages)
//                    {
//                        currentPage+=interval;
//                        stripper.setEndPage(currentPage);                        
//                    }
//                    else
//                    {
//                        currentPage = pages;
//                        stripper.setEndPage(pages);
//                    }
//                    
//                    data += stripper.getText(pdfDoc);
//                    this.updateProgress(currentPage, pages);
//                    System.out.println("Total Pages: " + pages + " | Current: " + currentPage);
//                }

                pdfDoc.close();
                this.updateProgress(8, 10);

                return data;
            } catch (IOException ex) {
                System.out.println("ERROR STRIPPING PDF");
                Logger.getLogger(RsvpFXMLController.class.getName()).log(Level.SEVERE, null, ex);
            }

            return null;
        }

        private String parseTextFile() {
            try {
                Scanner s = new Scanner(loadFile);
                String data = "";
                long fileSize = loadFile.getTotalSpace();
                long loaded = (long) 0.0;
                while (s.hasNext()) {
                    String line = s.next();
                    loaded += line.length() * 2;
                    //data += line.replace("\n", " ");
                    data += line;
                    this.updateProgress(loaded, fileSize*1.2);
                }
                s.close();
                this.updateProgress(80, 100);
                return data;
            } catch (FileNotFoundException e) {
                System.out.println("TEXT FILE READ ERROR");
            }
            return null;
        }

        private String[] breakApart(String in) {
            this.updateMessage(loadFile.getName() + " seperating words");
            String removeBreaks = in.replace("\n", " ");
            String[] words = removeBreaks.split(" ");
            ArrayList<String> wordsClean = new ArrayList<>();
            double counter = 0.0;
            double total = words.length;
            for(String w : words)
            {
                w = w.trim();
                if(!w.isEmpty())
                {
                    if(w.length()<=9 || w.endsWith(".") || w.endsWith(",") || w.endsWith("?") || w.endsWith("!") || w.endsWith(";") || w.endsWith(":") || w.endsWith("'") || w.endsWith("\""))
                    {
                        wordsClean.add(w);
                    }
                    else
                    {
                        String[] doubleBreak = shatter(w);
                        wordsClean.addAll(Arrays.asList(doubleBreak));
                    }
                    
                }
                    
                double percentage = counter/total;
                this.updateProgress(80+ (20*percentage), 100);
                counter++;
                //System.out.println("PERCENTAGE: " + percentage);
            }            
            String[] cleanedIn = new String[wordsClean.size()];
            wordsClean.toArray(cleanedIn);
            this.updateProgress(100, 100);
            this.updateMessage(loadFile.getName() + " ready.");
            return cleanedIn;
        }
        
        private String[] shatter(String tooBig)
        {
            double endCount = tooBig.length()/8;
            int wordCount = (int)endCount;
            if(tooBig.length() % 8 > 0 )
                wordCount++;
            
            String[] children = new String[wordCount];
            for(int i = 0; i < wordCount-1; i++)
            {
                children[i] = tooBig.substring((i*8), (i*8)+8) + "-";
            }
            children[wordCount-1] = tooBig.substring((wordCount-1)*8, tooBig.length());
//            System.out.println("&&&&&");
//            System.out.println(tooBig);
//            System.out.println(Arrays.toString(children));
//            System.out.println("&&&&&");
            return children;
        }

    }

    
}
