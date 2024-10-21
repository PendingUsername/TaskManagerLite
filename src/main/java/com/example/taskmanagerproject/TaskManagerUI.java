package com.example.taskmanagerproject;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.controlsfx.control.Notifications;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import net.synedra.validatorfx.Validator;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class TaskManagerUI extends Application {
    private TaskManager taskManager = new TaskManager();
    private ObservableList<Task> taskList;
    private Task selectedTask = null;
    private boolean showCompletedTasks = true;
    private Scene mainScene;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Task Manager");

        // Initialize task list and ListView
        taskList = FXCollections.observableArrayList(taskManager.getAllTasks());
        ListView<Task> listView = new ListView<>(taskList);
        listView.setPrefHeight(200);

        // Create a MenuBar
        MenuBar menuBar = new MenuBar();

        // File Menu
        Menu fileMenu = new Menu("File");
        MenuItem saveTasksMenuItem = new MenuItem("Save Tasks");
        MenuItem deleteAllMenuItem = new MenuItem("Delete All Tasks");
        MenuItem exportToTxtMenuItem = new MenuItem("Export to TXT");
        MenuItem exportToCsvMenuItem = new MenuItem("Export to CSV");
        MenuItem exitMenuItem = new MenuItem("Exit");

        fileMenu.getItems().addAll(saveTasksMenuItem, deleteAllMenuItem, new SeparatorMenuItem(), exportToTxtMenuItem, exportToCsvMenuItem, new SeparatorMenuItem(), exitMenuItem);

        // Edit Menu (Undo/Redo placeholders)
        Menu editMenu = new Menu("Edit");
        MenuItem undoMenuItem = new MenuItem("Undo");
        MenuItem redoMenuItem = new MenuItem("Redo");
        editMenu.getItems().addAll(undoMenuItem, redoMenuItem);

        // View Menu
        Menu viewMenu = new Menu("View");
        CheckMenuItem showCompletedMenuItem = new CheckMenuItem("Show Completed Tasks");
        showCompletedMenuItem.setSelected(true); // Default to showing completed tasks

        // Add "Dark Mode" option in the View Menu
        CheckMenuItem darkModeMenuItem = new CheckMenuItem("Dark Mode");

        viewMenu.getItems().addAll(showCompletedMenuItem, darkModeMenuItem);

        // Toggle Dark Mode when "Dark Mode" is checked
        darkModeMenuItem.setOnAction(e -> {
            String lightTheme = getClass().getResource("/css/light-theme.css").toExternalForm();
            String darkTheme = getClass().getResource("/css/dark-theme.css").toExternalForm();

            if (darkModeMenuItem.isSelected()) {
                mainScene.getStylesheets().remove(lightTheme);
                mainScene.getStylesheets().add(darkTheme);
            } else {
                mainScene.getStylesheets().remove(darkTheme);
                mainScene.getStylesheets().add(lightTheme);
            }
        });

        // Help Menu
        Menu helpMenu = new Menu("Help");
        MenuItem aboutMenuItem = new MenuItem("About");
        helpMenu.getItems().add(aboutMenuItem);

        // Add all menus to the MenuBar
        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, helpMenu);

        // Export to TXT file
        exportToTxtMenuItem.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Tasks as TXT");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            File file = fileChooser.showSaveDialog(primaryStage);
            if (file != null) {
                taskManager.exportTasksToTxt(file.getAbsolutePath());
                Notifications.create().title("Exported").text("Tasks exported to TXT").showInformation();
            }
        });

        // Export to CSV file
        exportToCsvMenuItem.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Tasks as CSV");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            File file = fileChooser.showSaveDialog(primaryStage);
            if (file != null) {
                taskManager.exportTasksToCsv(file.getAbsolutePath());
                Notifications.create().title("Exported").text("Tasks exported to CSV").showInformation();
            }
        });

        // Handle "Delete All Tasks" action
        deleteAllMenuItem.setOnAction(e -> {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Deletion");
            confirmAlert.setHeaderText("Delete All Tasks?");
            confirmAlert.setContentText("This will delete all tasks. Are you sure?");
            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    taskManager.deleteAllTasks();
                    taskList.setAll(taskManager.getAllTasks());  // Refresh task list
                    Notifications.create().title("Tasks Deleted").text("All tasks have been deleted.").showInformation();
                }
            });
        });

        // Handle Save Tasks action
        saveTasksMenuItem.setOnAction(e -> {
            taskManager.saveTasksToFile();
            Notifications.create().title("Tasks Saved").text("All tasks have been saved successfully.").showInformation();
        });

        // Handle Exit action
        exitMenuItem.setOnAction(e -> primaryStage.close());

        // Handle About action
        aboutMenuItem.setOnAction(e -> {
            Alert aboutAlert = new Alert(Alert.AlertType.INFORMATION);
            aboutAlert.setTitle("About");
            aboutAlert.setHeaderText("Task Manager Application");
            aboutAlert.setContentText("This is a simple task manager application built with JavaFX.");
            aboutAlert.showAndWait();
        });

        // Toggle completed tasks visibility
        showCompletedMenuItem.setOnAction(e -> {
            showCompletedTasks = showCompletedMenuItem.isSelected();
            refreshTaskList();  // Refresh the ListView based on this setting
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

        HBox timeBox = new HBox(10, hourSpinner, minuteSpinner, amPmComboBox);

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

        // Add/Edit task buttons
        Button addButton = new Button("Add Task");
        Button editButton = new Button("Edit Task");
        editButton.setDisable(true);  // Initially disabled until a task is selected
        Button toggleCompletionButton = new Button("Mark Complete/Incomplete");

        HBox buttonBox = new HBox(10);  // 10px spacing between buttons
        buttonBox.getChildren().addAll(addButton, editButton, toggleCompletionButton);

        // Add Task Action
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
                    hour += 12;
                } else if (amPm.equals("AM") && hour == 12) {
                    hour = 0;
                }

                LocalTime time = LocalTime.of(hour, minute);
                LocalDateTime deadline = LocalDateTime.of(date, time);  // Combine date and time
                TaskPriority priority = priorityComboBox.getValue();

                if (priority == null || date == null) {
                    Notifications.create().title("Error").text("Please select a date and priority").showWarning();
                    return;
                }

                // Add new task
                Task newTask = new Task(taskList.size() + 1, title, description, deadline, priority);
                taskManager.addTask(newTask);

                taskList.setAll(taskManager.getAllTasks());  // Refresh task list
                titleField.clear();
                descriptionField.clear();
                deadlinePicker.setValue(null);  // Clear deadline picker
                priorityComboBox.setValue(null);
                selectedTask = null;
                editButton.setDisable(true);  // Disable edit button when no task is selected
                Notifications.create().title("Task Added").text("Task has been added successfully!").showInformation();
            }
        });

        // Edit Task Action
        editButton.setOnAction(e -> {
            if (selectedTask != null) {
                String title = titleField.getText();
                String description = descriptionField.getText();
                LocalDate date = deadlinePicker.getValue();
                int hour = hourSpinner.getValue();
                int minute = minuteSpinner.getValue();
                String amPm = amPmComboBox.getValue();

                // Convert the 12-hour time with AM/PM to 24-hour time
                if (amPm.equals("PM") && hour != 12) {
                    hour += 12;
                } else if (amPm.equals("AM") && hour == 12) {
                    hour = 0;
                }

                LocalTime time = LocalTime.of(hour, minute);
                LocalDateTime deadline = LocalDateTime.of(date, time);
                TaskPriority priority = priorityComboBox.getValue();

                if (priority == null || date == null) {
                    Notifications.create().title("Error").text("Please select a date and priority").showWarning();
                    return;
                }

                // Update the task
                selectedTask.setTitle(title);
                selectedTask.setDescription(description);
                selectedTask.setDeadline(deadline);
                selectedTask.setPriority(priority);

                taskManager.saveTasksToFile();
                taskList.setAll(taskManager.getAllTasks());  // Refresh task list
                titleField.clear();
                descriptionField.clear();
                deadlinePicker.setValue(null);
                priorityComboBox.setValue(null);
                selectedTask = null;  // Clear selection after editing
                editButton.setDisable(true);  // Disable the edit button when no task is selected
                addButton.setDisable(false);  // Enable the add button again
                Notifications.create().title("Task Updated").text("Task has been updated successfully!").showInformation();
            }
        });

        toggleCompletionButton.setOnAction(e -> {
            if (selectedTask != null) {
                selectedTask.setCompleted(!selectedTask.isCompleted());
                taskManager.saveTasksToFile();
                taskList.setAll(taskManager.getAllTasks());  // Refresh task list
                Notifications.create().title("Task Updated").text("Task completion status has been toggled.").showInformation();
            } else {
                Notifications.create().title("No Task Selected").text("Please select a task to toggle completion.").showWarning();
            }
        });

        // Set ListView cell factory to add icons for task status and priority coloring
        listView.setCellFactory(new Callback<ListView<Task>, ListCell<Task>>() {
            @Override
            public ListCell<Task> call(ListView<Task> param) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(Task task, boolean empty) {
                        super.updateItem(task, empty);
                        if (empty || task == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            if (!showCompletedTasks && task.isCompleted()) {
                                setText(null);
                                setGraphic(null);
                                return;
                            }

                            // Task Title Label
                            Label titleLabel = new Label("Title: " + task.getTitle());
                            titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                            // Task Description Label
                            Label descriptionLabel = new Label("Description: " + task.getDescription());
                            descriptionLabel.setStyle("-fx-font-size: 12px;");

                            // Task Deadline Label
                            Label deadlineLabel = new Label("Deadline: " + (task.getDeadline() != null ? task.getDeadline() : "No deadline"));
                            deadlineLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: darkblue;");

                            // Task Priority Label
                            Label priorityLabel = new Label("Priority: " + task.getPriority());
                            priorityLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + getPriorityColor(task));

                            // Task Status Icon (check mark, hourglass, exclamation mark)
                            FontIcon statusIcon;
                            if (task.isCompleted()) {
                                statusIcon = new FontIcon(FontAwesomeSolid.CHECK);
                                statusIcon.setStyle("-fx-fill: green;");
                            } else if (task.hasDeadlinePassed()) {
                                statusIcon = new FontIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
                                statusIcon.setStyle("-fx-fill: red;");
                            } else {
                                statusIcon = new FontIcon(FontAwesomeSolid.HOURGLASS_HALF);
                                statusIcon.setStyle("-fx-fill: orange;");
                            }

                            // Layout the task info
                            VBox taskInfoBox = new VBox(5, titleLabel, descriptionLabel, deadlineLabel, priorityLabel);
                            HBox cellContent = new HBox(10, statusIcon, taskInfoBox);

                            setGraphic(cellContent);

                            // Add right-click ContextMenu for each task cell
                            ContextMenu contextMenu = new ContextMenu();

                            // Context menu item: Edit Task
                            MenuItem editMenuItem = new MenuItem("Edit Task");
                            editMenuItem.setOnAction(event -> {
                                selectedTask = task;
                                titleField.setText(task.getTitle());
                                descriptionField.setText(task.getDescription());
                                deadlinePicker.setValue(task.getDeadline().toLocalDate());
                                hourSpinner.getValueFactory().setValue(task.getDeadline().getHour() % 12 == 0 ? 12 : task.getDeadline().getHour() % 12);
                                minuteSpinner.getValueFactory().setValue(task.getDeadline().getMinute());
                                amPmComboBox.setValue(task.getDeadline().getHour() < 12 ? "AM" : "PM");
                                priorityComboBox.setValue(task.getPriority());
                                editButton.setDisable(false);
                            });

                            // Context menu item: Mark Complete/Incomplete
                            MenuItem toggleCompleteMenuItem = new MenuItem(task.isCompleted() ? "Mark Incomplete" : "Mark Complete");
                            toggleCompleteMenuItem.setOnAction(event -> {
                                task.setCompleted(!task.isCompleted());
                                taskManager.saveTasksToFile();
                                taskList.setAll(taskManager.getAllTasks());
                                Notifications.create().title("Task Updated").text("Task completion status has been toggled.").showInformation();
                            });

                            // Context menu item: Delete Task
                            MenuItem deleteMenuItem = new MenuItem("Delete Task");
                            deleteMenuItem.setOnAction(event -> {
                                taskManager.deleteTask(task.getId());
                                taskList.setAll(taskManager.getAllTasks());
                                Notifications.create().title("Task Deleted").text("Task has been deleted.").showInformation();
                            });

                            // Add menu items to the context menu
                            contextMenu.getItems().addAll(editMenuItem, toggleCompleteMenuItem, deleteMenuItem);

                            // Show the context menu on right-click (using MouseEvent)
                            setOnMouseClicked((MouseEvent event) -> {
                                if (event.getButton() == MouseButton.SECONDARY && task != null) {
                                    contextMenu.show(this, event.getScreenX(), event.getScreenY());
                                } else {
                                    contextMenu.hide();  // Hide the context menu if not right-click
                                }
                            });
                        }
                    }
                };
            }
        });

        // Handle task selection from the ListView
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedTask = newSelection;
                titleField.setText(selectedTask.getTitle());
                descriptionField.setText(selectedTask.getDescription());
                deadlinePicker.setValue(selectedTask.getDeadline().toLocalDate());
                hourSpinner.getValueFactory().setValue(selectedTask.getDeadline().getHour() % 12 == 0 ? 12 : selectedTask.getDeadline().getHour() % 12);
                minuteSpinner.getValueFactory().setValue(selectedTask.getDeadline().getMinute());
                amPmComboBox.setValue(selectedTask.getDeadline().getHour() < 12 ? "AM" : "PM");
                priorityComboBox.setValue(selectedTask.getPriority());
                editButton.setDisable(false);  // Enable the edit button when a task is selected
            }
        });

        // Create the main layout for the application
        BorderPane mainLayout = new BorderPane();
        // Add the MenuBar to the top of the layout
        mainLayout.setTop(menuBar);
        // Add the rest of your content to the layout (task form, buttons, etc.)
        VBox contentLayout = new VBox(10, titleField, descriptionField, deadlinePicker, timeBox, priorityComboBox, buttonBox, listView);
        contentLayout.setPadding(new Insets(10));
        mainLayout.setCenter(contentLayout);
        // Create a Scene with the main layout
        mainScene = new Scene(mainLayout, 500, 600);
        // Add your CSS files to the Scene
        mainScene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());
        // Set the Scene on the Stage
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    // Helper method to get priority color
    private String getPriorityColor(Task task) {
        switch (task.getPriority()) {
            case LOW:
                return "green;";
            case MEDIUM:
                return "orange;";
            case HIGH:
                return "red;";
            default:
                return "black;";
        }
    }

    // Refresh the task list when toggling completed tasks
    private void refreshTaskList() {
        taskList.setAll(taskManager.getAllTasks());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
