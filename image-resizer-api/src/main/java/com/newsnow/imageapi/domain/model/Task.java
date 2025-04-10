package com.newsnow.imageapi.domain.model;

import lombok.Getter; // Usaremos solo Getters, la entidad se modifica a través de métodos o constructor
import lombok.NonNull;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter // Solo getters, la creación y modificación se controlan
public class Task {

    private final UUID taskId;
    private final OffsetDateTime createdAt;
    private final String originalMD5;
    private final ImageResolution requestedResolution;

    // Estado y resultado - pueden cambiar
    private TaskStatus status;
    private String resultImageUrl;

    // Constructor para crear una nueva tarea (estado inicial)
    public Task(@NonNull String originalMD5, @NonNull ImageResolution requestedResolution) {
        this.taskId = UUID.randomUUID();
        this.createdAt = OffsetDateTime.now();
        this.originalMD5 = originalMD5;
        this.requestedResolution = requestedResolution;
        this.status = TaskStatus.PENDING;
        this.resultImageUrl = null;
    }

    // Podríamos necesitar un constructor para reconstruir desde la persistencia
    public Task(UUID taskId, OffsetDateTime createdAt, String originalMD5, ImageResolution requestedResolution, TaskStatus status, String resultImageUrl) {
        this.taskId = taskId;
        this.createdAt = createdAt;
        this.originalMD5 = originalMD5;
        this.requestedResolution = requestedResolution;
        this.status = status;
        this.resultImageUrl = resultImageUrl;
    }


    // Métodos para cambiar el estado (ejemplos)
    public void markAsProcessing() {
        if (this.status == TaskStatus.PENDING) {
            this.status = TaskStatus.PROCESSING;
        } else {
            System.err.println("Cannot mark task " + taskId + " as processing. Current status: " + status);
        }
    }

    public void markAsCompleted(@NonNull String resultImageUrl) {
        if (this.status == TaskStatus.PROCESSING) {
            this.status = TaskStatus.COMPLETED;
            this.resultImageUrl = resultImageUrl;
        } else {
            System.err.println("Cannot mark task " + taskId + " as completed. Current status: " + status);
        }
    }

    public void markAsFailed() {
        if (this.status == TaskStatus.PROCESSING || this.status == TaskStatus.PENDING) {
            this.status = TaskStatus.FAILED;
            this.resultImageUrl = null;
        } else {
            System.err.println("Cannot mark task " + taskId + " as failed. Current status: " + status);
        }
    }
}