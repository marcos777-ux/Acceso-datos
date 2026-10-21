package com.makro.aad.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class Base64Example {

    public void processBase64(Path imagePath, Path restoredPath) {
        if (!Files.exists(imagePath)) {
            System.err.println("El fichero no existe: " + imagePath);
            return;
        }

        try {
            byte[] binaryData = Files.readAllBytes(imagePath);
            System.out.println("Tamaño original: " + binaryData.length + " bytes");

            // Codificar bytes a cadena Base64
            String base64String = Base64.getEncoder().encodeToString(binaryData);
            System.out.println("Tamaño en Base64: " + base64String.length() + " caracteres");

            // Decodificar Base64 de vuelta a bytes
            byte[] restoredData = Base64.getDecoder().decode(base64String);
            Files.write(restoredPath, restoredData);

            System.out.println("Fichero restaurado con éxito en: " + restoredPath);

        } catch (IOException e) {
            System.err.println("Error procesando Base64: " + e.getMessage());
        }
    }
}