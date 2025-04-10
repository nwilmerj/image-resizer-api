package com.newsnow.imageapi.domain.port.out;

import java.io.InputStream;

public interface ImageStoragePort {
    // Guarda la imagen y devuelve la URL pública o identificador de almacenamiento
    String saveImage(InputStream imageInputStream, String filename, long contentLength) throws ImageStorageException;
}