package com.makro.aad.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

@Component
public class StoreIngestionScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(StoreIngestionScheduler.class);

    // Rutas de las carpetas
    private static final Path BASE =
            Path.of("C:/workspace/store_data");

    private static final Path INPUT = BASE.resolve("input");
    private static final Path OUTPUT = BASE.resolve("output");

    @Scheduled(fixedRate = 5000)
    public void processStoreBatches() {

        System.out.println("Buscando nuevos ficheros...");

        try {
            // Crear directorios si no existen
            Files.createDirectories(INPUT);
            Files.createDirectories(OUTPUT);
            Files.createDirectories(
                    OUTPUT.resolve("processed/signatures"));
            Files.createDirectories(
                    OUTPUT.resolve("error"));
            Files.createDirectories(
                    OUTPUT.resolve("bucket/aadtex-daily-reports/2026/09"));

            // Buscar archivos dentro de input
            try (Stream<Path> archivos = Files.list(INPUT)) {

                archivos
                        .filter(Files::isRegularFile)
                        .filter(this::esArchivoValido)
                        .forEach(this::comprobarArchivo);
            }

        } catch (IOException e) {
            System.err.println(
                    "Error al acceder a los ficheros: "
                            + e.getMessage()
            );
        }
    }

    private boolean esArchivoValido(Path archivo) {

        String nombre = archivo.getFileName()
                .toString()
                .toLowerCase(Locale.ROOT);

        return nombre.endsWith(".csv")
                || nombre.endsWith(".enc")
                || nombre.endsWith(".jpg")
                || nombre.endsWith(".zip");
    }

    private void comprobarArchivo(Path archivo) {

        try {
            // Comprobar si el archivo está vacío
            if (Files.size(archivo) == 0) {

                log.error(
                        "[ALERT EMAIL SENT -> soporte@aadtex.com] "
                                + "CRITICAL: Empty/Corrupt batch file: {}",
                        archivo.getFileName()
                );

                // Ruta de destino
                Path destino = OUTPUT.resolve("error")
                        .resolve(archivo.getFileName());

                // Mover el archivo a la carpeta error
                Files.move(
                        archivo,
                        destino,
                        StandardCopyOption.REPLACE_EXISTING
                );

                System.out.println(
                        "Archivo movido a errores: "
                                + archivo.getFileName()
                );

            } else {

                String nombre = archivo.getFileName().toString();

                String extension = nombre.toLowerCase(Locale.ROOT);

                if (extension.endsWith(".csv")) {

                    procesarCsv(archivo);

                } else if (extension.endsWith(".jpg")) {

                    copiarFirma(archivo);

                } else {

                    System.out.println("Archivo no vacío: " + nombre);
                }
            }

        } catch (IOException e) {
            System.err.println(
                    "Error al comprobar el archivo: "
                            + e.getMessage()
            );
        }
    }

    private void procesarCsv(Path archivo) throws IOException {

        BigDecimal total = BigDecimal.ZERO;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        Files.newInputStream(archivo),
                        StandardCharsets.UTF_8))) {

            // Saltar la cabecera
            reader.readLine();

            String linea;

            while ((linea = reader.readLine()) != null) {

                if (linea.isBlank()) {
                    continue;
                }

                String[] campos = linea.split(",", -1);

                if (campos.length != 5) {
                    throw new IOException(
                            "Fila CSV incorrecta: " + linea);
                }

                System.out.println("Tienda: " + campos[1]);

                try {
                    BigDecimal venta = new BigDecimal(campos[2]);
                    total = total.add(venta);

                } catch (NumberFormatException e) {
                    throw new IOException(
                            "Importe incorrecto: " + campos[2], e);
                }
            }
        }

        System.out.println("Total de ventas de "
                + archivo.getFileName() + ": " + total);
    }

    private void copiarFirma(Path archivo) throws IOException {

        // Ruta donde guardaremos la firma
        Path destino = OUTPUT.resolve("processed/signatures")
                .resolve(archivo.getFileName());

        // Abrir el archivo de entrada y el de salida
        try (
                BufferedInputStream entrada =
                        new BufferedInputStream(Files.newInputStream(archivo));

                BufferedOutputStream salida =
                        new BufferedOutputStream(Files.newOutputStream(destino))
        ) {

            // Buffer de 4 KB
            byte[] buffer = new byte[4096];

            int bytesLeidos;

            // Leer y copiar el archivo por bloques
            while ((bytesLeidos = entrada.read(buffer)) != -1) {

                salida.write(buffer, 0, bytesLeidos);
            }
        }

        System.out.println("Firma copiada: " + archivo.getFileName());
    }
}