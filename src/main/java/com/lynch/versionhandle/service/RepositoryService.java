package com.lynch.versionhandle.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RepositoryService {

    /**
     * Initialises repository (sets up basic versionhandle folder structure)
     * @param repoPath the relevant repository
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

            System.out.println("Repository initialised successfully.");

        } catch (IOException e) {
            throw new RuntimeException("Failed to initialise repository.", e);
        }
    }
}
