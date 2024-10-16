package com.example.taskmanagerproject;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Task implements Serializable {
    private int id;
    private String title;
    private String description;
    private boolean isCompleted;
    private LocalDateTime deadline;  // Use LocalDateTime for both date and time
    private TaskPriority priority;

    // Constructor
    public Task(int id, String title, String description, LocalDateTime deadline, TaskPriority priority) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.isCompleted = false;
        this.deadline = deadline;
        this.priority = priority;
    }

    // Getters and setters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { this.isCompleted = completed; }
    public LocalDateTime getDeadline() { return deadline; }  // Use LocalDateTime
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }

    // Check if the deadline has passed
    public boolean hasDeadlinePassed() {
        return LocalDateTime.now().isAfter(deadline);
    }

    @Override
    public String toString() {
        return "Task ID: " + id + ", Title: " + title + ", Description: " + description +
                ", Completed: " + (isCompleted ? "Yes" : "No") +
                ", Deadline: " + deadline.toString() +  // Convert LocalDateTime to string
                ", Priority: " + (priority != null ? priority : "None");
    }
}
