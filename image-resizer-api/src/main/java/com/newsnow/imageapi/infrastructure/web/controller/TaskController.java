package com.newsnow.imageapi.infrastructure.web.controller;

import com.newsnow.imageapi.application.dto.CreateTaskRequest;
import com.newsnow.imageapi.application.dto.TaskResponse;
import com.newsnow.imageapi.application.port.in.TaskUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/v1/task")
@RequiredArgsConstructor
public class TaskController {
    private final TaskUseCase taskUseCase;
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskResponse> createTask(
            @RequestParam("file") MultipartFile file,
            @RequestParam("width") int width,
            @RequestParam("height") int height) throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty.");
        }

        // 1. Lee los bytes UNA VEZ
        byte[] imageBytes = file.getBytes();

        // 2. Crea el DTO con los bytes
        CreateTaskRequest requestDto = new CreateTaskRequest(
                imageBytes,
                file.getOriginalFilename(),
                imageBytes.length,
                width,
                height
        );

        // 3. Llama al caso de uso
        TaskResponse responseDto = taskUseCase.createTask(requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable String taskId) {

        UUID taskUuid;
        try {
            taskUuid = UUID.fromString(taskId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Task ID format. Please use UUID.");
        }

        // Llamamos al caso de uso. Dejamos que cualquier excepción inesperada
        Optional<TaskResponse> taskOptional = taskUseCase.getTaskById(taskUuid);

        // Devolvemos 200 OK con el DTO si se encuentra, o 404 Not Found si no.
        return taskOptional
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}