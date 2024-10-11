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

public class TaskManagerUI extends Application {
    private TaskManager taskManager = new TaskManager();
    private ObservableList<Task> taskList;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Task Manager");

        // Initialize ObservableList with loaded tasks
        taskList = FXCollections.observableArrayList(taskManager.getAllTasks());

        // Create ListView to display tasks
        ListView<Task> listView = new ListView<>(taskList);
        listView.setPrefHeight(200);

        // Ensure ListView updates with the loaded tasks at startup
        taskList.setAll(taskManager.getAllTasks());  // Force the ListView to refresh

        // Input fields
        TextField titleField = new TextField();
        titleField.setPromptText("Enter task title");

        TextArea descriptionField = new TextArea();
        descriptionField.setPromptText("Enter task description");

        // Validator for input
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

        // Add task button
        Button addButton = new Button("Add Task");
        addButton.setOnAction(e -> {
            if (validator.validate()) {
                String title = titleField.getText();
                String description = descriptionField.getText();
                Task newTask = new Task(taskList.size() + 1, title, description);
                taskManager.addTask(newTask);
                taskList.setAll(taskManager.getAllTasks());  // Refresh task list in ListView
                titleField.clear();
                descriptionField.clear();
                Notifications.create().title("Task Added").text("Task has been added successfully!").showInformation();
            }
        });

        // Complete task button with FontAwesome icon
        Button completeButton = new Button("Complete Task", new FontIcon(FontAwesomeSolid.CHECK));
        completeButton.setOnAction(e -> {
            Task selectedTask = listView.getSelectionModel().getSelectedItem();
            if (selectedTask != null) {
                taskManager.completeTask(selectedTask.getId());
                taskList.setAll(taskManager.getAllTasks());  // Refresh task list in ListView
                Notifications.create().title("Task Completed").text("Task has been marked as completed.").showInformation();
            }
        });

        // Delete task button with FontAwesome icon
        Button deleteButton = new Button("Delete Task", new FontIcon(FontAwesomeSolid.TRASH));
        deleteButton.setOnAction(e -> {
            Task selectedTask = listView.getSelectionModel().getSelectedItem();
            if (selectedTask != null) {
                taskManager.deleteTask(selectedTask.getId());
                taskList.setAll(taskManager.getAllTasks());  // Refresh task list in ListView
                Notifications.create().title("Task Deleted").text("Task has been deleted.").showInformation();
            }
        });

        // Layout setup
        VBox layout = new VBox(10, titleField, descriptionField, addButton, listView, completeButton, deleteButton);
        layout.setPadding(new Insets(10));

        Scene scene = new Scene(layout, 400, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
