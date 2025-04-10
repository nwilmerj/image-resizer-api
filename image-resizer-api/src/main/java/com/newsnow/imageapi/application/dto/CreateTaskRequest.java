package com.newsnow.imageapi.application.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {
    private byte[] imageBytes;
    private String originalFilename;
    private long imageContentLength;
    private int targetWidth;
    private int targetHeight;
}