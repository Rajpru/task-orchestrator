package com.ailab.orchestrator.service;

import com.ailab.orchestrator.model.TaskEntity;
import com.ailab.orchestrator.model.TaskStatus;
import com.ailab.orchestrator.repository.TaskRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;

@Service
public class TaskOrchestratorService {

    private final TaskRepository taskRepo;
    private final TaskExecutorService executorService;
    private final Semaphore concurrencyLimiter;

    public TaskOrchestratorService(
            TaskRepository taskRepo,
            TaskExecutorService executorService,
            @Value("${orchestrator.concurrency:3}") int maxConcurrency) {
        this.taskRepo = taskRepo;
        this.executorService = executorService;
        this.concurrencyLimiter = new Semaphore(maxConcurrency);
    }

    @PostConstruct
    @Transactional
    public void onStartup() {
        taskRepo.resetRunningTasksOnStartup();
    }

    @Scheduled(fixedDelay = 200)
    public void pollAndDispatch() {
        propagateBlockedState();

        int availableSlots = concurrencyLimiter.availablePermits();
        if (availableSlots <= 0) return;

        Set<String> succeededIds = new HashSet<>(taskRepo.findAllSucceededTaskIds());
        List<TaskEntity> candidates = taskRepo.findCandidatesToRun(Instant.now());

        for (TaskEntity task : candidates) {
            if (!concurrencyLimiter.tryAcquire()) {
                break;
            }

            if (succeededIds.containsAll(task.getDependencies())) {
                markRunningAndDispatch(task.getId());
            } else {
                concurrencyLimiter.release();
            }
        }
    }

    @Transactional
    public void markRunningAndDispatch(String taskId) {
        TaskEntity task = taskRepo.findById(taskId).orElse(null);
        if (task == null || task.getStatus() != TaskStatus.PENDING) {
            concurrencyLimiter.release();
            return;
        }

        task.setStatus(TaskStatus.RUNNING);
        task.setAttempts(task.getAttempts() + 1);
        taskRepo.save(task);

        executorService.executeAsync(task, concurrencyLimiter);
    }

    @Transactional
    public void propagateBlockedState() {
        Set<String> badIds = new HashSet<>(taskRepo.findAllFailedOrBlockedTaskIds());
        if (badIds.isEmpty()) return;

        List<TaskEntity> pendingTasks = taskRepo.findAll();
        for (TaskEntity task : pendingTasks) {
            if (task.getStatus() == TaskStatus.PENDING) {
                for (String dep : task.getDependencies()) {
                    if (badIds.contains(dep)) {
                        task.setStatus(TaskStatus.BLOCKED);
                        taskRepo.save(task);
                        break;
                    }
                }
            }
        }
    }

    @Transactional
    public boolean cancelTask(String taskId) {
        TaskEntity task = taskRepo.findById(taskId).orElse(null);
        if (task == null || task.getStatus() == TaskStatus.SUCCEEDED || task.getStatus() == TaskStatus.FAILED) {
            return false;
        }
        task.setStatus(TaskStatus.CANCELLED);
        taskRepo.save(task);
        propagateBlockedState();
        return true;
    }
}