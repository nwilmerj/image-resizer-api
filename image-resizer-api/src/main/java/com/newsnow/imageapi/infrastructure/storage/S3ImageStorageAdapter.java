package com.newsnow.imageapi.infrastructure.storage;

import com.newsnow.imageapi.domain.port.out.ImageStorageException;
import com.newsnow.imageapi.domain.port.out.ImageStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;

@Component
public class S3ImageStorageAdapter implements ImageStoragePort {

    private final S3Client s3Client;
    private final String bucketName;
    private final String cloudfrontDomain;

    public S3ImageStorageAdapter(S3Client s3Client,
                                 @Value("${aws.s3.bucket-name}") String bucketName,
                                 @Value("${CLOUDFRONT_DOMAIN}") String cloudfrontDomain) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.cloudfrontDomain = cloudfrontDomain;
    }

    @Override
    public String saveImage(InputStream imageInputStream, String filename, long contentLength) throws ImageStorageException {
        String objectKey = "processed/" + filename;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            RequestBody requestBody = RequestBody.fromInputStream(imageInputStream, contentLength);
            s3Client.putObject(putObjectRequest, requestBody);
            return String.format("https://%s/%s", cloudfrontDomain, objectKey);
        } catch (S3Exception e) {
            System.err.println("S3 Error during image storage: " + e.awsErrorDetails().errorMessage());
            throw new ImageStorageException("Failed to store image in S3: " + e.awsErrorDetails().errorMessage(), e);
        } catch (SdkException e) {
            System.err.println("AWS SDK Error during image storage: " + e.getMessage());
            throw new ImageStorageException("AWS SDK error during image storage: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Unexpected error during image storage: " + e.getMessage());
            e.printStackTrace();
            throw new ImageStorageException("Unexpected error during image storage: " + e.getMessage(), e);
        } finally {
            try {
                if (imageInputStream != null) {
                    imageInputStream.close();
                }
            } catch (IOException e) {
                System.err.println("Warning: Failed to close input stream after S3 upload. " + e.getMessage());
            }
        }
    }
}