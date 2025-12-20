package com.finditnow.filegateway.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class LocalFileStorage implements FileStorage {
    private final Path root;

    public LocalFileStorage(Path root) {
        this.root = root;
    }

    @Override
    public void save(InputStream input, String fileKey) throws IOException {
        Path target = root.resolve(fileKey);

        Files.createDirectories(target.getParent());

        Path temp = Files.createTempFile("upload-", ".tmp");
        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
    }

    @Override
    public InputStream read(String fileKey) throws IOException {
        return Files.newInputStream(root.resolve(fileKey));
    }

    @Override
    public boolean exists(String fileKey) throws IOException {
        return Files.exists(root.resolve(fileKey));
    }
}
