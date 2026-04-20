package com.lynch.versionhandle.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RepositoryService {

    /**
     * Initialises repository (sets up basic versionhandle folder structure)
     * @param repoPath project root repository
     */
    public void init(Path repoPath) {

        // Create versionhandle path
        Path vhPath = repoPath.resolve(".versionhandle");

        if(Files.exists(vhPath)) {
            System.out.println("Repository already exists");
            return;
        }

        try {
            // Create versionhandle folders
            Files.createDirectories(vhPath);
            Files.createDirectories(vhPath.resolve("objects"));
            Files.createDirectories(vhPath.resolve("commits"));
            Files.createDirectories(vhPath.resolve("branches"));
            Files.createFile(vhPath.resolve("branches").resolve("main"));
            Files.createFile(vhPath.resolve("CURRENT"));
            Files.writeString(vhPath.resolve("CURRENT"), "main");

            System.out.println("Repository initialised successfully.");

        } catch (IOException e) {
            throw new RuntimeException("Failed to initialise repository.", e);
        }
    }
}
