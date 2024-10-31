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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;


public class TaskManagerUI extends Application {
    private TaskManager taskManager = new TaskManager();
    private ObservableList<Task> taskList;
    private ObservableList<Task> filteredTaskList;
    private Task selectedTask = null;
    private boolean showCompletedTasks = true;
    private Scene mainScene;

    // Email Configuration
    private static final String SMTP_SERVER = "smtp.gmail.com";
    private static final String USERNAME = "your-email@gmail.com";
    private static final String PASSWORD = "your-email-password";
    private static final String EMAIL_FROM = "your-email@gmail.com";

    // Undo/Redo functionality
    private Stack<List<Task>> undoStack = new Stack<>();
    private Stack<List<Task>> redoStack = new Stack<>();

    // Reminder-related
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Map<Task, Runnable> taskReminderMap = new HashMap<>();

    // PIN protection-related variables
    private boolean pinProtectionEnabled = false;
    private String userPin = "";
    private final Path configFile = Paths.get("config.txt");

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Task Manager");

        // Load PIN configuration on startup
        loadConfig();

        // Prompt for PIN if enabled
        if (pinProtectionEnabled && !promptForPin()) {
            System.exit(0); // Exit if PIN is incorrect
        }

        // Initialize task list and filtered task list for sorting/filtering
        taskList = FXCollections.observableArrayList(taskManager.getAllTasks());
        filteredTaskList = FXCollections.observableArrayList(taskList);

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
        MenuItem exportToCalendarMenuItem = new MenuItem("Export to Calendar (.ics)");
        MenuItem emailTasksMenuItem = new MenuItem("Email Tasks");
        MenuItem exitMenuItem = new MenuItem("Exit");

        fileMenu.getItems().addAll(saveTasksMenuItem, deleteAllMenuItem, new SeparatorMenuItem(), exportToTxtMenuItem, exportToCsvMenuItem, exportToCalendarMenuItem, emailTasksMenuItem, new SeparatorMenuItem(), exitMenuItem);

        emailTasksMenuItem.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Email Tasks");
            dialog.setHeaderText("Enter recipient email address");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(email -> {
                sendTasksByEmail(taskManager.getAllTasks(), email);
                Notifications.create().title("Email Sent").text("Tasks have been emailed to " + email).showInformation();
            });
        });

        // Add calendar export functionality
        exportToCalendarMenuItem.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Tasks as Calendar (.ics)");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ICS Files", "*.ics"));
            File file = fileChooser.showSaveDialog(primaryStage);
            if (file != null) {
                exportTasksToCalendar(file.getAbsolutePath());
                Notifications.create().title("Exported").text("Tasks exported to calendar (.ics)").showInformation();
            }
        });

        // Edit Menu
        Menu editMenu = new Menu("Edit");
        MenuItem undoMenuItem = new MenuItem("Undo");
        MenuItem redoMenuItem = new MenuItem("Redo");
        editMenu.getItems().addAll(undoMenuItem, redoMenuItem);

        // View Menu
        Menu viewMenu = new Menu("View");
        CheckMenuItem showCompletedMenuItem = new CheckMenuItem("Show Completed Tasks");
        showCompletedMenuItem.setSelected(true);
        CheckMenuItem darkModeMenuItem = new CheckMenuItem("Dark Mode");

        viewMenu.getItems().addAll(showCompletedMenuItem, darkModeMenuItem);

        // Settings Menu
        Menu settingsMenu = new Menu("Settings");
        CheckMenuItem enablePinMenuItem = new CheckMenuItem("Enable PIN Protection");
        enablePinMenuItem.setSelected(pinProtectionEnabled);
        enablePinMenuItem.setOnAction(e -> togglePinProtection());
        settingsMenu.getItems().add(enablePinMenuItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, settingsMenu);

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

        // Task Filtering and Sorting Dropdowns
        ComboBox<String> filterDropdown = new ComboBox<>();
        filterDropdown.getItems().addAll("All Tasks", "Completed Tasks", "Incomplete Tasks");
        filterDropdown.setValue("All Tasks");

        ComboBox<String> sortDropdown = new ComboBox<>();
        sortDropdown.getItems().addAll("Deadline", "Priority", "Creation Date");
        sortDropdown.setValue("Deadline");

        // ComboBox for category filter
        ComboBox<String> categoryFilterDropdown = new ComboBox<>();
        categoryFilterDropdown.getItems().add("All Categories");
        categoryFilterDropdown.setValue("All Categories");
        populateCategoryFilter(categoryFilterDropdown);

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
        amPmComboBox.setValue("PM");
        hourSpinner.setPrefWidth(60);
        minuteSpinner.setPrefWidth(60);

        // Ensure the minute spinner displays two digits
        minuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0) {
            {
                setConverter(new StringConverter<>() {
                    @Override
                    public String toString(Integer value) {
                        return value == null ? "00" : String.format("%02d", value);
                    }

                    @Override
                    public Integer fromString(String string) {
                        try {
                            return Integer.parseInt(string);
                        } catch (NumberFormatException e) {
                            return 0;
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
        editButton.setDisable(true);
        Button toggleCompletionButton = new Button("Mark Complete/Incomplete");

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(addButton, editButton, toggleCompletionButton);

        // Undo/Redo Menu actions
        undoMenuItem.setOnAction(e -> {
            if (!undoStack.isEmpty()) {
                backupTaskList(redoStack);
                List<Task> previousState = undoStack.pop();
                taskManager.setTasks(previousState);
                taskList.setAll(taskManager.getAllTasks());
                applyFilterAndSort(null, null, null);
            }
        });

        redoMenuItem.setOnAction(e -> {
            if (!redoStack.isEmpty()) {
                backupTaskList(undoStack);
                List<Task> nextState = redoStack.pop();
                taskManager.setTasks(nextState);
                taskList.setAll(taskManager.getAllTasks());
                applyFilterAndSort(null, null, null);
            }
        });

        // Add Task Action
        addButton.setOnAction(e -> {
            if (validator.validate()) {
                backupTaskList(undoStack);
                createOrUpdateTask(titleField, descriptionField, categoryField, deadlinePicker, hourSpinner, minuteSpinner, amPmComboBox, priorityComboBox, false);
                populateCategoryFilter(categoryFilterDropdown);
            }
        });

        // Edit Task Action
        editButton.setOnAction(e -> {
            if (selectedTask != null) {
                backupTaskList(undoStack);
                createOrUpdateTask(titleField, descriptionField, categoryField, deadlinePicker, hourSpinner, minuteSpinner, amPmComboBox, priorityComboBox, true);
                populateCategoryFilter(categoryFilterDropdown);
            }
        });

        // Toggle task completion status
        toggleCompletionButton.setOnAction(e -> {
            if (selectedTask != null) {
                backupTaskList(undoStack);
                selectedTask.setCompleted(!selectedTask.isCompleted());
                taskManager.saveTasksToFile();
                taskList.setAll(taskManager.getAllTasks());
                applyFilterAndSort(filterDropdown.getValue(), sortDropdown.getValue(), categoryFilterDropdown.getValue());
                Notifications.create().title("Task Updated").text("Task completion status has been toggled.").showInformation();
            } else {
                Notifications.create().title("No Task Selected").text("Please select a task to toggle completion.").showWarning();
            }
        });

        // Set ListView cell factory to add icons for task status and priority coloring
        listView.setCellFactory(new Callback<>() {
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
                                applyFilterAndSort(filterDropdown.getValue(), sortDropdown.getValue(), categoryFilterDropdown.getValue());
                                Notifications.create().title("Task Updated").text("Task completion status has been toggled.").showInformation();
                            });

                            // Context menu item: Delete Task
                            MenuItem deleteMenuItem = new MenuItem("Delete Task");
                            deleteMenuItem.setOnAction(event -> {
                                backupTaskList(undoStack);
                                taskManager.deleteTask(task.getId());
                                taskList.setAll(taskManager.getAllTasks());
                                applyFilterAndSort(filterDropdown.getValue(), sortDropdown.getValue(), categoryFilterDropdown.getValue());
                                Notifications.create().title("Task Deleted").text("Task has been deleted.").showInformation();
                            });

                            contextMenu.getItems().addAll(editMenuItem, toggleCompleteMenuItem, deleteMenuItem);

                            setOnMouseClicked((MouseEvent event) -> {
                                if (event.getButton() == MouseButton.SECONDARY && task != null) {
                                    contextMenu.show(this, event.getScreenX(), event.getScreenY());
                                } else {
                                    contextMenu.hide();
                                }
                            });
                        }
                    }
                };
            }
        });

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
                editButton.setDisable(false);
            }
        });

        saveTasksMenuItem.setOnAction(e -> {
            taskManager.saveTasksToFile();
            Notifications.create().title("Tasks Saved").text("All tasks have been saved successfully.").showInformation();
        });

        deleteAllMenuItem.setOnAction(e -> {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Deletion");
            confirmAlert.setHeaderText("Delete All Tasks?");
            confirmAlert.setContentText("This will delete all tasks. Are you sure?");
            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    backupTaskList(undoStack);
                    taskManager.deleteAllTasks();
                    taskList.setAll(taskManager.getAllTasks());
                    Notifications.create().title("Tasks Deleted").text("All tasks have been deleted.").showInformation();
                }
            });
        });

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

        exitMenuItem.setOnAction(e -> primaryStage.close());

        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(menuBar);
        VBox contentLayout = new VBox(10, filterSortBox, titleField, descriptionField, categoryField, deadlinePicker, timeBox, priorityComboBox, buttonBox, listView);
        contentLayout.setPadding(new Insets(10));
        mainLayout.setCenter(contentLayout);

        mainScene = new Scene(mainLayout, 500, 600);
        mainScene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());
        primaryStage.setScene(mainScene);
        primaryStage.show();

        taskList.forEach(this::scheduleTaskReminder);
    }

    private void exportTasksToCalendar(String filePath) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        StringBuilder icsContent = new StringBuilder("BEGIN:VCALENDAR\nVERSION:2.0\n");

        for (Task task : taskList) {
            if (task.getDeadline() != null) {
                icsContent.append("BEGIN:VEVENT\n")
                        .append("UID:").append(task.getId()).append("\n")
                        .append("SUMMARY:").append(task.getTitle()).append("\n")
                        .append("DESCRIPTION:").append(task.getDescription()).append("\n")
                        .append("DTSTART:").append(task.getDeadline().format(dateFormatter)).append("\n")
                        .append("PRIORITY:").append(task.getPriority().ordinal() + 1).append("\n")
                        .append("END:VEVENT\n");
            }
        }
        icsContent.append("END:VCALENDAR");

        try {
            Files.write(Paths.get(filePath), icsContent.toString().getBytes());
        } catch (IOException e) {
            Notifications.create().title("Error").text("Failed to export to calendar").showError();
            e.printStackTrace();
        }
    }

    private void createOrUpdateTask(TextField titleField, TextArea descriptionField, TextField categoryField, DatePicker deadlinePicker,
                                    Spinner<Integer> hourSpinner, Spinner<Integer> minuteSpinner, ComboBox<String> amPmComboBox,
                                    ComboBox<TaskPriority> priorityComboBox, boolean isEdit) {
        String title = titleField.getText();
        String description = descriptionField.getText();
        String category = categoryField.getText();
        LocalDate date = deadlinePicker.getValue();
        int hour = hourSpinner.getValue();
        int minute = minuteSpinner.getValue();
        String amPm = amPmComboBox.getValue();

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
            selectedTask.setCategory(category);
        } else {
            Task newTask = new Task(taskList.size() + 1, title, description, deadline, priority, category);
            taskManager.addTask(newTask);
            scheduleTaskReminder(newTask);
        }

        taskManager.saveTasksToFile();
        taskList.setAll(taskManager.getAllTasks());
        applyFilterAndSort(null, null, null);
    }

    private void scheduleTaskReminder(Task task) {
        Runnable existingReminder = taskReminderMap.remove(task);
        if (existingReminder != null) {
            scheduler.shutdownNow();
            scheduler = Executors.newScheduledThreadPool(1);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime taskDeadline = task.getDeadline();
        if (taskDeadline.isAfter(now)) {
            long delay = Duration.between(now, taskDeadline.minusMinutes(10)).toMillis();

            Runnable reminderTask = () -> Notifications.create()
                    .title("Reminder")
                    .text("Task '" + task.getTitle() + "' is due in 10 minutes!")
                    .showInformation();

            scheduler.schedule(reminderTask, delay, TimeUnit.MILLISECONDS);
            taskReminderMap.put(task, reminderTask);
        }
    }
    // Method to send tasks by email
    public void sendTasksByEmail(List<Task> tasks, String recipientEmail) {
        Properties prop = new Properties();
        prop.put("mail.smtp.host", SMTP_SERVER);
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Your Task List");

            // Build the email content
            StringBuilder emailContent = new StringBuilder("Here are your tasks:\n\n");
            for (Task task : tasks) {
                emailContent.append("Title: ").append(task.getTitle()).append("\n");
                emailContent.append("Description: ").append(task.getDescription()).append("\n");
                emailContent.append("Deadline: ").append(task.getDeadline()).append("\n");
                emailContent.append("Priority: ").append(task.getPriority()).append("\n");
                emailContent.append("Status: ").append(task.isCompleted() ? "Completed" : "Incomplete").append("\n\n");
            }

            message.setText(emailContent.toString());
            Transport.send(message);
            System.out.println("Email sent successfully!");

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    private void populateCategoryFilter(ComboBox<String> categoryFilterDropdown) {
        Set<String> categories = taskList.stream().map(Task::getCategory).collect(Collectors.toSet());
        categoryFilterDropdown.getItems().setAll("All Categories");
        categoryFilterDropdown.getItems().addAll(categories);
    }

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
                    comparator = Comparator.comparing(Task::getId);
                    break;
                default:
                    comparator = Comparator.comparing(Task::getDeadline);
                    break;
            }
        }

        filteredTaskList.setAll(taskList.stream().filter(filterCondition).sorted(comparator).collect(Collectors.toList()));
    }

    private void backupTaskList(Stack<List<Task>> stack) {
        List<Task> currentState = taskList.stream()
                .map(task -> new Task(task.getId(), task.getTitle(), task.getDescription(), task.getDeadline(), task.getPriority(), task.getCategory()))
                .collect(Collectors.toList());
        stack.push(currentState);
    }

    private boolean promptForPin() {
        TextInputDialog pinDialog = new TextInputDialog();
        pinDialog.setTitle("Enter PIN");
        pinDialog.setHeaderText("PIN Protection Enabled");
        pinDialog.setContentText("Please enter your PIN:");

        Optional<String> result = pinDialog.showAndWait();
        return result.isPresent() && result.get().equals(userPin);
    }

    private void loadConfig() {
        if (Files.exists(configFile)) {
            try (BufferedReader reader = Files.newBufferedReader(configFile)) {
                pinProtectionEnabled = Boolean.parseBoolean(reader.readLine());
                userPin = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void togglePinProtection() {
        TextInputDialog pinDialog = new TextInputDialog();
        pinDialog.setTitle("Set PIN");
        pinDialog.setHeaderText("Enter a new PIN");
        pinDialog.setContentText("PIN:");

        Optional<String> result = pinDialog.showAndWait();
        if (result.isPresent()) {
            userPin = result.get();
            pinProtectionEnabled = !pinProtectionEnabled;
            saveConfig();
            Notifications.create().title("PIN Protection")
                    .text(pinProtectionEnabled ? "PIN Protection Enabled" : "PIN Protection Disabled")
                    .showInformation();
        }
    }

    private void saveConfig() {
        try (BufferedWriter writer = Files.newBufferedWriter(configFile)) {
            writer.write(String.valueOf(pinProtectionEnabled));
            writer.newLine();
            writer.write(userPin);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
