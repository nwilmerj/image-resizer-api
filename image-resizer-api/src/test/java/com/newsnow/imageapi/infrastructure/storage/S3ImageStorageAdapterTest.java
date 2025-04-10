package com.newsnow.imageapi.infrastructure.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse; // Mockear respuesta

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class S3ImageStorageAdapterTest {

    @Mock
    private S3Client s3ClientMock; // Mockear cliente S3

    // Inyectamos manualmente el mock
    private S3ImageStorageAdapter adapter;

    // Valores de prueba para las propiedades inyectadas por @Value
    private final String testBucketName = "test-bucket";
    private final String testCloudfrontDomain = "d12345test.cloudfront.net";


    @Captor
    private ArgumentCaptor<PutObjectRequest> putRequestCaptor;
    @Captor
    private ArgumentCaptor<RequestBody> requestBodyCaptor;

    private InputStream testInputStream;
    private byte[] testData;
    private String testFilename;
    private long testContentLength;

    @BeforeEach
    void setUp() {
        // Instanciar el adapter con el mock y valores de prueba
        adapter = new S3ImageStorageAdapter(s3ClientMock, testBucketName, testCloudfrontDomain);

        testData = "test-content".getBytes();
        testInputStream = new ByteArrayInputStream(testData);
        testFilename = "test-" + UUID.randomUUID() + ".jpg";
        testContentLength = testData.length;
    }

    @Test
    @DisplayName("✅ S3 Adapter saveImage(): Debería llamar a putObject y construir URL de CloudFront")
    void saveImageShouldCallPutObjectAndBuildCloudfrontUrl() { // Actualizar nombre
        // Arrange
        PutObjectResponse mockResponse = PutObjectResponse.builder().eTag("test-etag").build();
        when(s3ClientMock.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(mockResponse);

        // Act
        String resultUrl = adapter.saveImage(testInputStream, testFilename, testContentLength);

        // Assert
        // 1. Verificar llamada a s3Client.putObject
        verify(s3ClientMock).putObject(putRequestCaptor.capture(), requestBodyCaptor.capture());

        // 2. Verificar PutObjectRequest
        PutObjectRequest actualRequest = putRequestCaptor.getValue();
        assertThat(actualRequest.bucket()).isEqualTo(testBucketName);
        assertThat(actualRequest.key()).isEqualTo("processed/" + testFilename);
        // Podríamos verificar ContentType, ACL si los configuráramos

        // 3. Verificar RequestBody (más difícil de verificar contenido exacto sin leerlo)
        RequestBody actualBody = requestBodyCaptor.getValue();
        assertThat(actualBody.contentLength()).isEqualTo(testContentLength);


        // 4. Verificar URL devuelta (FORMATO CLOUDFRONT)
        String expectedUrl = String.format("https://%s/processed/%s", testCloudfrontDomain, testFilename); // Usar dominio CF de prueba
        assertThat(resultUrl).isEqualTo(expectedUrl);
    }
}