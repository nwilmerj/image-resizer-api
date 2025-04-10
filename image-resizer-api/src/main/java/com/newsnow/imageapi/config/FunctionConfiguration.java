package com.newsnow.imageapi.config;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsnow.imageapi.application.dto.CreateTaskRequest;
import com.newsnow.imageapi.application.dto.ErrorResponse;
import com.newsnow.imageapi.application.dto.TaskResponse;
import com.newsnow.imageapi.application.port.in.TaskUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.io.IOException;
import java.time.OffsetDateTime; // Asegúrate de importar OffsetDateTime
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
public class FunctionConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FunctionConfiguration.class);

    private final TaskUseCase taskUseCase;
    private final ObjectMapper objectMapper;

    @Bean
    public Function<Message<APIGatewayProxyRequestEvent>, Message<APIGatewayProxyResponseEvent>> handleApiGatewayRequest() {
        return message -> {
            APIGatewayProxyRequestEvent request = message.getPayload();
            log.info("Received V1 Proxy request. Path: {}, Method: {}", request.getPath(), request.getHttpMethod());

            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withStatusCode(500); // Default code

            try {
                String httpMethod = request.getHttpMethod();
                String path = request.getPath();

                log.info("Handling V1 Proxy: Method={}, Path={}", httpMethod, path);

                // Enrutamiento
                if (HttpMethod.POST.name().equalsIgnoreCase(httpMethod) && "/v1/task".equals(path)) {
                    handlePostTaskV1(request, response);
                } else if (HttpMethod.GET.name().equalsIgnoreCase(httpMethod) && path != null && path.startsWith("/v1/task/")) {
                    handleGetTaskV1(request, response);
                } else {
                    setErrorResponseV1(response, 404, "Not Found", "No route found for " + httpMethod + " " + path, path);
                }
            } catch (Exception e) {
                log.error("Error processing V1 request: {}", e.getMessage(), e);
                setErrorResponseV1(response, 500, "Internal Server Error", "An unexpected error occurred: " + e.getMessage(),
                        request.getPath() != null ? request.getPath() : "Unknown");
            }

            return MessageBuilder.withPayload(response)
                    .copyHeadersIfAbsent(message.getHeaders())
                    .build();
        };
    }

    // Método para POST
    private void handlePostTaskV1(APIGatewayProxyRequestEvent request, APIGatewayProxyResponseEvent response) throws IOException {
        log.debug("Handling POST V1 request. Body received (Base64 encoded? {}): {}", request.getIsBase64Encoded(), request.getBody() != null ? "Present" : "Null");

        if (request.getBody() == null || request.getBody().isEmpty()) {
            log.warn("Received POST request with empty body.");
            setErrorResponseV1(response, 400, "Bad Request", "Missing request body.", "/v1/task");
            return;
        }

        try {
            // Parsear el JSON del body
            Map<String, Object> bodyMap = objectMapper.readValue(request.getBody(), Map.class);

            String imageDataBase64 = (String) bodyMap.get("imageData");
            String filename = (String) bodyMap.get("filename");
            int width = ((Number) bodyMap.getOrDefault("width", 100)).intValue();
            int height = ((Number) bodyMap.getOrDefault("height", 100)).intValue();

            if (imageDataBase64 == null || filename == null) {
                log.warn("Missing 'imageData' or 'filename' in JSON body.");
                setErrorResponseV1(response, 400, "Bad Request", "Missing 'imageData' or 'filename' in JSON body.", "/v1/task");
                return;
            }

            byte[] imageBytes = Base64.getDecoder().decode(imageDataBase64);
            log.info("Decoded {} bytes for image '{}'", imageBytes.length, filename);

            CreateTaskRequest serviceRequest = new CreateTaskRequest(
                    imageBytes,
                    filename,
                    imageBytes.length,
                    width,
                    height
            );

            // Llamar al caso de uso
            TaskResponse taskResponse = taskUseCase.createTask(serviceRequest);

            // Establecer respuesta exitosa
            response.setStatusCode(201); // Created
            response.setBody(objectMapper.writeValueAsString(taskResponse));

        } catch (JsonProcessingException | ClassCastException e) {
            log.error("Failed to parse request body JSON or cast values: {}", e.getMessage());
            setErrorResponseV1(response, 400, "Bad Request", "Invalid JSON format or data types in request body.", "/v1/task");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument during task creation: {}", e.getMessage());
            setErrorResponseV1(response, 400, "Bad Request", e.getMessage(), "/v1/task");
        } catch (Exception e) { // Capturar otros errores del use case
            log.error("Error in taskUseCase.createTask: {}", e.getMessage(), e);
            setErrorResponseV1(response, 500, "Internal Server Error", "Error creating task: " + e.getMessage(), "/v1/task");
        }
    }

    // Método adaptado para GET
    private void handleGetTaskV1(APIGatewayProxyRequestEvent request, APIGatewayProxyResponseEvent response) throws JsonProcessingException {
        String taskIdStr = null;
        if (request.getPathParameters() != null) {
            taskIdStr = request.getPathParameters().get("taskId");
        }
        log.info("Handling GET V1 request for taskId: {}", taskIdStr);

        if (taskIdStr == null) {
            log.warn("Task ID not found in path parameters.");
            setErrorResponseV1(response, 400, "Bad Request", "Missing Task ID in path.", request.getPath());
            return;
        }

        UUID taskId;
        try {
            taskId = UUID.fromString(taskIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid Task ID format received: {}", taskIdStr);
            setErrorResponseV1(response, 400, "Bad Request", "Invalid Task ID format.", request.getPath());
            return;
        }

        Optional<TaskResponse> taskOptional = taskUseCase.getTaskById(taskId); // Ahora taskId es visible

        if (taskOptional.isPresent()) {
            log.info("Task found for ID: {}", taskId);
            response.setStatusCode(200); // OK
            response.setBody(objectMapper.writeValueAsString(taskOptional.get()));
        } else {
            log.warn("Task not found for ID: {}", taskId);
            setErrorResponseV1(response, 404, "Not Found", "Task not found for ID: " + taskId, request.getPath());
        }
    }

    // Método helper para errores adaptado para V1 Response
    private void setErrorResponseV1(APIGatewayProxyResponseEvent response, int statusCode, String error, String message, String path) {
        response.setStatusCode(statusCode);
        response.setHeaders(Map.of("Content-Type", "application/json"));
        try {
            ErrorResponse errorDto = new ErrorResponse(OffsetDateTime.now(), statusCode, error, message, path);
            response.setBody(objectMapper.writeValueAsString(errorDto));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize error DTO: {}", e.getMessage());
            response.setBody("{\"error\":\"Failed to serialize error message\"}");
        }
    }
}