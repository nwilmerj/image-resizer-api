package com.newsnow.imageapi.application.port.in;

import com.newsnow.imageapi.application.dto.CreateTaskRequest;
import com.newsnow.imageapi.application.dto.TaskResponse; // Usamos el DTO de respuesta
import com.newsnow.imageapi.domain.model.Task; // Puede devolver la entidad o un DTO
import java.util.Optional;
import java.util.UUID;

public interface TaskUseCase {

    /**
     * Caso de uso para crear una nueva tarea de redimensionamiento.
     * Procesa la imagen, la almacena y guarda los metadatos.
     *
     * @param request DTO con los datos de la imagen y dimensiones.
     * @return TaskResponse DTO con los detalles de la tarea creada.
     * @throws com.newsnow.imageapi.domain.port.out.ImageProcessingException Si falla el redimensionamiento.
     * @throws com.newsnow.imageapi.domain.port.out.ImageStorageException Si falla el almacenamiento.
     * @throws IllegalArgumentException Si los datos de entrada son inválidos.
     */
    TaskResponse createTask(CreateTaskRequest request);

    /**
     * Caso de uso para obtener los detalles de una tarea existente.
     *
     * @param taskId El ID único de la tarea.
     * @return Optional<TaskResponse> Contiene el DTO de la tarea si se encuentra.
     */
    Optional<TaskResponse> getTaskById(UUID taskId);
}