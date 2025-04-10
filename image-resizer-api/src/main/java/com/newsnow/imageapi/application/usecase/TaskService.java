package com.newsnow.imageapi.application.usecase;

import com.newsnow.imageapi.application.dto.CreateTaskRequest;
import com.newsnow.imageapi.application.dto.TaskResponse;
import com.newsnow.imageapi.application.port.in.TaskUseCase;
import com.newsnow.imageapi.domain.model.ImageResolution;
import com.newsnow.imageapi.domain.model.Task;
import com.newsnow.imageapi.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class TaskService implements TaskUseCase {

    // --- Puertos de Salida (inyectados) ---
    private final TaskRepository taskRepository;
    private final ImageProcessorPort imageProcessorPort;
    private final ImageStoragePort imageStoragePort;

    @Override
    public TaskResponse createTask(CreateTaskRequest request) {
        if (request.getImageBytes() == null || request.getImageBytes().length == 0 || request.getTargetWidth() <= 0 || request.getTargetHeight() <= 0) {
            throw new IllegalArgumentException("Invalid input data (bytes or dimensions) for task creation.");
        }

        // 2. Calcular MD5 (¡Ahora es fácil con los bytes!)
        String originalMD5 = calculateMD5(request.getImageBytes());

        ImageResolution targetResolution = new ImageResolution(request.getTargetWidth(), request.getTargetHeight());

        // 3. Crear Entidad de Dominio
        Task task = new Task(originalMD5, targetResolution);
        task.markAsProcessing();
        taskRepository.save(task);

        String imageUrl = null;
        byte[] resizedImageBytes = null; // Necesitamos los bytes redimensionados para S3

        try {
            // 4. Procesar Imagen: Crea stream DESDE los bytes recibidos
            InputStream streamToProcess = new ByteArrayInputStream(request.getImageBytes());
            resizedImageBytes = imageProcessorPort.resizeImage(streamToProcess, targetResolution);

            // 5. Almacenar Imagen Procesada: Crea stream DESDE los bytes redimensionados
            String filename = task.getTaskId().toString() + getFileExtension(request.getOriginalFilename());
            InputStream streamToStore = new ByteArrayInputStream(resizedImageBytes);
            imageUrl = imageStoragePort.saveImage(streamToStore, filename, resizedImageBytes.length);

            // 6. Actualizar y Guardar Estado Final
            task.markAsCompleted(imageUrl);
            taskRepository.save(task);

        } catch (Exception e) {
            task.markAsFailed();
            taskRepository.save(task);
            throw e; // GlobalExceptionHandler lo maneja
        }
        return mapTaskToResponse(task);
    }

    @Override
    public Optional<TaskResponse> getTaskById(UUID taskId) {
        // 1. Buscar Tarea (usando el puerto)
        Optional<Task> taskOptional = taskRepository.findById(taskId);

        // 2. Mapear a DTO si existe
        return taskOptional.map(this::mapTaskToResponse); // Usa referencia a método para mapear
    }

    // Método helper para calcular MD5 desde byte[]
    private String calculateMD5(byte[] inputBytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(inputBytes);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        } catch (Exception e) {
            System.err.println("Warning: Could not calculate MD5 hash. " + e.getMessage());
            return "md5-calculation-failed";
        }
    }


    // Método helper para obtener extensión (simple)
    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return ".tmp"; // Extensión por defecto o manejar error
    }


    // Método helper para mapear la Entidad Task a TaskResponse DTO
    private TaskResponse mapTaskToResponse(Task task) {
        if (task == null) {
            return null;
        }
        return new TaskResponse(
                task.getTaskId(),
                task.getCreatedAt(),
                task.getOriginalMD5(),
                task.getRequestedResolution().toString(),
                task.getResultImageUrl()
        );
    }
}