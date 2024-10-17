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

    // New method to delete all tasks
    public void deleteAllTasks() {
        tasks.clear();  // Remove all tasks from the list
        saveTasksToFile();  // Save the empty list to the file
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
}
