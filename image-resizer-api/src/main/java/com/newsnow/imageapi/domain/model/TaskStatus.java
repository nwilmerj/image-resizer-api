package com.newsnow.imageapi.domain.model;

public enum TaskStatus {
    PENDING,      // Recibida, esperando procesamiento
    PROCESSING,   // Procesamiento en curso
    COMPLETED,    // Procesamiento exitoso
    FAILED        // Procesamiento fallido
}