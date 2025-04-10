package com.newsnow.imageapi.domain.port.out;

import com.newsnow.imageapi.domain.model.ImageResolution;
import java.io.InputStream;

public interface ImageProcessorPort {
    // Devuelve los bytes de la imagen redimensionada
    byte[] resizeImage(InputStream imageInputStream, ImageResolution targetResolution) throws ImageProcessingException;
}