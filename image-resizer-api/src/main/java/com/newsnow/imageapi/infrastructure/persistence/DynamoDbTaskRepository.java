package com.newsnow.imageapi.infrastructure.persistence;

import com.newsnow.imageapi.domain.model.ImageResolution;
import com.newsnow.imageapi.domain.model.Task;
import com.newsnow.imageapi.domain.model.TaskStatus;
import com.newsnow.imageapi.domain.port.out.TaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DynamoDbTaskRepository implements TaskRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    // Mapeador reutilizable para convertir entre Task y Item de DynamoDB
    private static final TaskDynamoDbItemMapper MAPPER = new TaskDynamoDbItemMapper();

    public DynamoDbTaskRepository(DynamoDbClient dynamoDbClient,
                                  @Value("${aws.dynamodb.table-name}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    @Override
    public void save(Task task) {
        try {
            // Mapear la entidad Task a un Item de DynamoDB
            Map<String, AttributeValue> item = MAPPER.toItem(task);

            // Crear la solicitud PutItem
            PutItemRequest request = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build();

            // Ejecutar la operación
            dynamoDbClient.putItem(request);

        } catch (DynamoDbException e) {
            // Envolver excepciones de DynamoDB
            System.err.println("DynamoDB Error saving task " + task.getTaskId() + ": " + e.getMessage());
            throw new RuntimeException("Failed to save task to DynamoDB", e);
        }
    }

    @Override
    public Optional<Task> findById(UUID taskId) {
        try {
            // Crear la clave para la búsqueda
            Map<String, AttributeValue> keyToGet = Map.of(
                    MAPPER.ATTR_TASK_ID, AttributeValue.builder().s(taskId.toString()).build()
            );

            // Crear la solicitud GetItem
            GetItemRequest request = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(keyToGet)
                    .build();

            // Ejecutar la operación
            GetItemResponse response = dynamoDbClient.getItem(request);

            // Verificar si se encontró el item y mapearlo de vuelta a Task
            if (response.hasItem()) {
                Task task = MAPPER.fromItem(response.item());
                return Optional.of(task);
            } else {
                return Optional.empty(); // No se encontró la tarea
            }

        } catch (DynamoDbException e) {
            System.err.println("DynamoDB Error finding task " + taskId + ": " + e.getMessage());
            throw new RuntimeException("Failed to find task in DynamoDB", e);
        }
    }

    // --- Clase interna estática para el mapeo ---
    private static class TaskDynamoDbItemMapper {
        private static final String ATTR_TASK_ID = "taskId";
        private static final String ATTR_CREATED_AT = "createdAt";
        private static final String ATTR_ORIGINAL_MD5 = "originalMD5";
        private static final String ATTR_REQ_WIDTH = "requestedWidth";
        private static final String ATTR_REQ_HEIGHT = "requestedHeight";
        private static final String ATTR_STATUS = "status";
        private static final String ATTR_RESULT_URL = "resultImageUrl";

        // Formateador para timestamps ISO 8601
        private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        /** Mapea una entidad Task a un Map de AttributeValue para DynamoDB. */
        public Map<String, AttributeValue> toItem(Task task) {
            Map<String, AttributeValue> item = new java.util.HashMap<>();
            item.put(ATTR_TASK_ID, AttributeValue.builder().s(task.getTaskId().toString()).build());
            item.put(ATTR_CREATED_AT, AttributeValue.builder().s(task.getCreatedAt().format(ISO_FORMATTER)).build());
            item.put(ATTR_ORIGINAL_MD5, AttributeValue.builder().s(task.getOriginalMD5()).build());
            item.put(ATTR_REQ_WIDTH, AttributeValue.builder().n(String.valueOf(task.getRequestedResolution().getWidth())).build());
            item.put(ATTR_REQ_HEIGHT, AttributeValue.builder().n(String.valueOf(task.getRequestedResolution().getHeight())).build());
            item.put(ATTR_STATUS, AttributeValue.builder().s(task.getStatus().name()).build());

            // Solo añadir resultImageUrl si no es nulo
            if (task.getResultImageUrl() != null) {
                item.put(ATTR_RESULT_URL, AttributeValue.builder().s(task.getResultImageUrl()).build());
            }

            return item;
        }

        /** Mapea un Map de AttributeValue de DynamoDB a una entidad Task. */
        public Task fromItem(Map<String, AttributeValue> item) {
            UUID taskId = UUID.fromString(item.get(ATTR_TASK_ID).s());
            OffsetDateTime createdAt = OffsetDateTime.parse(item.get(ATTR_CREATED_AT).s(), ISO_FORMATTER);
            String originalMD5 = item.get(ATTR_ORIGINAL_MD5).s();
            int width = Integer.parseInt(item.get(ATTR_REQ_WIDTH).n());
            int height = Integer.parseInt(item.get(ATTR_REQ_HEIGHT).n());
            ImageResolution resolution = new ImageResolution(width, height);
            TaskStatus status = TaskStatus.valueOf(item.get(ATTR_STATUS).s());
            String resultUrl = item.containsKey(ATTR_RESULT_URL) ? item.get(ATTR_RESULT_URL).s() : null;

            // Usar el constructor adecuado de Task para reconstruir
            return new Task(taskId, createdAt, originalMD5, resolution, status, resultUrl);
        }
    }
}