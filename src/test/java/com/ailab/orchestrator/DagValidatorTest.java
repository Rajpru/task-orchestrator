package com.ailab.orchestrator;

import com.ailab.orchestrator.model.dto.SubmitRequest;
import com.ailab.orchestrator.service.DagValidatorService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DagValidatorTest {

    private final DagValidatorService validator = new DagValidatorService();

    @Test
    void shouldPassValidDag() {
        SubmitRequest.TaskItem t1 = new SubmitRequest.TaskItem();
        t1.setId("task-1");
        t1.setTaskType("EXTRACT_PAGES");

        SubmitRequest.TaskItem t2 = new SubmitRequest.TaskItem();
        t2.setId("task-2");
        t2.setTaskType("OCR_TEXT");
        t2.setDependencies(Set.of("task-1"));

        assertDoesNotThrow(() -> validator.validateDag(List.of(t1, t2)));
    }

    @Test
    void shouldRejectCircularDependencies() {
        SubmitRequest.TaskItem t1 = new SubmitRequest.TaskItem();
        t1.setId("task-A");
        t1.setTaskType("EXTRACT_PAGES");
        t1.setDependencies(Set.of("task-B"));

        SubmitRequest.TaskItem t2 = new SubmitRequest.TaskItem();
        t2.setId("task-B");
        t2.setTaskType("OCR_TEXT");
        t2.setDependencies(Set.of("task-A"));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateDag(List.of(t1, t2))
        );
        assertTrue(ex.getMessage().contains("Circular dependency detected"));
    }
}