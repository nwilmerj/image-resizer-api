package com.newsnow.imageapi.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ImageResolutionTest {

    @Test
    @DisplayName("✅ ImageResolution: Debería crearse correctamente con valores válidos")
    void shouldCreateSuccessfullyWithValidValues() {
        int width = 800;
        int height = 600;
        ImageResolution res = new ImageResolution(width, height);

        assertThat(res.getWidth()).isEqualTo(width);
        assertThat(res.getHeight()).isEqualTo(height);
        assertThat(res.toString()).isEqualTo("800x600");
    }

    @Test
    @DisplayName("❌ ImageResolution: Debería lanzar excepción si width es inválido")
    void shouldThrowExceptionForInvalidWidth() {
        assertThatThrownBy(() -> new ImageResolution(0, 600))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Width and Height must be positive");

        assertThatThrownBy(() -> new ImageResolution(-100, 600))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Width and Height must be positive");
    }

    @Test
    @DisplayName("❌ ImageResolution: Debería lanzar excepción si height es inválido")
    void shouldThrowExceptionForInvalidHeight() {
        assertThatThrownBy(() -> new ImageResolution(800, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Width and Height must be positive");

        assertThatThrownBy(() -> new ImageResolution(800, -100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Width and Height must be positive");
    }
}