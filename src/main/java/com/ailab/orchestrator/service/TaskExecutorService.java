package com.ailab.orchestrator.service;

import com.ailab.orchestrator.model.TaskEntity;
import com.ailab.orchestrator.model.TaskStatus;
import com.ailab.orchestrator.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.Semaphore;

@Service
public class TaskExecutorService {

    private final TaskRepository taskRepo;
    private final int maxRetries;
    private final long baseBackoffMillis;
    private final Random random = new Random();

    public TaskExecutorService(
            TaskRepository taskRepo,
            @Value("${orchestrator.max-retries:3}") int maxRetries,
            @Value("${orchestrator.base-backoff-ms:1000}") long baseBackoffMillis) {
        this.taskRepo = taskRepo;
        this.maxRetries = maxRetries;
        this.baseBackoffMillis = baseBackoffMillis;
    }

    @Async
    public void executeAsync(TaskEntity task, Semaphore semaphore) {
        try {
            boolean success = simulateWorkload(task.getTaskType());

            TaskEntity current = taskRepo.findById(task.getId()).orElse(null);
            if (current == null || current.getStatus() == TaskStatus.CANCELLED) {
                return;
            }

            if (success) {
                current.setStatus(TaskStatus.SUCCEEDED);
                current.setNextRetryAt(null);
            } else {
                if (current.getAttempts() >= maxRetries) {
                    current.setStatus(TaskStatus.FAILED);
                } else {
                    long delay = baseBackoffMillis * (1L << (current.getAttempts() - 1));
                    current.setStatus(TaskStatus.PENDING);
                    current.setNextRetryAt(Instant.now().plusMillis(delay));
                }
            }
            taskRepo.save(current);
        } finally {
            semaphore.release();
        }
    }

    private boolean simulateWorkload(String type) {
        try {
            long duration = switch (type) {
                case "EXTRACT_PAGES" -> 800;
                case "OCR_TEXT" -> 1500;
                case "GENERATE_THUMBNAILS" -> 600;
                case "SUMMARIZE_LLM" -> 1200;
                default -> 500;
            };
            double failureRate = switch (type) {
                case "OCR_TEXT" -> 0.20;
                case "SUMMARIZE_LLM" -> 0.15;
                default -> 0.05;
            };

            Thread.sleep(duration);
            return random.nextDouble() >= failureRate;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}