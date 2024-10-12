package com.example.taskmanagerproject;

import java.io.Serializable;
import java.time.LocalDate;

public class Task implements Serializable {
    private int id;
    private String title;
    private String description;
    private boolean isCompleted;
    private LocalDate deadline;
    private TaskPriority priority;  // Using TaskPriority enum

    public Task(int id, String title, String description, LocalDate deadline, TaskPriority priority) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.isCompleted = false;
        this.deadline = deadline;
        this.priority = this.priority;  // Initialize priority
    }

    // Getters and setters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { this.isCompleted = completed; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public TaskPriority getPriority() { return priority; }  // Getter for priority
    public void setPriority(TaskPriority priority) { this.priority = priority; }  // Setter for priority

    @Override
    public String toString() {
        return "Task ID: " + id + ", Title: " + title + ", Description: " + description +
                ", Completed: " + (isCompleted ? "Yes" : "No") +
                ", Deadline: " + (deadline != null ? deadline : "No deadline") +
                ", Priority: " + priority;  // Include priority in task description
    }
}
