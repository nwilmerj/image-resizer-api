package com.newsnow.imageapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.util.Iterator;

@SpringBootApplication
public class ImageResizerApiApplication {

	public static void main(String[] args) {

		// --- CÓDIGO DE VERIFICACIÓN DE IMAGEIO ---
		System.out.println("--- Verificando ImageIO Readers ---");
		checkImageReader("PNG");
		checkImageReader("JPEG");
		checkImageReader("JPG"); // A veces se registra como JPG
		checkImageReader("GIF");
		checkImageReader("BMP");
		checkImageReader("WEBP"); // Si añadiste el plugin webp
		checkImageReader("TIFF"); // Si añadiste el plugin tiff
		System.out.println("----------------------------------");

		// Iniciar la aplicación Spring Boot
		SpringApplication.run(ImageResizerApiApplication.class, args);
	}

	private static void checkImageReader(String formatName) {
		Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(formatName);
		if (!readers.hasNext()) {
			System.out.println("⚠️ NO hay reader para " + formatName.toUpperCase());
		} else {
			while (readers.hasNext()) {
				ImageReader reader = readers.next();
				System.out.println("✅ Reader " + formatName.toUpperCase() + " encontrado: " + reader.getClass().getName());
				System.out.println("   Originating Provider: " + reader.getOriginatingProvider().getClass().getName());
			}
		}
	}

}