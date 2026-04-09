package com.lynch.versionhandle.service;

import com.lynch.versionhandle.util.HashUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AddService {

    /**
     * Checks conditions for file to be added then adds to objects if valid
     * @param fileName name of file requested to add
     */
    public void add(String fileName) {

        Path repoPath = Path.of(".");
        Path vhPath = repoPath.resolve(".versionhandle");
        Path filePath = repoPath.resolve(fileName);

        // checks project is initialised
        if(!Files.exists(vhPath)) {
            System.out.println("No versionhandle repository. Initialise project first.");
            return;
        }

        // checks file exists in relevant directory
        if(!Files.exists(filePath)) {
            System.out.println("File does not exist: " + fileName);
            return;
        }

        // hash content and add to objects
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
            throw new RuntimeException("Failed to add file: " + fileName, e);
        }
    }
}
