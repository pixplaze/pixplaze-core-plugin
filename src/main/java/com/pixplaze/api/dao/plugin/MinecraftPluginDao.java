package com.pixplaze.api.dao.plugin;

import com.pixplaze.api.exception.PixplazeApiException;
import com.pixplaze.api.ext.data.plugin.MinecraftPluginInfo;
import com.pixplaze.util.ServerFiles;
import com.pixplaze.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.io.*;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class MinecraftPluginDao {
    private final Server server = Bukkit.getServer();

    public List<MinecraftPluginInfo> getEnabledPlugins() {
        return Arrays.stream(server.getPluginManager().getPlugins())
                .map(p -> new MinecraftPluginInfo(p.getName(), null, null))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<MinecraftPluginInfo> getInstalledPlugins() {
        final var installedPlugins = new ArrayList<MinecraftPluginInfo>();
        final var pluginsDirectory = Paths.get(ServerFiles.getServerDirectoryPath().toString(), "plugins").toFile();
        final var pluginFiles = pluginsDirectory.listFiles(this::isJarFile);

        for (var pluginFile : pluginFiles) {
            try {
                installedPlugins.add(toMinecraftPluginInfo(pluginFile));
            } catch (IOException e) {
                throw new PixplazeApiException("Could not read plugin info for %s!".formatted(pluginFile.getName()), e);
            } catch (NoSuchAlgorithmException e) {
                throw new PixplazeApiException("Could not calculate SHA256 hash sum for %s".formatted(pluginFile.getName()), e);
            }
        }

        return installedPlugins;
    }

    private MinecraftPluginInfo toMinecraftPluginInfo(File pluginFile) throws IOException, NoSuchAlgorithmException {
        String pluginName = null;
        String pluginVersion = null;
        String sha256sum = calculateFileHash(pluginFile);

        try (final var pluginJarFile = new JarFile(pluginFile)) {
            final var pluginConfigJarEntry = pluginJarFile.stream()
                    .filter(this::isPluginYml)
                    .findFirst()
                    .orElseThrow();
            final var pluginConfigJarEntryInputStream = pluginJarFile.getInputStream(pluginConfigJarEntry);

            try (final var bufferedReader = new BufferedReader(new InputStreamReader(pluginConfigJarEntryInputStream))) {
                String line;
                while (Objects.nonNull(line = bufferedReader.readLine())) {
                    line = line.trim();
                    if (line.startsWith("name:")) {
                        pluginName = Utils.dequotify(line.substring(5)); // Извлекаем имя плагина
                        continue;
                    }

                    if (line.startsWith("version:")) {
                        pluginVersion = Utils.dequotify(line.substring(8)); // Извлекаем версию плагина
                        continue;
                    }

                    if (Objects.nonNull(pluginName) && Objects.nonNull(pluginVersion)) {
                        break;
                    }
                }
            }
        }

        return new MinecraftPluginInfo(pluginName, pluginVersion, sha256sum);
    }

    private static String calculateFileHash(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] byteBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(byteBuffer)) != -1) {
                digest.update(byteBuffer, 0, bytesRead);
            }
        }

        // Преобразуем хэш в шестнадцатеричную строку
        StringBuilder hexString = new StringBuilder();
        for (byte b : digest.digest()) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private boolean isJarFile(File file) {
        return file.getName().endsWith(".jar");
    }

    private boolean isPluginYml(JarEntry jarEntry) {
        return jarEntry.getName().endsWith("plugin.yml");
    }
}
