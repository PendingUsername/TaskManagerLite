package com.example.taskmanagerproject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TaskManager {
    private List<Task> tasks;
    private final String filePath = "tasks.dat";  // File to store tasks

    public TaskManager() {
        tasks = new ArrayList<>();
        loadTasksFromFile();  // Load tasks when TaskManager is created
    }

    public void addTask(Task task) {
        tasks.add(task);
        saveTasksToFile();  // Save tasks after adding
    }

    public List<Task> getAllTasks() {
        return tasks;
    }

    public void completeTask(int id) {
        for (Task task : tasks) {
            if (task.getId() == id) {
                task.setCompleted(true);
                saveTasksToFile();  // Save tasks after completion
                return;
            }
        }
    }

    public void deleteTask(int id) {
        tasks.removeIf(task -> task.getId() == id);
        saveTasksToFile();  // Save tasks after deletion
    }

    // Save tasks to a file
    public void saveTasksToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(tasks);
            System.out.println("Tasks saved to file.");  // Debug statement
        } catch (IOException e) {
            System.out.println("Error saving tasks: " + e.getMessage());
        }
    }

    // Load tasks from a file
    private void loadTasksFromFile() {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("No tasks file found.");  // Debug statement
            return;  // If file doesn't exist, nothing to load
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            tasks = (List<Task>) ois.readObject();
            System.out.println("Tasks loaded: " + tasks.size());  // Debug statement
            for (Task task : tasks) {
                System.out.println("Loaded Task: " + task);  // Print out loaded tasks
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error loading tasks: " + e.getMessage());
        }
    }
}
