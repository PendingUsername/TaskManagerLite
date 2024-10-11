package com.example.taskmanagerproject;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.controlsfx.control.Notifications;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import net.synedra.validatorfx.Validator;

import java.time.LocalDate;

public class TaskManagerUI extends Application {
    private TaskManager taskManager = new TaskManager();
    private ObservableList<Task> taskList;
    private Task selectedTask = null;  // Track the task being edited

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Task Manager");

        // Initialize ObservableList with loaded tasks
        taskList = FXCollections.observableArrayList(taskManager.getAllTasks());

        // Create ListView to display tasks
        ListView<Task> listView = new ListView<>(taskList);
        listView.setPrefHeight(200);

        // Input fields for task details
        TextField titleField = new TextField();
        titleField.setPromptText("Enter task title");

        TextArea descriptionField = new TextArea();
        descriptionField.setPromptText("Enter task description");

        // DatePicker for deadline
        DatePicker deadlinePicker = new DatePicker();
        deadlinePicker.setPromptText("Select task deadline");

        // Validator for input fields
        Validator validator = new Validator();
        validator.createCheck()
                .dependsOn("title", titleField.textProperty())
                .withMethod(c -> {
                    String title = c.get("title");
                    if (title == null || title.trim().isEmpty()) {
                        c.error("Task title cannot be empty!");
                    }
                })
                .decorates(titleField);

        // Add/Edit task button
        Button addButton = new Button("Add Task");
        addButton.setOnAction(e -> {
            if (validator.validate()) {
                String title = titleField.getText();
                String description = descriptionField.getText();
                LocalDate deadline = deadlinePicker.getValue();  // Get selected deadline

                if (selectedTask == null) {
                    // Add new task
                    Task newTask = new Task(taskList.size() + 1, title, description, deadline);
                    taskManager.addTask(newTask);
                } else {
                    // Update existing task
                    selectedTask.setTitle(title);
                    selectedTask.setDescription(description);
                    selectedTask.setDeadline(deadline);  // Update the deadline
                    taskManager.saveTasksToFile();  // Save updated tasks
                }

                taskList.setAll(taskManager.getAllTasks());  // Refresh task list in ListView
                titleField.clear();
                descriptionField.clear();
                deadlinePicker.setValue(null);  // Clear deadline picker
                selectedTask = null;  // Reset selected task after editing
                addButton.setText("Add Task");  // Change button text back to "Add Task"
                Notifications.create().title("Task Saved").text("Task has been added/updated successfully!").showInformation();
            }
        });

        // Complete task button with FontAwesome icon
        Button completeButton = new Button("Complete Task", new FontIcon(FontAwesomeSolid.CHECK));
        completeButton.setOnAction(e -> {
            Task task = listView.getSelectionModel().getSelectedItem();
            if (task != null) {
                taskManager.completeTask(task.getId());
                taskList.setAll(taskManager.getAllTasks());  // Refresh task list in ListView
                Notifications.create().title("Task Completed").text("Task has been marked as completed.").showInformation();
            }
        });

        // Delete task button with FontAwesome icon
        Button deleteButton = new Button("Delete Task", new FontIcon(FontAwesomeSolid.TRASH));
        deleteButton.setOnAction(e -> {
            Task task = listView.getSelectionModel().getSelectedItem();
            if (task != null) {
                taskManager.deleteTask(task.getId());
                taskList.setAll(taskManager.getAllTasks());  // Refresh task list in ListView
                Notifications.create().title("Task Deleted").text("Task has been deleted.").showInformation();
            }
        });

        // Edit task button: Populate selected task in input fields
        Button editButton = new Button("Edit Task", new FontIcon(FontAwesomeSolid.PENCIL_ALT));
        editButton.setOnAction(e -> {
            Task task = listView.getSelectionModel().getSelectedItem();
            if (task != null) {
                selectedTask = task;  // Track the task being edited
                titleField.setText(task.getTitle());
                descriptionField.setText(task.getDescription());
                deadlinePicker.setValue(task.getDeadline());  // Populate deadline picker
                addButton.setText("Save Task");  // Change button text to "Save Task"
            }
        });

        // Layout setup
        VBox layout = new VBox(10, titleField, descriptionField, deadlinePicker, addButton, listView, editButton, completeButton, deleteButton);
        layout.setPadding(new Insets(10));

        Scene scene = new Scene(layout, 450, 450);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
