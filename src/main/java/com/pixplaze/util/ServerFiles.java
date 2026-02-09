package com.pixplaze.util;

import org.bukkit.Bukkit;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class ServerFiles {

    /**
     * Loads image file and converts it to Base64 string
     * @param file        image file to convert to Base64 string
     * @param imageFormat image format as png, img, etc...
     * @return Base64 string of provided image file
     * @throws IOException if file not
     */
    public static String imageToBase64(File file, String imageFormat) throws IOException {
        final var outputStream = new ByteArrayOutputStream();
        final var serverIconImage = ImageIO.read(file);

        ImageIO.write(serverIconImage, imageFormat, outputStream);

        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    public static Path getServerDirectoryPath() {
        return Paths.get(Bukkit.getServer().getWorldContainer().getAbsoluteFile().getParent());
    }
}
