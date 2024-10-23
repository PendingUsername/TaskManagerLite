package com.example.taskmanagerproject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TaskManager {
    private List<Task> tasks;
    private static final String TASKS_FILE = "tasks.dat";

    public TaskManager() {
        tasks = new ArrayList<>();
        loadTasksFromFile();
    }

    // Get all tasks from the list
    public List<Task> getAllTasks() {
        return tasks;
    }

    // Set the entire task list (for undo/redo operations)
    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
        saveTasksToFile();
    }

    // Add a task and save to file
    public void addTask(Task task) {
        tasks.add(task);
        saveTasksToFile();
    }

    // Delete a specific task by ID
    public void deleteTask(int id) {
        tasks.removeIf(task -> task.getId() == id);
        saveTasksToFile();
    }

    // Delete all tasks
    public void deleteAllTasks() {
        tasks.clear();
        saveTasksToFile();
    }

    // Save tasks to the file
    public void saveTasksToFile() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(TASKS_FILE))) {
            out.writeObject(tasks);
        } catch (IOException e) {
            System.out.println("Error saving tasks: " + e.getMessage());
        }
    }

    // Load tasks from the file
    @SuppressWarnings("unchecked")
    public void loadTasksFromFile() {
        File file = new File(TASKS_FILE);
        if (file.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                tasks = (List<Task>) in.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Error loading tasks: " + e.getMessage());
            }
        }
    }

    // Export tasks to a TXT file
    public void exportTasksToTxt(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (Task task : tasks) {
                writer.write(task.toString());
                writer.write("--------------------------------------------\n");
            }
            System.out.println("Tasks exported to " + filename);
        } catch (IOException e) {
            System.out.println("Error exporting tasks to TXT: " + e.getMessage());
        }
    }

    // Export tasks to a CSV file
    public void exportTasksToCsv(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            // Write CSV header
            writer.write("Task ID,Title,Description,Category,Deadline,Priority,Completed\n");
            for (Task task : tasks) {
                writer.write(task.getId() + "," +
                        task.getTitle() + "," +
                        task.getDescription().replace(",", ";") + "," +
                        task.getCategory() + "," +
                        (task.getDeadline() != null ? task.getDeadline() : "No deadline") + "," +
                        task.getPriority() + "," +
                        (task.isCompleted() ? "Yes" : "No") + "\n");
            }
            System.out.println("Tasks exported to " + filename);
        } catch (IOException e) {
            System.out.println("Error exporting tasks to CSV: " + e.getMessage());
        }
    }
}
