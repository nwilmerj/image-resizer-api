package com.newsnow.imageapi.application.usecase;

import com.newsnow.imageapi.application.dto.CreateTaskRequest;
import com.newsnow.imageapi.application.dto.TaskResponse;
import com.newsnow.imageapi.domain.model.ImageResolution;
import com.newsnow.imageapi.domain.model.Task;
import com.newsnow.imageapi.domain.model.TaskStatus;
import com.newsnow.imageapi.domain.port.out.ImageProcessingException;
import com.newsnow.imageapi.domain.port.out.ImageProcessorPort;
import com.newsnow.imageapi.domain.port.out.ImageStorageException;
import com.newsnow.imageapi.domain.port.out.ImageStoragePort;
import com.newsnow.imageapi.domain.port.out.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit; // Importar ChronoUnit
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ImageProcessorPort imageProcessorPort;
    @Mock
    private ImageStoragePort imageStoragePort;

    @Captor
    private ArgumentCaptor<Task> taskCaptor;

    @InjectMocks
    private TaskService taskService;

    private byte[] sampleImageBytes;
    private CreateTaskRequest validRequest;
    private ImageResolution targetResolution;
    private String sampleOriginalFilename = "test.jpg";
    private UUID sampleTaskId; // No inicializar aquí, se genera en Task o se define en el test
    private OffsetDateTime sampleTimestamp; // No inicializar aquí


    @BeforeEach
    void setUp() {
        sampleImageBytes = new byte[]{1, 2, 3};
        targetResolution = new ImageResolution(100, 50);
        validRequest = new CreateTaskRequest(
                sampleImageBytes,
                sampleOriginalFilename,
                sampleImageBytes.length,
                targetResolution.getWidth(),
                targetResolution.getHeight()
        );
    }

    @Test
    @DisplayName("✅ createTask: Debería crear tarea exitosamente con datos válidos")
    void shouldCreateTaskSuccessfullyWhenInputIsValid() throws Exception {
        // Arrange
        byte[] resizedBytes = new byte[]{4, 5, 6};
        String expectedImageUrl = "http://example.com/processed/image.jpg";

        when(imageProcessorPort.resizeImage(any(InputStream.class), eq(targetResolution)))
                .thenReturn(resizedBytes);
        when(imageStoragePort.saveImage(any(InputStream.class), anyString(), eq((long) resizedBytes.length)))
                .thenReturn(expectedImageUrl);

        // Act
        TaskResponse response = taskService.createTask(validRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTaskId()).isNotNull();
        assertThat(response.getTimestamp()).isCloseTo(OffsetDateTime.now(), within(2, ChronoUnit.SECONDS)); // Usar ChronoUnit
        assertThat(response.getOriginalMD5()).isNotEmpty().isNotEqualTo("md5-calculation-failed");
        assertThat(response.getResolution()).isEqualTo(targetResolution.toString());
        assertThat(response.getImageUrl()).isEqualTo(expectedImageUrl);

        // Verificar interacciones
        verify(imageProcessorPort, times(1)).resizeImage(any(InputStream.class), eq(targetResolution));
        verify(imageStoragePort, times(1)).saveImage(any(InputStream.class), endsWith(response.getTaskId().toString() + ".jpg"), eq((long) resizedBytes.length));
        verify(taskRepository, times(2)).save(taskCaptor.capture());

        // Verificar los estados guardados
        List<Task> savedTasks = taskCaptor.getAllValues();
        assertThat(savedTasks).hasSize(2);
        //Task firstSave = savedTasks.get(0);
        Task secondSave = savedTasks.get(1);

        // Verificar el estado de CADA tarea guardada
        assertThat(secondSave.getStatus()).isEqualTo(TaskStatus.COMPLETED);  // El segundo debe ser COMPLETED
        assertThat(secondSave.getResultImageUrl()).isEqualTo(expectedImageUrl);
        assertThat(secondSave.getTaskId()).isEqualTo(response.getTaskId());
    }


    @Test
    @DisplayName("✅ createTask: Debería lanzar IllegalArgumentException si los bytes son nulos o vacíos")
    void shouldThrowIllegalArgumentExceptionWhenBytesAreInvalid() {
        // Arrange
        CreateTaskRequest nullBytesRequest = new CreateTaskRequest(null, sampleOriginalFilename, 0, 100, 50);
        CreateTaskRequest emptyBytesRequest = new CreateTaskRequest(new byte[]{}, sampleOriginalFilename, 0, 100, 50);
        String expectedErrorMessage = "Invalid input data (bytes or dimensions) for task creation.";

        // Act & Assert
        assertThatThrownBy(() -> taskService.createTask(nullBytesRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedErrorMessage);

        assertThatThrownBy(() -> taskService.createTask(emptyBytesRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedErrorMessage);

        // No verifyNoInteractions
    }

    @Test
    @DisplayName("✅ createTask: Debería guardar tarea como FAILED si el procesamiento de imagen falla")
    void shouldSaveFolderAsFailedWhenImageProcessingFails() throws Exception {
        // Arrange
        ImageProcessingException processingException = new ImageProcessingException("Processing Error");
        when(imageProcessorPort.resizeImage(any(InputStream.class), any(ImageResolution.class)))
                .thenThrow(processingException);

        // Act & Assert Exception
        assertThatThrownBy(() -> taskService.createTask(validRequest))
                .isInstanceOf(ImageProcessingException.class)
                .isEqualTo(processingException);

        // Assert State After Exception
        // 1. Verifica que save() se llamó exactamente DOS veces
        verify(taskRepository, times(2)).save(taskCaptor.capture());

        // 2. Obtén los argumentos capturados
        List<Task> capturedTasks = taskCaptor.getAllValues();
        assertThat(capturedTasks).hasSize(2);

        // 3. Verifica el estado de CADA UNO
        Task secondSaveState = capturedTasks.get(1);

        assertThat(secondSaveState.getStatus()).isEqualTo(TaskStatus.FAILED);   // El segundo debe ser FAILED
        assertThat(secondSaveState.getResultImageUrl()).isNull(); // Asegurarse que no hay URL en el estado FAILED

        // Verificaciones adicionales de interacción
        verify(imageStoragePort, never()).saveImage(any(), any(), anyLong());
        verify(imageProcessorPort, times(1)).resizeImage(any(), any());
    }


    @Test
    @DisplayName("✅ createTask: Debería guardar tarea como FAILED si el almacenamiento de imagen falla")
    void shouldSaveFolderAsFailedWhenImageStorageFails() throws Exception {
        // Arrange
        byte[] resizedBytes = new byte[]{4, 5, 6};
        ImageStorageException storageException = new ImageStorageException("S3 Error");

        when(imageProcessorPort.resizeImage(any(InputStream.class), eq(targetResolution)))
                .thenReturn(resizedBytes); // Procesamiento OK
        when(imageStoragePort.saveImage(any(InputStream.class), anyString(), anyLong()))
                .thenThrow(storageException); // Falla el almacenamiento

        // Act & Assert Exception
        assertThatThrownBy(() -> taskService.createTask(validRequest))
                .isInstanceOf(ImageStorageException.class)
                .isEqualTo(storageException);

        // Assert State After Exception
        // 1. Verifica que save() se llamó exactamente DOS veces
        verify(taskRepository, times(2)).save(taskCaptor.capture());

        // 2. Obtén los argumentos capturados
        List<Task> capturedTasks = taskCaptor.getAllValues();
        assertThat(capturedTasks).hasSize(2);

        // 3. Verifica el estado de CADA UNO
        Task secondSaveState = capturedTasks.get(1);

        assertThat(secondSaveState.getStatus()).isEqualTo(TaskStatus.FAILED);   // El segundo debe ser FAILED
        assertThat(secondSaveState.getResultImageUrl()).isNull();

        // Verificaciones adicionales de interacción
        verify(imageProcessorPort, times(1)).resizeImage(any(), any());
        verify(imageStoragePort, times(1)).saveImage(any(), any(), anyLong());
    }

    @Test
    @DisplayName("✅ getTaskById: Debería devolver TaskResponse si la tarea existe")
    void shouldReturnTaskResponseWhenTaskExists() {
        // Arrange
        sampleTaskId = UUID.randomUUID();
        sampleTimestamp = OffsetDateTime.now();
        // Usar el constructor completo para tener todos los datos controlados
        Task existingTask = new Task(
                sampleTaskId, sampleTimestamp, "md5hash", targetResolution,
                TaskStatus.COMPLETED, "http://example.com/image.jpg"
        );
        when(taskRepository.findById(sampleTaskId)).thenReturn(Optional.of(existingTask));

        // Act
        Optional<TaskResponse> responseOptional = taskService.getTaskById(sampleTaskId);

        // Assert
        assertThat(responseOptional).isPresent();
        TaskResponse response = responseOptional.get();
        assertThat(response.getTaskId()).isEqualTo(sampleTaskId);
        assertThat(response.getTimestamp()).isEqualTo(sampleTimestamp);
        assertThat(response.getOriginalMD5()).isEqualTo("md5hash");
        assertThat(response.getResolution()).isEqualTo(targetResolution.toString());
        assertThat(response.getImageUrl()).isEqualTo("http://example.com/image.jpg");

        verify(taskRepository, times(1)).findById(sampleTaskId);
    }

    @Test
    @DisplayName("❓ getTaskById: Debería devolver Optional vacío si la tarea no existe")
    void shouldReturnEmptyOptionalWhenTaskDoesNotExist() {
        // Arrange
        sampleTaskId = UUID.randomUUID();
        when(taskRepository.findById(sampleTaskId)).thenReturn(Optional.empty());

        // Act
        Optional<TaskResponse> responseOptional = taskService.getTaskById(sampleTaskId);

        // Assert
        assertThat(responseOptional).isEmpty();
        verify(taskRepository, times(1)).findById(sampleTaskId);
    }
}