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
import javafx.util.StringConverter;
import org.controlsfx.control.Notifications;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import net.synedra.validatorfx.Validator;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TaskManagerUI extends Application {
    private TaskManager taskManager = new TaskManager();
    private ObservableList<Task> taskList;
    private ObservableList<Task> filteredTaskList;
    private Task selectedTask = null;
    private boolean showCompletedTasks = true;
    private Scene mainScene;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Task Manager");

        // Initialize task list and filtered task list for sorting/filtering
        taskList = FXCollections.observableArrayList(taskManager.getAllTasks());
        filteredTaskList = FXCollections.observableArrayList(taskList);  // Filtered view for sorting and filtering

        ListView<Task> listView = new ListView<>(filteredTaskList);
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

        // Edit Menu
        Menu editMenu = new Menu("Edit");
        MenuItem undoMenuItem = new MenuItem("Undo");
        MenuItem redoMenuItem = new MenuItem("Redo");
        editMenu.getItems().addAll(undoMenuItem, redoMenuItem);

        // View Menu
        Menu viewMenu = new Menu("View");
        CheckMenuItem showCompletedMenuItem = new CheckMenuItem("Show Completed Tasks");
        showCompletedMenuItem.setSelected(true); // Default to showing completed tasks
        CheckMenuItem darkModeMenuItem = new CheckMenuItem("Dark Mode");

        viewMenu.getItems().addAll(showCompletedMenuItem, darkModeMenuItem);

        // Toggle Dark Mode
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

        // Handle showing completed tasks
        showCompletedMenuItem.setOnAction(e -> {
            showCompletedTasks = showCompletedMenuItem.isSelected();
            applyFilterAndSort(null, null, null);
        });

        // Help Menu
        Menu helpMenu = new Menu("Help");
        MenuItem aboutMenuItem = new MenuItem("About");
        helpMenu.getItems().add(aboutMenuItem);
        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, helpMenu);

        // Task Filtering and Sorting Dropdowns
        ComboBox<String> filterDropdown = new ComboBox<>();
        filterDropdown.getItems().addAll("All Tasks", "Completed Tasks", "Incomplete Tasks");
        filterDropdown.setValue("All Tasks");

        ComboBox<String> sortDropdown = new ComboBox<>();
        sortDropdown.getItems().addAll("Deadline", "Priority", "Creation Date");
        sortDropdown.setValue("Deadline");

        // New ComboBox for filtering by category
        ComboBox<String> categoryFilterDropdown = new ComboBox<>();
        categoryFilterDropdown.getItems().add("All Categories"); // Default option
        categoryFilterDropdown.setValue("All Categories");
        populateCategoryFilter(categoryFilterDropdown);  // Dynamically populate based on existing tasks

        filterDropdown.setOnAction(e -> applyFilterAndSort(filterDropdown.getValue(), sortDropdown.getValue(), categoryFilterDropdown.getValue()));
        sortDropdown.setOnAction(e -> applyFilterAndSort(filterDropdown.getValue(), sortDropdown.getValue(), categoryFilterDropdown.getValue()));
        categoryFilterDropdown.setOnAction(e -> applyFilterAndSort(filterDropdown.getValue(), sortDropdown.getValue(), categoryFilterDropdown.getValue()));

        HBox filterSortBox = new HBox(10, new Label("Filter:"), filterDropdown, new Label("Sort:"), sortDropdown, new Label("Category:"), categoryFilterDropdown);
        filterSortBox.setPadding(new Insets(10));

        // Input fields for task details
        TextField titleField = new TextField();
        titleField.setPromptText("Enter task title");

        TextArea descriptionField = new TextArea();
        descriptionField.setPromptText("Enter task description");

        TextField categoryField = new TextField();
        categoryField.setPromptText("Enter task category");

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

        // Ensure the minute spinner displays two digits
        minuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0) {
            {
                setConverter(new StringConverter<>() {
                    @Override
                    public String toString(Integer value) {
                        return value == null ? "00" : String.format("%02d", value);  // Pads with zero if needed
                    }

                    @Override
                    public Integer fromString(String string) {
                        try {
                            return Integer.parseInt(string);
                        } catch (NumberFormatException e) {
                            return 0;  // Default value if invalid input
                        }
                    }
                });
            }
        });

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
                createOrUpdateTask(titleField, descriptionField, categoryField, deadlinePicker, hourSpinner, minuteSpinner, amPmComboBox, priorityComboBox, false);
                populateCategoryFilter(categoryFilterDropdown);  // Refresh category filter after adding a task
            }
        });

        // Edit Task Action
        editButton.setOnAction(e -> {
            if (selectedTask != null) {
                createOrUpdateTask(titleField, descriptionField, categoryField, deadlinePicker, hourSpinner, minuteSpinner, amPmComboBox, priorityComboBox, true);
                populateCategoryFilter(categoryFilterDropdown);  // Refresh category filter after editing a task
            }
        });

        // Toggle task completion status
        toggleCompletionButton.setOnAction(e -> {
            if (selectedTask != null) {
                selectedTask.setCompleted(!selectedTask.isCompleted());
                taskManager.saveTasksToFile();
                taskList.setAll(taskManager.getAllTasks());  // Refresh task list
                applyFilterAndSort(filterDropdown.getValue(), sortDropdown.getValue(), categoryFilterDropdown.getValue());  // Apply filters and sort after toggle
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

                            // Task Category Label
                            Label categoryLabel = new Label("Category: " + task.getCategory());
                            categoryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: darkviolet;");

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
                            VBox taskInfoBox = new VBox(5, titleLabel, descriptionLabel, categoryLabel, deadlineLabel, priorityLabel);
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
                                categoryField.setText(task.getCategory());
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
                                applyFilterAndSort(filterDropdown.getValue(), sortDropdown.getValue(), categoryFilterDropdown.getValue());  // Apply filters and sort after toggle
                                Notifications.create().title("Task Updated").text("Task completion status has been toggled.").showInformation();
                            });

                            // Context menu item: Delete Task
                            MenuItem deleteMenuItem = new MenuItem("Delete Task");
                            deleteMenuItem.setOnAction(event -> {
                                taskManager.deleteTask(task.getId());
                                taskList.setAll(taskManager.getAllTasks());
                                applyFilterAndSort(filterDropdown.getValue(), sortDropdown.getValue(), categoryFilterDropdown.getValue());  // Apply filters and sort after delete
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
                categoryField.setText(selectedTask.getCategory());
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
        mainLayout.setTop(menuBar);
        VBox contentLayout = new VBox(10, filterSortBox, titleField, descriptionField, categoryField, deadlinePicker, timeBox, priorityComboBox, buttonBox, listView);
        contentLayout.setPadding(new Insets(10));
        mainLayout.setCenter(contentLayout);

        // Create a Scene with the main layout
        mainScene = new Scene(mainLayout, 500, 600);
        mainScene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    // Method to create or update a task
    private void createOrUpdateTask(TextField titleField, TextArea descriptionField, TextField categoryField, DatePicker deadlinePicker,
                                    Spinner<Integer> hourSpinner, Spinner<Integer> minuteSpinner, ComboBox<String> amPmComboBox,
                                    ComboBox<TaskPriority> priorityComboBox, boolean isEdit) {
        String title = titleField.getText();
        String description = descriptionField.getText();
        String category = categoryField.getText();  // Retrieve the category
        LocalDate date = deadlinePicker.getValue();
        int hour = hourSpinner.getValue();
        int minute = minuteSpinner.getValue();
        String amPm = amPmComboBox.getValue();

        // Convert the 12-hour time with AM/PM to 24-hour time
        if (amPm.equals("PM") && hour != 12) hour += 12;
        else if (amPm.equals("AM") && hour == 12) hour = 0;

        LocalTime time = LocalTime.of(hour, minute);
        LocalDateTime deadline = LocalDateTime.of(date, time);
        TaskPriority priority = priorityComboBox.getValue();

        if (priority == null || date == null || category.isEmpty()) {
            Notifications.create().title("Error").text("Please select a date, priority, and category").showWarning();
            return;
        }

        if (isEdit && selectedTask != null) {
            selectedTask.setTitle(title);
            selectedTask.setDescription(description);
            selectedTask.setDeadline(deadline);
            selectedTask.setPriority(priority);
            selectedTask.setCategory(category);  // Update the category
        } else {
            Task newTask = new Task(taskList.size() + 1, title, description, deadline, priority, category);
            taskManager.addTask(newTask);
        }

        taskManager.saveTasksToFile();
        taskList.setAll(taskManager.getAllTasks());
        applyFilterAndSort(null, null, null);
    }

    // Method to populate category filter dynamically based on existing tasks
    private void populateCategoryFilter(ComboBox<String> categoryFilterDropdown) {
        Set<String> categories = taskList.stream().map(Task::getCategory).collect(Collectors.toSet());
        categoryFilterDropdown.getItems().setAll("All Categories");
        categoryFilterDropdown.getItems().addAll(categories);
    }

    // Method to apply filters and sorting to the task list
    private void applyFilterAndSort(String filter, String sortBy, String category) {
        Predicate<Task> filterCondition = task -> true;

        if (filter != null) {
            switch (filter) {
                case "Completed Tasks":
                    filterCondition = Task::isCompleted;
                    break;
                case "Incomplete Tasks":
                    filterCondition = task -> !task.isCompleted();
                    break;
                default:
                    filterCondition = task -> true;
                    break;
            }
        }

        if (category != null && !category.equals("All Categories")) {
            Predicate<Task> categoryCondition = task -> task.getCategory().equals(category);
            filterCondition = filterCondition.and(categoryCondition);
        }

        Comparator<Task> comparator = Comparator.comparing(Task::getDeadline);

        if (sortBy != null) {
            switch (sortBy) {
                case "Priority":
                    comparator = Comparator.comparing(Task::getPriority);
                    break;
                case "Creation Date":
                    comparator = Comparator.comparing(Task::getId);  // Assuming ID represents creation order
                    break;
                default:
                    comparator = Comparator.comparing(Task::getDeadline);
                    break;
            }
        }

        filteredTaskList.setAll(taskList.stream().filter(filterCondition).sorted(comparator).collect(Collectors.toList()));
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

    public static void main(String[] args) {
        launch(args);
    }
}
