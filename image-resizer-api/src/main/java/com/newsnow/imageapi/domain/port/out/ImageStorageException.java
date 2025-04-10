package com.newsnow.imageapi.domain.port.out;

public class ImageStorageException extends RuntimeException {
    public ImageStorageException(String message, Throwable cause) {
        super(message, cause);
    }
    public ImageStorageException(String message) {
        super(message);
    }
}