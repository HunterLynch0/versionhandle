package com.lynch.versionhandle.service;

import com.lynch.versionhandle.util.HashUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AddService {

    public void add(String fileName) {

        Path repoPath = Path.of(".");
        Path vhPath = repoPath.resolve(".versionhandle");
        Path filePath = repoPath.resolve(fileName);

        if(!Files.exists(vhPath)) {
            System.out.println("No versionhandle repository. Initialise project first.");
            return;
        }

        if(!Files.exists(filePath)) {
            System.out.println("File does not exist: " + fileName);
            return;
        }

        try {
            byte[] content = Files.readAllBytes(filePath);
            String hash = HashUtil.sha256(content);

            Path objectPath = vhPath.resolve("objects").resolve(hash);

            if(!Files.exists(objectPath)) {
                Files.write(objectPath, content);
            }

            System.out.println("File added: " + fileName);
            System.out.println("Stored as: " + hash);

        } catch(IOException e) {
            throw new RuntimeException("Failed to add file: " + fileName);
        }
    }
}
