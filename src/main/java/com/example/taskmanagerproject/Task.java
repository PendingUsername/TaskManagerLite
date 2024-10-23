package com.example.taskmanagerproject;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Task implements Serializable {
    private static final long serialVersionUID = 1L;  // Version 1 for serialization

    private int id;
    private String title;
    private String description;
    private boolean isCompleted;
    private LocalDateTime deadline;
    private TaskPriority priority;
    private LocalDateTime creationDate;  // To store when the task is created
    private String category;  // New field to store category

    // Constructor
    public Task(int id, String title, String description, LocalDateTime deadline, TaskPriority priority, String category) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.isCompleted = false;
        this.deadline = deadline;
        this.priority = priority;
        this.category = category;  // Initialize category
        this.creationDate = LocalDateTime.now();  // Set creation date
    }

    // Getters and setters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { this.isCompleted = completed; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }

    // New category getter and setter
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    // New getter for creation date
    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    // Check if the deadline has passed
    public boolean hasDeadlinePassed() {
        return LocalDateTime.now().isAfter(deadline);
    }

    @Override
    public String toString() {
        return "Task ID: " + id + "\n" +
                "Title: " + title + "\n" +
                "Description: " + description + "\n" +
                "Category: " + category + "\n" +  // Include category
                "Deadline: " + (deadline != null ? deadline : "No deadline") + "\n" +
                "Priority: " + (priority != null ? priority : "None") + "\n" +
                "Creation Date: " + creationDate + "\n" +
                "Completed: " + (isCompleted ? "Yes" : "No");
    }
}
