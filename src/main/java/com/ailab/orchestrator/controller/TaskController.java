package com.ailab.orchestrator.controller;

import com.ailab.orchestrator.model.TaskEntity;
import com.ailab.orchestrator.model.TaskStatus;
import com.ailab.orchestrator.model.dto.StatsResponse;
import com.ailab.orchestrator.model.dto.SubmitRequest;
import com.ailab.orchestrator.model.dto.TaskResponse;
import com.ailab.orchestrator.repository.TaskRepository;
import com.ailab.orchestrator.service.DagValidatorService;
import com.ailab.orchestrator.service.TaskOrchestratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository taskRepo;
    private final DagValidatorService dagValidator;
    private final TaskOrchestratorService orchestrator;

    public TaskController(TaskRepository taskRepo, DagValidatorService dagValidator, TaskOrchestratorService orchestrator) {
        this.taskRepo = taskRepo;
        this.dagValidator = dagValidator;
        this.orchestrator = orchestrator;
    }

    @PostMapping
    public ResponseEntity<?> submitTasks(@RequestBody SubmitRequest request) {
        try {
            dagValidator.validateDag(request.getTasks());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }

        List<TaskEntity> entities = request.getTasks().stream()
                .map(t -> new TaskEntity(t.getId(), t.getTaskType(), t.getDependencies()))
                .collect(Collectors.toList());

        taskRepo.saveAll(entities);
        List<String> ids = entities.stream().map(TaskEntity::getId).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("submittedTaskIds", ids));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getStatus(@PathVariable String id) {
        return taskRepo.findById(id)
                .map(t -> ResponseEntity.ok(new TaskResponse(t.getId(), t.getStatus(), t.getAttempts(), t.getTaskType())))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelTask(@PathVariable String id) {
        boolean cancelled = orchestrator.cancelTask(id);
        if (!cancelled) {
            return ResponseEntity.badRequest().body(Map.of("error", "Task not found or in terminal state"));
        }
        return ResponseEntity.ok(Map.of("status", "CANCELLED", "id", id));
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats() {
        long running = taskRepo.countByStatus(TaskStatus.RUNNING);
        long waiting = taskRepo.countByStatus(TaskStatus.PENDING);
        long blocked = taskRepo.countByStatus(TaskStatus.BLOCKED);
        long succeeded = taskRepo.countByStatus(TaskStatus.SUCCEEDED);
        long failed = taskRepo.countByStatus(TaskStatus.FAILED);

        return ResponseEntity.ok(new StatsResponse(running, waiting, blocked, succeeded, failed));
    }
}