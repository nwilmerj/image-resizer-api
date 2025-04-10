package com.newsnow.imageapi.infrastructure.web.controller;

import com.newsnow.imageapi.application.dto.TaskResponse;
import com.newsnow.imageapi.application.port.in.TaskUseCase;
import com.newsnow.imageapi.domain.port.out.ImageProcessingException; // Importar para simular error
import com.newsnow.imageapi.infrastructure.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException; // Importar para mockear error
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private TaskUseCase taskUseCase;

    @InjectMocks
    private TaskController taskController;

    private MockMvc mockMvc;

    private UUID sampleTaskId;
    private TaskResponse sampleTaskResponse;
    private MockMultipartFile sampleMultipartFile;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(taskController)
                .setControllerAdvice(new GlobalExceptionHandler()) // Asegurarse que el Advice está aquí
                .build();

        sampleTaskId = UUID.randomUUID();
        sampleTaskResponse = new TaskResponse(
                sampleTaskId, OffsetDateTime.now(), "md5hash", "100x50", "http://cdn.example.com/img.jpg"
        );
        sampleMultipartFile = new MockMultipartFile(
                "file",
                "test-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "simulated image content".getBytes()
        );
    }

    @Test
    @DisplayName("✅ POST /v1/task: Debería devolver 201 Created con TaskResponse si la creación es exitosa")
    void shouldReturn201AndTaskResponseOnSuccessfulCreation() throws Exception {
        // Arrange
        when(taskUseCase.createTask(any(com.newsnow.imageapi.application.dto.CreateTaskRequest.class)))
                .thenReturn(sampleTaskResponse);

        // Act & Assert
        mockMvc.perform(multipart("/v1/task")
                        .file(sampleMultipartFile)
                        .param("width", "100")
                        .param("height", "50")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.taskId", is(sampleTaskId.toString())))
                .andExpect(jsonPath("$.originalMD5", is("md5hash")))
                .andExpect(jsonPath("$.resolution", is("100x50")))
                .andExpect(jsonPath("$.imageUrl", is("http://cdn.example.com/img.jpg")));

        verify(taskUseCase, times(1)).createTask(any(com.newsnow.imageapi.application.dto.CreateTaskRequest.class));
    }

    @Test
    @DisplayName("✅ POST /v1/task: Debería devolver 400 Bad Request si falta el archivo") // Corregido nombre
    void shouldReturn400WhenFileIsMissing() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/v1/task")
                        // No file
                        .param("width", "100")
                        .param("height", "50")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest()); // Spring maneja @RequestParam requerido faltante

        verify(taskUseCase, never()).createTask(any());
    }

    @Test
    @DisplayName("✅ POST /v1/task: Debería devolver 400 Bad Request si falta width o height") // Corregido nombre
    void shouldReturn400WhenDimensionIsMissing() throws Exception {
        // Falta height
        mockMvc.perform(multipart("/v1/task")
                        .file(sampleMultipartFile)
                        .param("width", "100")
                        // falta height
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        // Falta width
        mockMvc.perform(multipart("/v1/task")
                        .file(sampleMultipartFile)
                        // falta width
                        .param("height", "50")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        verify(taskUseCase, never()).createTask(any());
    }

    @Test
    @DisplayName("✅ POST /v1/task: Debería devolver 400 Bad Request si TaskUseCase lanza IllegalArgumentException") // Corregido nombre
    void shouldReturn400WhenUseCaseThrowsIllegalArgumentException() throws Exception {
        // Arrange
        String errorMessage = "Invalid dimensions provided."; // Mensaje esperado
        when(taskUseCase.createTask(any(com.newsnow.imageapi.application.dto.CreateTaskRequest.class)))
                .thenThrow(new IllegalArgumentException(errorMessage)); // Simular excepción del servicio

        // Act & Assert
        mockMvc.perform(multipart("/v1/task")
                        .file(sampleMultipartFile)
                        .param("width", "100")
                        .param("height", "50")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest()) // Esperar 400
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // Verificar el mensaje devuelto por GlobalExceptionHandler
                .andExpect(jsonPath("$.message", is(errorMessage)))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @DisplayName("✅ POST /v1/task: Debería devolver 500 Internal Server Error si TaskUseCase lanza ImageProcessingException") // Nuevo Test
    void shouldReturn500WhenUseCaseThrowsImageProcessingException() throws Exception {
        // Arrange
        String errorMessage = "Thumbnailator failed.";
        when(taskUseCase.createTask(any(com.newsnow.imageapi.application.dto.CreateTaskRequest.class)))
                .thenThrow(new ImageProcessingException(errorMessage)); // Simular excepción del puerto

        // Act & Assert
        mockMvc.perform(multipart("/v1/task")
                        .file(sampleMultipartFile)
                        .param("width", "100")
                        .param("height", "50")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError()) // Esperar 500
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // Verificar el mensaje devuelto por GlobalExceptionHandler
                .andExpect(jsonPath("$.message", is(errorMessage))) // O un mensaje genérico si lo prefieres
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Internal Server Error")));
    }


    @Test
    @DisplayName("✅ GET /v1/task/{taskId}: Debería devolver 200 OK con TaskResponse si la tarea existe")
    void shouldReturn200AndTaskResponseWhenTaskExists() throws Exception {
        // Arrange
        when(taskUseCase.getTaskById(sampleTaskId)).thenReturn(Optional.of(sampleTaskResponse));

        // Act & Assert
        mockMvc.perform(get("/v1/task/{taskId}", sampleTaskId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.taskId", is(sampleTaskId.toString())))
                .andExpect(jsonPath("$.imageUrl", is(sampleTaskResponse.getImageUrl())));

        verify(taskUseCase, times(1)).getTaskById(sampleTaskId);
    }

    @Test
    @DisplayName("✅ GET /v1/task/{taskId}: Debería devolver 404 Not Found si la tarea no existe") // Corregido nombre
    void shouldReturn404WhenTaskDoesNotExist() throws Exception {
        // Arrange
        when(taskUseCase.getTaskById(sampleTaskId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/v1/task/{taskId}", sampleTaskId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // Verificar status 404

        verify(taskUseCase, times(1)).getTaskById(sampleTaskId);
    }

    @Test
    @DisplayName("✅ GET /v1/task/{taskId}: Debería devolver 400 Bad Request si el formato de taskId es inválido") // Corregido nombre
    void shouldReturn400WhenTaskIdIsInvalidFormat() throws Exception {
        // Arrange
        String invalidTaskId = "esto-no-es-un-uuid";
        // Mensaje esperado (puede variar ligeramente según cómo se lance/capture la excepción)
        String expectedErrorMessage = "Invalid Task ID format. Please use UUID.";

        // Act & Assert
        mockMvc.perform(get("/v1/task/{taskId}", invalidTaskId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // Verifica el mensaje específico devuelto por GlobalExceptionHandler al capturar la excepción del controller
                .andExpect(jsonPath("$.message", containsString("Invalid Task ID format"))) // Usar containsString si no es exacto
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")));

        verify(taskUseCase, never()).getTaskById(any());
    }
}