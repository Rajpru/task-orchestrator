package com.ailab.orchestrator.service;

import com.ailab.orchestrator.model.dto.SubmitRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DagValidatorService {

    public void validateDag(List<SubmitRequest.TaskItem> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            throw new IllegalArgumentException("Task list cannot be empty");
        }

        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adj = new HashMap<>();
        Set<String> allIds = new HashSet<>();

        for (SubmitRequest.TaskItem t : tasks) {
            allIds.add(t.getId());
        }

        for (SubmitRequest.TaskItem t : tasks) {
            inDegree.put(t.getId(), t.getDependencies() != null ? t.getDependencies().size() : 0);
            if (t.getDependencies() != null) {
                for (String dep : t.getDependencies()) {
                    if (!allIds.contains(dep)) {
                        throw new IllegalArgumentException("Task " + t.getId() + " depends on non-existent task: " + dep);
                    }
                    adj.computeIfAbsent(dep, k -> new ArrayList<>()).add(t.getId());
                }
            }
        }

        Queue<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        int visited = 0;
        while (!queue.isEmpty()) {
            String curr = queue.poll();
            visited++;

            List<String> neighbors = adj.getOrDefault(curr, Collections.emptyList());
            for (String neighbor : neighbors) {
                int updated = inDegree.get(neighbor) - 1;
                inDegree.put(neighbor, updated);
                if (updated == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (visited != tasks.size()) {
            throw new IllegalArgumentException("Circular dependency detected across submitted tasks.");
        }
    }
}