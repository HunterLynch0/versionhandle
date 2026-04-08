package com.lynch.versionhandle.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Repository {

    public void initialise(Path path) {

        // create versionhandle path
        Path vhPath = path.resolve(".versionhandle");

        if(Files.exists(vhPath)) {
            System.out.println("Repository already exists");
            return;
        }

        try {

            // create versionhandle folders
            Files.createDirectories(vhPath);
            Files.createDirectories(vhPath.resolve("objects"));
            Files.createDirectories(vhPath.resolve("commits"));

            System.out.println("Repository initialised successfully.");

        } catch (IOException e) {
            throw new RuntimeException("Failed to initialise repository.", e);
        }
    }
}
