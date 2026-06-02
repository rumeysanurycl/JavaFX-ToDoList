package com.example.todolist;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.time.LocalDate;
import java.util.Optional; // Required package to handle dialog button controls

public class HelloApplication extends Application {

    // File Operations: Path of the plain text file where tasks are saved
    private static final String FILE_PATH = "tasks.txt";
    // JavaFX ObservableList: Automatically reflects list changes to the user interface
    private ObservableList<String> taskList = FXCollections.observableArrayList();

    @Override
    public void start(Stage primaryStage) {
        // --- SECURE LOGIN WINDOW (PASSWORD CHECK) ---
        boolean isAuthorized = false;

        // The loop continues and blocks the main application until the correct password is typed
        while (!isAuthorized) {
            TextInputDialog passwordDialog = new TextInputDialog();
            passwordDialog.setTitle("Login Required");
            passwordDialog.setHeaderText("To-Do List Secure Authentication");
            passwordDialog.setContentText("Please enter the application password:");

            // If the user clicks the "Cancel" button or closes the window, close the application safely
            Optional<String> result = passwordDialog.showAndWait();
            if (!result.isPresent()) {
                System.exit(0);
            }

            String enteredPassword = result.get().trim();

            // The application password is set to "1234"
            if (enteredPassword.equals("1234")) {
                isAuthorized = true; // Password is correct, the loop ends and the main dashboard opens
            } else {
                // If the password is wrong, show an error alert message window
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Invalid Password!");
                errorAlert.setContentText("The password you entered is incorrect. Please try again!");
                errorAlert.showAndWait();
            }
        }

        // --- MAIN APPLICATION LOGIC (Runs only if the password check passes) ---
        // 1. Read and load old tasks from the local file when the application starts
        loadTasksFromFile();

        // 2. Dynamically check deadlines and update alert tags
        checkReminders();

        // Basic GUI Components Initialization (Standard JavaFX Nodes)
        ListView<String> listView = new ListView<>(taskList);

        TextField taskInput = new TextField();
        taskInput.setPromptText("Enter a new task..."); // Placeholder text helper
        taskInput.setPrefWidth(220);

        ComboBox<String> timePicker = new ComboBox<>();
        timePicker.getItems().addAll("Today", "Tomorrow", "Next Week");
        timePicker.setValue("Today"); // Default choice selection
        timePicker.setPrefWidth(120);

        Button addButton = new Button("Add");
        Button deleteButton = new Button("Delete Selected");
        deleteButton.setMaxWidth(Double.MAX_VALUE); // Stretches the button horizontally across the layout

        // --- TASK ADDITION LOGIC AND INPUT VALIDATION ---
        Runnable addNewTaskAction = () -> {
            String newTaskText = taskInput.getText().trim();
            String selectedTime = timePicker.getValue();

            // Add the task to the list if the text entry field is not empty
            if (!newTaskText.isEmpty()) {
                String targetDate;

                // Calculate the specific date using the LocalDate class based on the ComboBox selection
                if (selectedTime.equals("Tomorrow")) {
                    targetDate = LocalDate.now().plusDays(1).toString();
                } else if (selectedTime.equals("Next Week")) {
                    targetDate = LocalDate.now().plusWeeks(1).toString();
                } else {
                    targetDate = LocalDate.now().toString(); // Default choice: "Today"
                }

                // Append the format brackets [ ] and save the task entry into the list sequence
                taskList.add("[ ] " + newTaskText + " (" + targetDate + ")");
                taskInput.clear(); // Clear the text input field
                checkReminders();  // Refresh the dynamic alert state indicators
                saveTasksToFile();  // Synchronize data immediately with the local plane text repository
            } else {
                // Warning dialog pop-up that appears when the user attempts to submit an empty task
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning");
                alert.setHeaderText("Empty Task!");
                alert.setContentText("You cannot add an empty task. Please write something!");
                alert.showAndWait();
            }
        };

        // Action Triggers: Dispatched upon clicking the "Add" button or pressing the ENTER key
        addButton.setOnAction(e -> addNewTaskAction.run());
        taskInput.setOnAction(e -> addNewTaskAction.run());

        // Task Deletion Action Trigger
        deleteButton.setOnAction(e -> {
            int selectedIndex = listView.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                taskList.remove(selectedIndex); // Remove the highlighted row index from the collection mapping
                saveTasksToFile(); // Commit changes into the local plain text layout
            }
        });

        // Double Click Operational Trigger (MouseEvent): Toggle task status between completed [✔] and active [ ]
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) { // Detect double click layout events
                int selectedIndex = listView.getSelectionModel().getSelectedIndex();

                if (selectedIndex >= 0) {
                    String currentTask = taskList.get(selectedIndex);

                    // If the item is incomplete, change to completed and clear old reminder warning tokens
                    if (currentTask.startsWith("[ ] ")) {
                        String updatedTask = currentTask.replace("[ ] ", "[✔] ");
                        updatedTask = updatedTask.replace(" [TODAY!]", "").replace(" [OVERDUE!]", "");
                        taskList.set(selectedIndex, updatedTask);
                    }
                    // If the item is already checked, toggle it back to the active incomplete state
                    else if (currentTask.startsWith("[✔] ")) {
                        String updatedTask = currentTask.replace("[✔] ", "[ ] ");
                        taskList.set(selectedIndex, updatedTask);
                    }

                    checkReminders(); // Recalculate context indicators
                    saveTasksToFile(); // Commit updates to the data repository stream
                }
            }
        });

        // Layout Containers and Structural Alignment (Plain Safe Architecture - CSS-Free Style)
        HBox inputPanel = new HBox(10, taskInput, timePicker, addButton); // Horizontal alignment box
        VBox mainPanel = new VBox(15, inputPanel, listView, deleteButton); // Vertical structure layout layout

        Scene scene = new Scene(mainPanel, 500, 500);
        primaryStage.setTitle("To-Do List Application");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- TIMING DATA PROCESSING METHODS WITH CORE STRING MANIPULATION ---
    private void checkReminders() {
        LocalDate today = LocalDate.now();

        for (int i = 0; i < taskList.size(); i++) {
            String task = taskList.get(i);

            // Only track deadline indicators for active (incomplete) tasks
            if (task.startsWith("[ ] ")) {

                // Condition 1: If the milestone date matches today and doesn't have an indicator, append [TODAY!]
                if (task.contains(today.toString()) && !task.contains("[TODAY!]")) {
                    taskList.set(i, task + " [TODAY!]");
                }
                // Condition 2: Check for overdue context records via substring parsing frameworks
                else {
                    try {
                        // Temporarily clean tags to evaluate string structures safely
                        String cleanTask = task.replace(" [TODAY!]", "").replace(" [OVERDUE!]", "");
                        int len = cleanTask.length();

                        // Extract the 10-character date sequence from the inside of the final closing parentheses
                        if (cleanTask.endsWith(")")) {
                            String dateStr = cleanTask.substring(len - 11, len - 1);
                            LocalDate taskDate = LocalDate.parse(dateStr);

                            // If the item calendar record predates today, add the [OVERDUE!] notification tag
                            if (taskDate.isBefore(today) && !task.contains("[OVERDUE!]")) {
                                String safeTask = task.replace(" [TODAY!]", "");
                                taskList.set(i, safeTask + " [OVERDUE!]");
                            }
                        }
                    } catch (Exception e) {
                        // Exception Handling: Prevents runtime application failures upon incorrect date formats
                    }
                }
            }
        }
    }

    // Commit Structural Tasks Persistence mapping utilizing BufferedWriter Streams
    private void saveTasksToFile() {
        // Try-with-resources statement: Closes streaming parameters automatically to handle system memory safely
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (String task : taskList) {
                writer.write(task);
                writer.newLine(); // Write each task sequence item line-by-line
            }
        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
        }
    }

    // Retrieve Structural Input tasks mapping utilizing BufferedReader Streams
    private void loadTasksFromFile() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return; // If the file is not yet initialized, abort initialization process

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            // Iterate through every plain text code line map until the sequence hits empty boundaries
            while ((line = reader.readLine()) != null) {
                taskList.add(line);
            }
        } catch (IOException e) {
            System.out.println("Error loading file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args); // Launch the JavaFX Application Context
    }
}