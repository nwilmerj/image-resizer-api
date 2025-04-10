package com.newsnow.imageapi.domain.port.out;

import com.newsnow.imageapi.domain.model.Task;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository {
    void save(Task task);
    Optional<Task> findById(UUID taskId);
}