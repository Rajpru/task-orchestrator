package com.ailab.orchestrator.model.dto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SubmitRequest {
    private List<TaskItem> tasks;

    public List<TaskItem> getTasks() { return tasks; }
    public void setTasks(List<TaskItem> tasks) { this.tasks = tasks; }

    public static class TaskItem {
        private String id;
        private String taskType;
        private Set<String> dependencies = new HashSet<>();

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTaskType() { return taskType; }
        public void setTaskType(String taskType) { this.taskType = taskType; }
        public Set<String> getDependencies() { return dependencies; }
        public void setDependencies(Set<String> dependencies) { this.dependencies = dependencies; }
    }
}