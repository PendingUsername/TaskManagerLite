package com.example.taskmanagerproject;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Task implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String title;
    private String description;
    private boolean isCompleted;
    private LocalDateTime deadline;
    private TaskPriority priority;
    private LocalDateTime creationDate;
    private String category;

    // Constructor
    public Task(int id, String title, String description, LocalDateTime deadline, TaskPriority priority, String category) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.isCompleted = false;
        this.deadline = deadline;
        this.priority = priority;
        this.category = category;
        this.creationDate = LocalDateTime.now();
    }

    // Copy constructor
    public Task(Task other) {
        this.id = other.id;
        this.title = other.title;
        this.description = other.description;
        this.isCompleted = other.isCompleted;
        this.deadline = other.deadline;
        this.priority = other.priority;
        this.category = other.category;
        this.creationDate = other.creationDate;
    }

    // Getters and setters...
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
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDateTime getCreationDate() { return creationDate; }

    public boolean hasDeadlinePassed() {
        return LocalDateTime.now().isAfter(deadline);
    }

    @Override
    public String toString() {
        return "Task ID: " + id + "\n" +
                "Title: " + title + "\n" +
                "Description: " + description + "\n" +
                "Category: " + category + "\n" +
                "Deadline: " + (deadline != null ? deadline : "No deadline") + "\n" +
                "Priority: " + (priority != null ? priority : "None") + "\n" +
                "Creation Date: " + creationDate + "\n" +
                "Completed: " + (isCompleted ? "Yes" : "No");
    }
}
