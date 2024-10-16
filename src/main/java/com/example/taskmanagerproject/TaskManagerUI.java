package com.example.taskmanagerproject;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.controlsfx.control.Notifications;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import net.synedra.validatorfx.Validator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

        // Set ListView cell factory to add icons for task status and restore priority coloring
        listView.setCellFactory(new Callback<ListView<Task>, ListCell<Task>>() {
            @Override
            public ListCell<Task> call(ListView<Task> param) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(Task task, boolean empty) {
                        super.updateItem(task, empty);
                        if (empty || task == null) {
                            setText(null);
                            setGraphic(null);  // Reset icon when empty
                        } else {
                            // Create the task text label
                            Label taskLabel = new Label(task.toString());

                            // Set the color of the task text based on priority
                            if (task.getPriority() == TaskPriority.LOW) {
                                taskLabel.setStyle("-fx-text-fill: green;");
                            } else if (task.getPriority() == TaskPriority.MEDIUM) {
                                taskLabel.setStyle("-fx-text-fill: orange;");
                            } else if (task.getPriority() == TaskPriority.HIGH) {
                                taskLabel.setStyle("-fx-text-fill: red;");
                            }

                            // Create the appropriate status icon (check mark, hourglass, or exclamation mark)
                            FontIcon statusIcon;
                            if (task.isCompleted()) {
                                statusIcon = new FontIcon(FontAwesomeSolid.CHECK);
                                statusIcon.setIconSize(18);
                                statusIcon.setStyle("-fx-fill: green;");  // Green check mark for completed
                            } else if (task.hasDeadlinePassed()) {
                                statusIcon = new FontIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
                                statusIcon.setIconSize(18);
                                statusIcon.setStyle("-fx-fill: red;");  // Red exclamation mark for deadline passed
                            } else {
                                statusIcon = new FontIcon(FontAwesomeSolid.HOURGLASS_HALF);
                                statusIcon.setIconSize(18);
                                statusIcon.setStyle("-fx-fill: orange;");  // Orange hourglass for in-progress
                            }

                            // Create HBox to display the icon before the task text
                            HBox cellContent = new HBox(10);  // 10px spacing
                            cellContent.getChildren().addAll(statusIcon, taskLabel);  // Icon before the text

                            setGraphic(cellContent);  // Set the HBox as the graphic for this cell
                        }
                    }
                };
            }
        });

        // Input fields for task details
        TextField titleField = new TextField();
        titleField.setPromptText("Enter task title");

        TextArea descriptionField = new TextArea();
        descriptionField.setPromptText("Enter task description");

        // DatePicker for the deadline date
        DatePicker deadlinePicker = new DatePicker();
        deadlinePicker.setPromptText("Select task date");

        // Time selection using Spinner for 12-hour clock and ComboBox for AM/PM
        Spinner<Integer> hourSpinner = new Spinner<>(1, 12, 12);
        Spinner<Integer> minuteSpinner = new Spinner<>(0, 59, 0);
        ComboBox<String> amPmComboBox = new ComboBox<>(FXCollections.observableArrayList("AM", "PM"));
        amPmComboBox.setValue("PM");  // Default to PM
        hourSpinner.setPrefWidth(60);
        minuteSpinner.setPrefWidth(60);

        HBox timeBox = new HBox(10, hourSpinner, minuteSpinner, amPmComboBox);  // Combine time spinners in an HBox

        // ComboBox for task priority
        ComboBox<TaskPriority> priorityComboBox = new ComboBox<>();
        priorityComboBox.setItems(FXCollections.observableArrayList(TaskPriority.values()));
        priorityComboBox.setPromptText("Select task priority");

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
                LocalDate date = deadlinePicker.getValue();
                int hour = hourSpinner.getValue();
                int minute = minuteSpinner.getValue();
                String amPm = amPmComboBox.getValue();

                // Convert the 12-hour time with AM/PM to 24-hour time
                if (amPm.equals("PM") && hour != 12) {
                    hour += 12;  // Convert PM hours to 24-hour format
                } else if (amPm.equals("AM") && hour == 12) {
                    hour = 0;  // Midnight case (12 AM is 00:00 in 24-hour time)
                }

                LocalTime time = LocalTime.of(hour, minute);
                LocalDateTime deadline = LocalDateTime.of(date, time);  // Combine date and time
                TaskPriority priority = priorityComboBox.getValue();  // Get selected priority

                if (priority == null || date == null) {
                    Notifications.create().title("Error").text("Please select a date and priority").showWarning();
                    return;  // Prevent task creation without priority or date
                }

                if (selectedTask == null) {
                    // Add new task
                    Task newTask = new Task(taskList.size() + 1, title, description, deadline, priority);
                    taskManager.addTask(newTask);
                } else {
                    // Update existing task
                    selectedTask.setTitle(title);
                    selectedTask.setDescription(description);
                    selectedTask.setDeadline(deadline);
                    selectedTask.setPriority(priority);  // Update priority
                    taskManager.saveTasksToFile();  // Save updated tasks
                }

                taskList.setAll(taskManager.getAllTasks());  // Refresh task list in ListView
                titleField.clear();
                descriptionField.clear();
                deadlinePicker.setValue(null);  // Clear deadline picker
                priorityComboBox.setValue(null);  // Clear priority selection
                selectedTask = null;  // Reset selected task after editing
                addButton.setText("Add Task");  // Change button text back to "Add Task"
                Notifications.create().title("Task Saved").text("Task has been added/updated successfully!").showInformation();
            }
        });

        // Toggle completion button
        Button toggleCompletionButton = new Button("Toggle Completion");
        toggleCompletionButton.setOnAction(e -> {
            Task task = listView.getSelectionModel().getSelectedItem();
            if (task != null) {
                // Toggle the completion status of the selected task
                task.setCompleted(!task.isCompleted());
                taskManager.saveTasksToFile();  // Save the updated completion status

                // Refresh the task list to show the updated completion status
                taskList.setAll(taskManager.getAllTasks());
                Notifications.create().title("Task Updated")
                        .text("Task has been " + (task.isCompleted() ? "completed." : "marked as in-progress."))
                        .showInformation();
            } else {
                Notifications.create().title("No Task Selected").text("Please select a task to toggle completion.").showWarning();
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
                deadlinePicker.setValue(task.getDeadline().toLocalDate());  // Populate date picker

                // Convert 24-hour time to 12-hour time for display
                int hour = task.getDeadline().getHour();
                String amPm = (hour >= 12) ? "PM" : "AM";
                if (hour > 12) hour -= 12;  // Convert to 12-hour format
                if (hour == 0) hour = 12;   // Midnight case
                hourSpinner.getValueFactory().setValue(hour);
                minuteSpinner.getValueFactory().setValue(task.getDeadline().getMinute());
                amPmComboBox.setValue(amPm);  // Set AM/PM combo box

                priorityComboBox.setValue(task.getPriority());  // Populate priority combo box
                addButton.setText("Save Task");  // Change button text to "Save Task"
            }
        });

        // Layout setup
        VBox layout = new VBox(10, titleField, descriptionField, deadlinePicker, timeBox, priorityComboBox, addButton, toggleCompletionButton, listView, editButton, deleteButton);
        layout.setPadding(new Insets(10));

        Scene scene = new Scene(layout, 500, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
