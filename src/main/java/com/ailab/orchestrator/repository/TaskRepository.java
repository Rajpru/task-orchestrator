package com.ailab.orchestrator.repository;

import com.ailab.orchestrator.model.TaskEntity;
import com.ailab.orchestrator.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, String> {

    long countByStatus(TaskStatus status);

    @Query("SELECT t FROM TaskEntity t WHERE t.status = com.ailab.orchestrator.model.TaskStatus.PENDING " +
            "AND (t.nextRetryAt IS NULL OR t.nextRetryAt <= :now) " +
            "ORDER BY t.createdAt ASC")
    List<TaskEntity> findCandidatesToRun(@Param("now") Instant now);

    @Transactional
    @Modifying
    @Query("UPDATE TaskEntity t SET t.status = com.ailab.orchestrator.model.TaskStatus.PENDING WHERE t.status = com.ailab.orchestrator.model.TaskStatus.RUNNING")
    void resetRunningTasksOnStartup();

    @Query("SELECT t.id FROM TaskEntity t WHERE t.status = com.ailab.orchestrator.model.TaskStatus.SUCCEEDED")
    List<String> findAllSucceededTaskIds();

    @Query("SELECT t.id FROM TaskEntity t WHERE t.status IN (com.ailab.orchestrator.model.TaskStatus.FAILED, com.ailab.orchestrator.model.TaskStatus.BLOCKED, com.ailab.orchestrator.model.TaskStatus.CANCELLED)")
    List<String> findAllFailedOrBlockedTaskIds();
}