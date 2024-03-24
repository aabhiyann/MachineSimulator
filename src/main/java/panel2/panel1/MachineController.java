package panel2.panel1;


import javafx.animation.Timeline;
import javafx.application.Application;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.concurrent.Task;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class MachineController extends Application {

    private Task<Void> runningTask; //
    private Thread runningThread;
    private HashMap<String, TextField> regMap;
    private HashMap<String, TextField> ixrMap;
    private Memory memory;
    private Timeline poller;
    private CPU cpu;

    // Add other FXML elements as needed
    @FXML
    private TextField TFGPR0;
    @FXML
    private TextField TFGPR1;
    @FXML
    private TextField TFGPR2;
    @FXML
    private TextField TFGPR3;
    @FXML
    private TextField TFIXR1;
    @FXML
    private TextField TFIXR2;
    @FXML
    private TextField TFIXR3;
    @FXML
    private TextField TFBINARY;
    @FXML
    private TextField TFOCTAL;
    @FXML
    private TextField TFPC;
    @FXML
    private TextField TFMAR;
    @FXML
    private TextField TFMBR;
    @FXML
    private TextField TFIR;
    @FXML
    private TextField TFCC;
    @FXML
    private TextField TFMFR;
    @FXML
    private TextField INPUTFILE;
    @FXML
    private TextArea TFCONSOLEPRINTER;
    @FXML
    private TextField TFCONSOLEKEYBOARD;
    @FXML
    private TableView<CacheEntry> cacheTable;
    @FXML
    private TableColumn<CacheEntry, String> tagColumn;
    @FXML
    private TableColumn<CacheEntry, String> valueColumn;
    @FXML
    private Button GPR0;
    @FXML
    private Button GPR1;
    @FXML
    private Button GPR2;
    @FXML
    private Button GPR3;
    @FXML
    private Button IXR0;
    @FXML
    private Button IXR1;
    @FXML
    private Button IXR2;
    @FXML
    private Button PC_Button;
    @FXML
    private Button MAR_Button;
    @FXML
    private Button MBR_Button;
    @FXML
    private Button IR_Button;
    @FXML
    private Button Read20numbers;
    @FXML
    private Button COMPARE;
    @FXML
    private void handleread20numbers() {
        String input = TFCONSOLEKEYBOARD.getText().trim(); // Ensure whitespace from start/end is removed
        String[] numbers = input.split("\\s*,\\s*"); // Split the numbers by comma, allowing for spaces

        if (numbers.length != 20) {
            return;
        }

        cpu.setGPR(1, String.format("%16s", Integer.toBinaryString(Integer.MAX_VALUE)).replace(' ', '0')); // Initialize with max value for comparison
        cpu.setGPR(2, String.format("%16s", Integer.toBinaryString(Integer.MAX_VALUE)).replace(' ', '0')); // Smallest difference also starts at max

        int targetNumber = Integer.parseInt(cpu.getGPR(0), 2); // Retrieve the target number

        for (String numStr : numbers) {
            try {
                int currentNumber = Integer.parseInt(numStr.trim());
                int currentDifference = Math.abs(currentNumber - targetNumber);

                int smallestDifference = Integer.parseInt(cpu.getGPR(2), 2);
                if (currentDifference < smallestDifference) {
                    cpu.setGPR(1, String.format("%16s", Integer.toBinaryString(currentNumber)).replace(' ', '0')); // Update closest number
                    cpu.setGPR(2, String.format("%16s", Integer.toBinaryString(currentDifference)).replace(' ', '0')); // Update smallest difference
                }
            } catch (NumberFormatException e) {
                return;
            }
        }

        updateFromCPU2UI(); // Update the UI to reflect changes
        int closestNumber = Integer.parseInt(cpu.getGPR(1), 2);
        TFCONSOLEPRINTER.appendText("Closest number to target: " + closestNumber + "\n");

        TFCONSOLEKEYBOARD.clear(); // Clear the input field after processing
    }

    @FXML
    private void handlecompare() {
        try {
            int targetNumber = Integer.parseInt(TFCONSOLEKEYBOARD.getText().trim());
            cpu.setGPR(0, String.format("%16s", Integer.toBinaryString(targetNumber)).replace(' ', '0')); // Store the target number with leading zeroes

            updateFromCPU2UI(); // Update the UI with the new target number
            TFCONSOLEKEYBOARD.clear();
            TFCONSOLEPRINTER.appendText("Target number set: " + targetNumber + "\n");
        } catch (NumberFormatException e) {
        }
    }



    public void init(){
        regMap = new HashMap<>();
        regMap.put("00", TFGPR0);
        regMap.put("01", TFGPR1);
        regMap.put("10", TFGPR2);
        regMap.put("11", TFGPR3);
        ixrMap = new HashMap<>();
        ixrMap.put("01", TFIXR1);
        ixrMap.put("10", TFIXR2);
        ixrMap.put("11", TFIXR3);
    }

    @FXML
    private void initialize() {
        //initialization for FXML-related
        TFOCTAL.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                String input = TFOCTAL.getText(); // get user input
                if (input.matches("^[0-7]+$") && input.length() <= 6) { // check for 8 bit octal
                    // transform to binary
                    String binaryString = Integer.toBinaryString(Integer.parseInt(input, 8));
                    // padding with 0
                    String paddedBinaryString = String.format("%16s", binaryString).replace(' ', '0');
                    TFBINARY.setText(paddedBinaryString); // copy to binary textfield
                } else {
                    TFOCTAL.setText("Invalid input!");
                }
            }
        });
        GPR0.setOnAction(event -> {
            TFGPR0.setText(TFBINARY.getText());
            updateFromUI2CPU();
        });
        GPR1.setOnAction(event -> {
            TFGPR1.setText(TFBINARY.getText());
            updateFromUI2CPU();
        });
        GPR2.setOnAction(event -> {
            TFGPR2.setText(TFBINARY.getText());
            updateFromUI2CPU();
        });
        GPR3.setOnAction(event -> {
            TFGPR3.setText(TFBINARY.getText());
            updateFromUI2CPU();
        });
        IXR0.setOnAction(event -> {
            TFIXR3.setText(TFBINARY.getText());
            updateFromUI2CPU();
        });
        IXR1.setOnAction(event -> {
            TFIXR1.setText(TFBINARY.getText());
            updateFromUI2CPU();
        });
        IXR2.setOnAction(event -> {
            TFIXR2.setText(TFBINARY.getText());
            updateFromUI2CPU();
        });
        PC_Button.setOnAction(event -> {
            String binaryData = TFBINARY.getText();
            binaryData = String.format("%12s", binaryData).replace(' ', '0');
            String lowest12bits = binaryData.substring(binaryData.length()-12);
            TFPC.setText(lowest12bits);
            updateFromUI2CPU();
        });
        MAR_Button.setOnAction(event -> {
            String binaryData = TFBINARY.getText();
            binaryData = String.format("%12s", binaryData).replace(' ', '0');
            String lowest12bits = binaryData.substring(binaryData.length()-12);
            TFMAR.setText(lowest12bits);
            updateFromUI2CPU();
        });
        MBR_Button.setOnAction(event -> {
            TFMBR.setText(TFBINARY.getText());
            updateFromUI2CPU();
        });
        IR_Button.setOnAction(event -> {
            TFIR.setText(TFBINARY.getText());
            updateFromUI2CPU();
        });
        memory=new Memory();
        cacheTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tagColumn.setCellValueFactory(cellData -> cellData.getValue().tagProperty());
        valueColumn.setCellValueFactory(cellData -> cellData.getValue().valueProperty());
        cacheTable.setItems(memory.getCache());
        memory.getCache().addListener(new ListChangeListener<CacheEntry>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends CacheEntry> change) {
                while (change.next()) {
                    if (change.wasAdded() || change.wasRemoved() || change.wasUpdated()) {
                        // update if any change in cache
                        cacheTable.refresh();
                    }
                }
            }
        });
        cpu = new CPU();
        cpu.init();
        cpu.setIoCallback(new IOCallback() {
            @Override
            public void onInputreceived(String input) {
                cpu.setKeyboardInput(input);
            }
        });
        cpu.setStateUpdateCallback(() -> {
            Platform.runLater(this::updateFromCPU2UI);
        });
        TFCONSOLEKEYBOARD.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                String input = TFCONSOLEKEYBOARD.getText();
                TFCONSOLEKEYBOARD.clear();
                IOCallback callback=cpu.getIoCallback();
                if(callback!=null) {
                    callback.onInputreceived(input);
                }
            }
        });
    }
    private int updateFromUI2CPU(){
        cpu.setGPR(0,TFGPR0.getText());
        cpu.setGPR(1,TFGPR1.getText());
        cpu.setGPR(2,TFGPR2.getText());
        cpu.setGPR(3,TFGPR3.getText());
        cpu.setIXR(1,TFIXR1.getText());
        cpu.setIXR(2,TFIXR2.getText());
        cpu.setIXR(3,TFIXR3.getText());
        cpu.setPC(TFPC.getText());
        cpu.setMAR(TFMAR.getText());
        cpu.setMBR(TFMBR.getText());
        cpu.setIR(TFIR.getText());
        cpu.setCC(TFCC.getText());
        cpu.setMFR(TFMFR.getText());

        //cpu.setConsolePrinter(TFCONSOLEPRINTER.getText());
        //cpu.setConsoleKeyboard(TFCONSOLEKEYBOARD.getText());
        return 0;
    }
    private int updateFromCPU2UI(){
        TFGPR0.setText(cpu.getGPR(0));
        TFGPR1.setText(cpu.getGPR(1));
        TFGPR2.setText(cpu.getGPR(2));
        TFGPR3.setText(cpu.getGPR(3));
        TFIXR1.setText(cpu.getIXR(1));
        TFIXR2.setText(cpu.getIXR(2));
        TFIXR3.setText(cpu.getIXR(3));
        TFPC.setText(cpu.getPC());
        TFMAR.setText(cpu.getMAR());
        TFMBR.setText(cpu.getMBR());
        TFIR.setText(cpu.getIR());
        TFCC.setText(cpu.getCC());
        TFMFR.setText(cpu.getMFR());
        TFCONSOLEPRINTER.setText(cpu.getConsolePrinter());
        //TFCONSOLEKEYBOARD.setText(cpu.getConsoleKeyboard());
        return 0;
    }


    @FXML
    private void handleRunButton() {
        // concurrency via Task
        runningTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                while(!isCancelled()){
                    // HALT controls isCancelled()
                    cpu.step(memory);
                    Thread.sleep(1000);// sleep 1s each cycle for a clear display in panel
                }
                return null;
            }
        };
        runningTask.setOnSucceeded(e -> System.out.println("Task completed successfully."));
        runningTask.setOnCancelled(e -> System.out.println("Task was cancelled."));
        // start in new thread
        runningThread = new Thread(runningTask);
        runningThread.start();
    }

    @FXML
    private void handleIPLButton() {
        // Initialize the machine, clear memory
        init();
        updateFromCPU2UI();
        String filePath = INPUTFILE.getText();
        memory.loadInstructionsFromFile(filePath);
        String firstLocation = String.format("%12s", Integer.toBinaryString(memory.getFirstAddress())).replace(' ', '0');
        TFPC.setText(firstLocation);
        TFMAR.setText(firstLocation);
        updateFromUI2CPU();
        System.out.println("The IPL button is pressed");

    }

    @FXML
    private void handleHaltButton() {
        // HALT cancels run
        if (runningTask != null && runningTask.isRunning()) {
            runningTask.cancel();
        }
        System.out.println("The Halt button is pressed");
    }

    @FXML
    private void handleStepButton() {
        cpu.step(memory);
    }

    @FXML
    private void handleLoadButton() {
        // Load MBR with memory at MAR
        String address = TFMAR.getText();
        int decimalAdd = Integer.parseInt(address, 2);
        String memoryContent = memory.getMemoryContent(decimalAdd);
        TFMBR.setText(memoryContent);
        updateFromUI2CPU();
        System.out.println("The Load button is pressed");
    }

    @FXML
    private void handleStoreButton() {
        // Store MBR to memory at MAR
        String content = TFMBR.getText();
        String address = TFMAR.getText();
        int decimalAdd = Integer.parseInt(address, 2);
        memory.setMemoryContent(decimalAdd, content);
        updateFromUI2CPU();
        System.out.println("The Store button is pressed");
    }

    @FXML
    private void handleLoadPlusButton() {
        // not implemented yet
        System.out.println("The Load+ button is pressed");
    }

    @FXML
    private void handleStorePlusButton() {
        // not implemented yet
        System.out.println("The Store+ button is pressed");
    }

    @Override
    public void start(Stage stage) throws IOException {
        // set up the UI
        FXMLLoader fxmlLoader = new FXMLLoader(MachineController.class.getResource("machine.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 750);
        stage.setTitle("Machine Simulator");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}