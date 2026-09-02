package com.ailab.orchestrator.model.dto;

import com.ailab.orchestrator.model.TaskStatus;

public class TaskResponse {
    private String id;
    private TaskStatus status;
    private int attempts;
    private String taskType;

    public TaskResponse(String id, TaskStatus status, int attempts, String taskType) {
        this.id = id;
        this.status = status;
        this.attempts = attempts;
        this.taskType = taskType;
    }

    public String getId() { return id; }
    public TaskStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public String getTaskType() { return taskType; }
}