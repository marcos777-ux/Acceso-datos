package com.makro.aad.file;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TextFileExample {

    public void processTextFile() {
        Path path = Paths.get("students.csv");

        try {
            String csvData = """
                ID,Name,Role
                1,Sophia,Developer
                2,Marcus,Project Manager
                """;

            Files.writeString(path, csvData, StandardCharsets.UTF_8);
            System.out.println("Fichero escrito correctamente: " + path.toAbsolutePath());

            String retrievedContent = Files.readString(path, StandardCharsets.UTF_8);
            System.out.println("--- Contenido recuperado ---");
            System.out.println(retrievedContent);

        } catch (IOException e) {
            System.err.println("Error al procesar el fichero de texto: " + e.getMessage());
        }
    }
}