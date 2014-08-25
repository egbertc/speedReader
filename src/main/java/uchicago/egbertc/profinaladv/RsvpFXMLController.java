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
import java.util.List;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Callback;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;


public class RsvpFXMLController {
    
    private Task readTask;
    private String readString;
    private String[] wordSplit;
    public static final int WORD_DELAY = 400;
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
    void fileDrag(ActionEvent event) {
        System.out.println("FILE DRAG");
    }

    @FXML
    void fileFinder(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Files");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("Text Files", "*.txt", "*.pdf"));
        
        List<File> selected = fileChooser.showOpenMultipleDialog(null);
        
        addFiles(selected);
    }
    
    @FXML
    void toggleRead(ActionEvent event) {
       // btnToggleRead.setSelected(!btnToggleRead.isSelected());
        System.out.println("BUTTON is Selected: " +btnToggleRead.isSelected());
        
        if(readTask.isRunning())
            readTask.cancel();
        else
        {
            initTask();
            new Thread(readTask).start();
        }
        
    }

    @FXML
    void initialize() {
        assert tabWelcome01 != null : "fx:id=\"tabWelcome01\" was not injected: check your FXML file 'RsvpFXML.fxml'.";
        assert lblReader != null : "fx:id=\"lblReader\" was not injected: check your FXML file 'RsvpFXML.fxml'.";
        assert tabReader != null : "fx:id=\"tabReader\" was not injected: check your FXML file 'RsvpFXML.fxml'.";
        assert btnToggleRead != null : "fx:id=\"btnToggleRead\" was not injected: check your FXML file 'RsvpFXML.fxml'.";
        
        
        readString = "Hello there, the Angel from my nightmare. The Shadow in the background of the morgue. "
                + "The unsuspecting victim, of darkness in the valley we can live like jack and sally if we want. "
                + "Where you can always find me, we'll have halloween on christmas and in the night we'll wish this never ends... "
                + "wish this never ends..."; 
        
        TableColumn<FileDownTask, String> statusCol = new TableColumn("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<FileDownTask, String>(
                "message"));
        statusCol.setPrefWidth(100);

        TableColumn<FileDownTask, Double> progressCol = new TableColumn("Progress");
        progressCol.setCellValueFactory(new PropertyValueFactory<FileDownTask, Double>(
                "progress"));
        
        final TableColumn<FileDownTask, Boolean> btnCol = new TableColumn("Load");
        btnCol.setSortable(false);
        
        btnCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FileDownTask, Boolean>, ObservableValue<Boolean>>()
        {
            @Override
            public ObservableValue<Boolean> call(TableColumn.CellDataFeatures<FileDownTask, Boolean> features)
            {
                return new SimpleBooleanProperty(features.getValue() != null);
            }
        });
        
        btnCol.setCellFactory(new Callback<TableColumn<FileDownTask, Boolean>, TableCell<FileDownTask, Boolean>>()
        {
            @Override
            public TableCell<FileDownTask, Boolean> call(TableColumn<FileDownTask, Boolean> personBooleanTableColumn)
            {
                return new ToggleButtonCell(tblFiles, btnCol);
            }
        });

        progressCol.setPrefWidth(125);

        //this is the most important call
        progressCol.setCellFactory(ProgressBarTableCell.<FileDownTask>forTableColumn());

        TableColumn<FileDownTask, String> titleCol = new TableColumn("File");
        titleCol.setCellValueFactory(new PropertyValueFactory<FileDownTask, String>(
                "title"));
        titleCol.setPrefWidth(175);

        //add the cols
        tblFiles.getColumns().addAll(titleCol, statusCol, btnCol, progressCol);
        
        executor = Executors.newFixedThreadPool(1, new ThreadFactory()
        {
            @Override
            public Thread newThread(Runnable r)
            {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });
        
//        for (FileDownTask pbarTask : tblFiles.getItems()) {
//            executor.execute(pbarTask);
//        }
        
        
        initTask();
    }
    
    private class ToggleButtonCell extends TableCell<FileDownTask, Boolean>
    {
        final ToggleButton tgl = new ToggleButton("Select");
        
        ToggleButtonCell(final TableView tbl, final TableColumn col )
        {            
            tgl.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    int index = tbl.getItems().size();
                    System.out.println("INDEX: " + index);
                    FileDownTask task = (FileDownTask) tbl.getItems().get(index-1);
                    executor.execute(task);
                    
                    System.out.println("TEST.");
                }
            });
        }
        
        @Override protected void updateItem(Boolean item, boolean empty)
        {
            super.updateItem(item, empty);
            if(!empty)
            {
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setGraphic(tgl);
            }
        }
        
        
        
    }
    
    private void initTask()
    {
        final String[] readWords = parseString(readString);
        readTask = new Task<Void>() {

            @Override
            protected Void call() throws Exception {

                for (final String str : readWords) {
                    int delayBonus = punctuationDelay(str);
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            lblReader.setText(str);
                        }
                    });
                    //DELAY words per minute
                    Thread.sleep((60 * 1000 / WORD_DELAY )  + (delayBonus*7500/WORD_DELAY));
                }

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        btnToggleRead.setSelected(false);
                    }
                });

                return null;
            }
        };
    }
    
    private void addFiles(List<File> files)
    {
        for(final File f: files)
        {
            final Task<String[]> task = new FileDownTask(f);
            
            task.setOnSucceeded(new EventHandler<WorkerStateEvent>()
            {
                    @Override
                    public void handle(WorkerStateEvent t)
                    {
                        wordSplit = task.getValue();                        
                        System.out.println("LOADED!");
                        System.out.println("Words: " + wordSplit.length);
                    }
            });
            tblFiles.getItems().add((FileDownTask) task);
        }        
    }
    
    class FileDownTask extends Task<String[]>
    {
        private final File loadFile;
        public FileDownTask(File f)
        {
            loadFile = f;
            this.updateTitle(loadFile.getName());
            this.updateMessage("standing by");
        }
        
        @Override
        protected String[] call() throws Exception {
            if(loadFile.getAbsolutePath().endsWith(".txt"))
            {
                this.updateMessage("loading txt file");
                return breakApart(parseTextFile());
            }
            else if(loadFile.getAbsolutePath().endsWith(".pdf"))
            {
                this.updateMessage("loading pdf file");
                return breakApart(parsePdfFile());
            }
            
            return null;
        }
        
        private String parsePdfFile()
        {
            try
            {
                this.updateProgress(1, 10);
                PDDocument pdfDoc = PDDocument.load(loadFile);
                PDFTextStripper stripper = new PDFTextStripper();
                String data = "";
                int pages = stripper.getEndPage();
                this.updateProgress(4, 10);
                data = stripper.getText(pdfDoc);
                this.updateProgress(10, 10);
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
                
                return data;
            }
            catch(IOException ex)
            {
                System.out.println("ERROR STRIPPING PDF");
                Logger.getLogger(RsvpFXMLController.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            return null;
        }
        
        private String parseTextFile()
        {
            try
            {
                Scanner s = new Scanner(loadFile);
                String data = "";
                long fileSize = loadFile.getTotalSpace();
                long loaded = (long) 0.0;
                while(s.hasNext())
                {
                    String line = s.next();
                    loaded += line.length()*2;
                    data += line.replace("\n", " ");
                    this.updateProgress(loaded, fileSize);
                }
                this.updateProgress(100,100);
                return data;
            }
            catch(FileNotFoundException e)
            {
                System.out.println("TEXT FILE READ ERROR");
            }
            return null;
        }
        
        private String[] breakApart(String in)
        {
            this.updateMessage("seperating words");
            String[] words = in.split(" ");
            this.updateMessage("ready.");
            return words;
        }
        
    }
    
    private int punctuationDelay(String s)
    {
        char endChar = s.charAt(s.length()-1);
        switch(endChar)
        {
            case '.': return 5;                
            case ',': return 3;
            case '?': return 5;
            case '!': return 2;
            case ':': return 3;
            default: return 0;            
        }
            
    }
    
    private String[] parseString(String in)
    {
        String[] words = in.split(" ");
        return words;
    }
}

