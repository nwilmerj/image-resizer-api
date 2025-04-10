package com.newsnow.imageapi.infrastructure.persistence;

import com.newsnow.imageapi.domain.model.ImageResolution;
import com.newsnow.imageapi.domain.model.Task;
import com.newsnow.imageapi.domain.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamoDbTaskRepositoryTest {

    @Mock
    private DynamoDbClient dynamoDbClientMock; // Mockear el cliente SDK

    // Usaremos la instancia real del Mapper, ya que es lógica interna que queremos probar indirectamente
    // No necesitamos @InjectMocks porque instanciamos el Repository manualmente con el mock
    private DynamoDbTaskRepository repository;

    // Necesitamos el nombre de la tabla
    private final String testTableName = "TestImageTasks";

    @Captor
    private ArgumentCaptor<PutItemRequest> putItemRequestCaptor;
    @Captor
    private ArgumentCaptor<GetItemRequest> getItemRequestCaptor;

    private Task testTask;
    private UUID testTaskId;

    @BeforeEach
    void setUp() {
        // Instanciar el repositorio con el cliente mockeado
        repository = new DynamoDbTaskRepository(dynamoDbClientMock, testTableName);

        testTaskId = UUID.randomUUID();
        ImageResolution resolution = new ImageResolution(300, 200);
        // Usar constructor completo para controlar todos los campos
        testTask = new Task(testTaskId, OffsetDateTime.now(), "md5-hash-test", resolution, TaskStatus.COMPLETED, "http://image.url/img.png");
    }

    @Test
    @DisplayName("✅ DynamoDB Adapter save(): Debería llamar a putItem con el mapeo correcto")
    void saveShouldCallPutItemWithCorrectMapping() {
        // Arrange (No necesita when para putItem que es void)

        // Act
        repository.save(testTask);

        // Assert
        verify(dynamoDbClientMock).putItem(putItemRequestCaptor.capture());
        PutItemRequest actualRequest = putItemRequestCaptor.getValue();

        assertThat(actualRequest.tableName()).isEqualTo(testTableName);
        Map<String, AttributeValue> item = actualRequest.item();

        // Verificar mapeo de campos clave
        assertThat(item.get("taskId").s()).isEqualTo(testTask.getTaskId().toString());
        assertThat(item.get("createdAt").s()).isEqualTo(testTask.getCreatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        assertThat(item.get("originalMD5").s()).isEqualTo(testTask.getOriginalMD5());
        assertThat(item.get("requestedWidth").n()).isEqualTo(String.valueOf(testTask.getRequestedResolution().getWidth()));
        assertThat(item.get("requestedHeight").n()).isEqualTo(String.valueOf(testTask.getRequestedResolution().getHeight()));
        assertThat(item.get("status").s()).isEqualTo(testTask.getStatus().name());
        assertThat(item.get("resultImageUrl").s()).isEqualTo(testTask.getResultImageUrl());
    }

    @Test
    @DisplayName("✅ DynamoDB Adapter findById(): Debería llamar a getItem y mapear la respuesta correctamente")
    void findByIdShouldCallGetItemAndMapResponse() {
        // Arrange
        // Simular una respuesta de DynamoDB
        Map<String, AttributeValue> mockItem = Map.of(
                "taskId", AttributeValue.builder().s(testTaskId.toString()).build(),
                "createdAt", AttributeValue.builder().s(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)).build(),
                "originalMD5", AttributeValue.builder().s("mock-md5").build(),
                "requestedWidth", AttributeValue.builder().n("400").build(),
                "requestedHeight", AttributeValue.builder().n("300").build(),
                "status", AttributeValue.builder().s(TaskStatus.PROCESSING.name()).build()
                // No incluimos resultImageUrl para probar ese caso
        );
        GetItemResponse mockResponse = GetItemResponse.builder().item(mockItem).build();
        when(dynamoDbClientMock.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);

        // Act
        Optional<Task> result = repository.findById(testTaskId);

        // Assert
        // Verificar la llamada a getItem
        verify(dynamoDbClientMock).getItem(getItemRequestCaptor.capture());
        GetItemRequest actualRequest = getItemRequestCaptor.getValue();
        assertThat(actualRequest.tableName()).isEqualTo(testTableName);
        assertThat(actualRequest.key().get("taskId").s()).isEqualTo(testTaskId.toString());

        // Verificar el resultado mapeado
        assertThat(result).isPresent();
        Task foundTask = result.get();
        assertThat(foundTask.getTaskId()).isEqualTo(testTaskId);
        assertThat(foundTask.getOriginalMD5()).isEqualTo("mock-md5");
        assertThat(foundTask.getStatus()).isEqualTo(TaskStatus.PROCESSING);
        assertThat(foundTask.getRequestedResolution().getWidth()).isEqualTo(400);
        assertThat(foundTask.getRequestedResolution().getHeight()).isEqualTo(300);
        assertThat(foundTask.getResultImageUrl()).isNull(); // Verificamos que maneja la ausencia
    }

    @Test
    @DisplayName("❓ DynamoDB Adapter findById(): Debería devolver Optional vacío si getItem no encuentra item")
    void findByIdShouldReturnEmptyWhenGetItemReturnsNoItem() {
        // Simular una respuesta sin item: simplemente no llames a .item()
        GetItemResponse mockResponse = GetItemResponse.builder()
                .build();
        when(dynamoDbClientMock.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);

        // Act
        Optional<Task> result = repository.findById(testTaskId);

        // Assert
        verify(dynamoDbClientMock).getItem(any(GetItemRequest.class));
        assertThat(result).isEmpty(); // Verifica que el Optional está vacío
    }
}