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
import java.util.Optional; // Şifre penceresinin buton kontrolü için paket

public class HelloApplication extends Application {

    // Files and Streams (plain text record file)
    private static final String FILE_PATH = "tasks.txt";
    private ObservableList<String> taskList = FXCollections.observableArrayList();

    @Override
    public void start(Stage primaryStage) {
        // --- SECURE LOGIN WINDOW (PASSWORD CHECK) ---
        boolean isAuthorized = false;

        while (!isAuthorized) {
            TextInputDialog passwordDialog = new TextInputDialog();
            passwordDialog.setTitle("Login Required");
            passwordDialog.setHeaderText("To-Do List Secure Authentication");
            passwordDialog.setContentText("Please enter the application password:");

            // Eğer kullanıcı iptal butonuna basarsa veya pencereyi kapatırsa uygulamayı tamamen kapat
            Optional<String> result = passwordDialog.showAndWait();
            if (!result.isPresent()) {
                System.exit(0);
            }

            String enteredPassword = result.get().trim();

            // Giriş şifresini "1234" olarak belirledik
            if (enteredPassword.equals("1234")) {
                isAuthorized = true; // Şifre doğru, döngü sonlanır ve ana ekran açılır
            } else {
                // Şifre yanlışsa hata mesajı gösterir
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Invalid Password!");
                errorAlert.setContentText("The password you entered is incorrect. Please try again!");
                errorAlert.showAndWait();
            }
        }

        // --- MAIN APPLICATION LOGIC (Şifre doğruysa burası devreye girer) ---
        // 1. Load old tasks from file when the application opens.
        loadTasksFromFile();

        // 2. Dynamically monitor dates and alerts.
        checkReminders();

        // Basic GUI Components (Standard JavaFX)
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
                // Boş görev girildiğinde uyarı penceresi
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

        // (MouseEvent): Çift tıklandığında görevi tamamlandı/tamamlanmadı yapar.
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

        // Layout Panes (Sade Hizalama)
        HBox inputPanel = new HBox(10, taskInput, timePicker, addButton);
        VBox mainPanel = new VBox(15, inputPanel, listView, deleteButton);

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

            if (task.startsWith("[ ] ")) {
                if (task.contains(today.toString()) && !task.contains("[TODAY!]")) {
                    taskList.set(i, task + " [TODAY!]");
                }
                else {
                    try {
                        String cleanTask = task.replace(" [TODAY!]", "").replace(" [OVERDUE!]", "");
                        int len = cleanTask.length();

                        if (cleanTask.endsWith(")")) {
                            String dateStr = cleanTask.substring(len - 11, len - 1);
                            LocalDate taskDate = LocalDate.parse(dateStr);

                            if (taskDate.isBefore(today) && !task.contains("[OVERDUE!]")) {
                                String safeTask = task.replace(" [TODAY!]", "");
                                taskList.set(i, safeTask + " [OVERDUE!]");
                            }
                        }
                    } catch (Exception e) {
                        // Exception Handling
                    }
                }
            }
        }
    }

    // BufferedWriter and Exception Handling (Save plain text)
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

    // Reading from a File with BufferedReader
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