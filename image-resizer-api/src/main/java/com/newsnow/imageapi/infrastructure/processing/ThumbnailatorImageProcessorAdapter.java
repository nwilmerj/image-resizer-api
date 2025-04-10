package com.newsnow.imageapi.infrastructure.processing;

import com.newsnow.imageapi.domain.model.ImageResolution;
import com.newsnow.imageapi.domain.port.out.ImageProcessingException;
import com.newsnow.imageapi.domain.port.out.ImageProcessorPort;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
public class ThumbnailatorImageProcessorAdapter implements ImageProcessorPort {

    @Override
    public byte[] resizeImage(InputStream imageInputStream, ImageResolution targetResolution) throws ImageProcessingException {
        if (imageInputStream == null) {
            throw new ImageProcessingException("Input stream cannot be null.");
        }

        // Verificación
        try (InputStream managedInputStream = imageInputStream;
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // --- LOG DE DEPURACIÓN ---
            int availableBytes = managedInputStream.available(); // Llama sobre el stream gestionado
            System.out.println("DEBUG: Bytes disponibles en InputStream antes de Thumbnails: " + availableBytes);
            if (availableBytes <= 0) {
                System.err.println("ERROR: InputStream parece vacío antes de procesar!");
                throw new ImageProcessingException("Input stream provided is empty.");
            }
            // --- FIN LOG ---

            // Usa Thumbnailator para redimensionar
            Thumbnails.of(managedInputStream)
                    .size(targetResolution.getWidth(), targetResolution.getHeight())
                    .toOutputStream(outputStream);

            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new ImageProcessingException("Failed to resize image due to IO error: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ImageProcessingException("Invalid arguments for image processing: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Unexpected error during image processing: " + e.getMessage());
            e.printStackTrace();
            throw new ImageProcessingException("Unexpected error during image processing: " + e.getMessage(), e);
        }
    }
}