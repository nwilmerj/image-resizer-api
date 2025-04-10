package com.newsnow.imageapi.domain.model;

import lombok.Value;

@Value
public class ImageResolution {

    int width;
    int height;

    // El constructor es generado por @Value
    public ImageResolution(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and Height must be positive values.");
        }
        this.width = width;
        this.height = height;
    }

    @Override
    public String toString() {
        return width + "x" + height;
    }
}