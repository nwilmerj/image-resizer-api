package com.newsnow.imageapi.application.dto;

import lombok.Data; // Combina @Getter, @Setter, @ToString, @EqualsAndHashCode, @RequiredArgsConstructor
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.OffsetDateTime; // Mejor tipo para timestamps con zona horaria
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private UUID taskId;
    private OffsetDateTime timestamp;
    private String originalMD5;
    private String resolution;
    private String imageUrl;
}