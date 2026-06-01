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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.time.LocalDate;

public class HelloApplication extends Application {

    //Files and Streams (plain text record file)
    private static final String FILE_PATH = "tasks.txt";
    private ObservableList<String> taskList = FXCollections.observableArrayList();

    @Override
    public void start(Stage primaryStage) {
        // 1. Load old tasks from file when the application opens.
        loadTasksFromFile();

        // 2. Dynamically monitor dates and alerts.
        checkReminders();

        // Basic GUI Components
        ListView<String> listView = new ListView<>(taskList);

        TextField taskInput = new TextField();
        taskInput.setPromptText("Enter a new task...");
        taskInput.setPrefWidth(220);

        ComboBox<String> timePicker = new ComboBox<>();
        timePicker.getItems().addAll("Today", "Tomorrow", "Next Week");
        timePicker.setValue("Today");
        timePicker.setPrefWidth(120);

        Button addButton = new Button("Add");
        Button deleteButton = new Button("Delete Selected");

        // CSS Styling (Font sizes and Placeholder color)
        listView.setStyle("-fx-font-size: 16px; -fx-background-radius: 5;");

        // The tooltip text color has been set to #95a5a6 (dark soft gray).
        taskInput.setStyle("-fx-font-size: 15px; -fx-background-radius: 5; -fx-border-color: #bdc3c7; -fx-prompt-text-fill: #7f8c8d; -fx-opacity: 1.0;");
        timePicker.setStyle("-fx-font-size: 15px; -fx-background-radius: 5; -fx-border-color: #bdc3c7;");

        addButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-background-radius: 5;");
        deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;");
        deleteButton.setMaxWidth(Double.MAX_VALUE);

        // --- TASK ADDITION LOGIC AND INPUT VALIDATION ---
        Runnable addNewTaskAction = () -> {
            String newTask = taskInput.getText().trim();
            String selectedTime = timePicker.getValue();

            if (!newTask.isEmpty()) {
                String targetDate;

                if (selectedTime.equals("Tomorrow")) {
                    targetDate = LocalDate.now().plusDays(1).toString();
                } else if (selectedTime.equals("Next Week")) {
                    targetDate = LocalDate.now().plusWeeks(1).toString();
                } else {
                    targetDate = LocalDate.now().toString(); // "Today"
                }

                taskList.add("[ ] " + newTask + " (" + targetDate + ")");
                taskInput.clear();
                checkReminders();
                saveTasksToFile();
            } else {
                // Warning window that will appear when an empty task is entered
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning");
                alert.setHeaderText("Empty Task!");
                alert.setContentText("You cannot add an empty task. Please write something!");
                alert.showAndWait();
            }
        };

        // Triggers (Button Click and ENTER Key)
        addButton.setOnAction(e -> addNewTaskAction.run());
        taskInput.setOnAction(e -> addNewTaskAction.run());

        // Task Deletion Action
        deleteButton.setOnAction(e -> {
            int selectedIndex = listView.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                taskList.remove(selectedIndex);
                saveTasksToFile();
            }
        });

        // (MouseEvent): To check/uncheck when double-clicked.
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                int selectedIndex = listView.getSelectionModel().getSelectedIndex();

                if (selectedIndex >= 0) {
                    String currentTask = taskList.get(selectedIndex);

                    if (currentTask.startsWith("[ ] ")) {
                        String updatedTask = currentTask.replace("[ ] ", "[✔] ");
                        updatedTask = updatedTask.replace(" [TODAY!]", "").replace(" [OVERDUE!]", "");
                        taskList.set(selectedIndex, updatedTask);
                    } else if (currentTask.startsWith("[✔] ")) {
                        String updatedTask = currentTask.replace("[✔] ", "[ ] ");
                        taskList.set(selectedIndex, updatedTask);
                    }

                    checkReminders();
                    saveTasksToFile();
                }
            }
        });

        // Layout Panes
        HBox inputPanel = new HBox(10, taskInput, timePicker, addButton);
        VBox mainPanel = new VBox(15, inputPanel, listView, deleteButton);
        mainPanel.setStyle("-fx-padding: 15; -fx-background-color: #f5f6fa;");

        Scene scene = new Scene(mainPanel, 500, 500);
        primaryStage.setTitle("To-Do List Application");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- PRECISE TIME AND DELAY CONTROL WITH STRING METHODS ---
    private void checkReminders() {
        LocalDate today = LocalDate.now();

        for (int i = 0; i < taskList.size(); i++) {
            String task = taskList.get(i);

            // Only check for incomplete tasks.
            if (task.startsWith("[ ] ")) {

                // 1. Condition: If the date is today and there is no alert, add [TODAY!]
                if (task.contains(today.toString()) && !task.contains("[TODAY!]")) {
                    taskList.set(i, task + " [TODAY!]");
                }
                // 2. Condition: Historical data verification (Secure system that prevents duplicate label printing)
                else {
                    try {
                        // By temporarily clearing potential alerts, we always lock the history to the very end.
                        String cleanTask = task.replace(" [TODAY!]", "").replace(" [OVERDUE!]", "");
                        int len = cleanTask.length();

                        if (cleanTask.endsWith(")")) {
                            // We safely extract the 10 characters before the final closing parenthesis.
                            String dateStr = cleanTask.substring(len - 11, len - 1);
                            LocalDate taskDate = LocalDate.parse(dateStr);

                            // If the mission date is before today and there is already no [OVERDUE!] warning
                            if (taskDate.isBefore(today) && !task.contains("[OVERDUE!]")) {
                                // We are completely removing the old [TODAY!] warning from the text and replacing it with [OVERDUE!].
                                String safeTask = task.replace(" [TODAY!]", "");
                                taskList.set(i, safeTask + " [OVERDUE!]");
                            }
                        }
                    } catch (Exception e) {
                        // Prevent the program from crashing if a conversion error occurs. (Exception Handling)
                    }
                }
            }
        }
    }

    //BufferedWriter and Exception Handling (Save plain text)
    private void saveTasksToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (String task : taskList) {
                writer.write(task);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
        }
    }

    //Reading from a File with BufferedReader
    private void loadTasksFromFile() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                taskList.add(line);
            }
        } catch (IOException e) {
            System.out.println("Error loading file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}