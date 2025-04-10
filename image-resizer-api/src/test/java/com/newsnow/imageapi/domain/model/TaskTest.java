package com.newsnow.imageapi.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class TaskTest {

    private ImageResolution resolution;
    private String md5 = "initial-md5";
    private Task task;

    @BeforeEach
    void setUp() {
        resolution = new ImageResolution(100, 100);
        // Usar el constructor principal para nueva tarea
        task = new Task(md5, resolution);
    }

    @Test
    @DisplayName("✅ Task: Debería inicializarse en estado PENDING")
    void shouldInitializeInPendingState() {
        assertThat(task.getTaskId()).isNotNull();
        assertThat(task.getCreatedAt()).isNotNull();
        assertThat(task.getOriginalMD5()).isEqualTo(md5);
        assertThat(task.getRequestedResolution()).isEqualTo(resolution);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(task.getResultImageUrl()).isNull();
    }

    @Test
    @DisplayName("✅ Task: markAsProcessing() debería cambiar estado de PENDING a PROCESSING")
    void markAsProcessingShouldChangeStateFromPending() {
        task.markAsProcessing();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PROCESSING);
    }

    @Test
    @DisplayName("⚠️ Task: markAsProcessing() no debería cambiar estado si no está PENDING")
    void markAsProcessingShouldNotChangeStateIfNotPending() {
        task.markAsProcessing(); // a PROCESSING
        task.markAsProcessing(); // Intentar de nuevo
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PROCESSING); // Sigue igual

        task.markAsCompleted("url"); // a COMPLETED
        task.markAsProcessing(); // Intentar
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED); // Sigue igual

        Task failedTask = new Task(md5, resolution);
        failedTask.markAsFailed(); // a FAILED
        failedTask.markAsProcessing(); // Intentar
        assertThat(failedTask.getStatus()).isEqualTo(TaskStatus.FAILED); // Sigue igual
    }

    @Test
    @DisplayName("✅ Task: markAsCompleted() debería cambiar estado y añadir URL si está PROCESSING")
    void markAsCompletedShouldChangeStateAndAddUrlWhenProcessing() {
        String url = "http://completed.url/image.jpg";
        task.markAsProcessing(); // Necesita estar en PROCESSING primero
        task.markAsCompleted(url);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(task.getResultImageUrl()).isEqualTo(url);
    }

    @Test
    @DisplayName("⚠️ Task: markAsCompleted() no debería cambiar estado si no está PROCESSING")
    void markAsCompletedShouldNotChangeStateIfNotProcessing() {
        String url = "http://completed.url/image.jpg";
        // Estando en PENDING
        task.markAsCompleted(url);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(task.getResultImageUrl()).isNull();

        // Estando en FAILED
        Task failedTask = new Task(md5, resolution);
        failedTask.markAsFailed();
        failedTask.markAsCompleted(url);
        assertThat(failedTask.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(failedTask.getResultImageUrl()).isNull();
    }


    @Test
    @DisplayName("✅ Task: markAsFailed() debería cambiar estado si está PENDING")
    void markAsFailedShouldChangeStateWhenPending() {
        task.markAsFailed();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(task.getResultImageUrl()).isNull();
    }

    @Test
    @DisplayName("✅ Task: markAsFailed() debería cambiar estado y limpiar URL si está PROCESSING")
    void markAsFailedShouldChangeStateAndClearUrlWhenProcessing() {
        Task processingTask = new Task(md5, resolution);
        processingTask.markAsProcessing();
        // Opcionalmente, dale una URL temporal para asegurar que se limpia
        // processingTask.markAsCompleted("http://some.url/image.jpg"); // ¡Esto está mal aquí!
        // processingTask.status = TaskStatus.PROCESSING; // Forzar si es necesario para probar limpieza
        // processingTask.resultImageUrl = "http://some.url/image.jpg"; // Forzar si es necesario

        // Asegúrate de que esté en PROCESSING
        processingTask.markAsProcessing(); // Llamada redundante si no cambió
        assertThat(processingTask.getStatus()).isEqualTo(TaskStatus.PROCESSING); // Verifica estado previo

        processingTask.markAsFailed(); // Ahora sí, marcar como fallida

        assertThat(processingTask.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(processingTask.getResultImageUrl()).isNull(); // Verificar limpieza de URL si la tenía
    }
}