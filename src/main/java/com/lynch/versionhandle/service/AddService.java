package com.lynch.versionhandle.service;

import java.nio.file.Files;
import java.nio.file.Path;

public class AddService {

    public void add(String fileName) {

        Path repoPath = Path.of(".");
        Path vhPath = repoPath.resolve(".versionhandle");

        if(!Files.exists(vhPath)) {
            System.out.println("No versionhandle repository. Initialise project first.");
            return;
        }

        Path filePath = vhPath.resolve(fileName);

        if(!Files.exists(filePath)) {
            System.out.println("File does not exist: " + fileName);
            return;
        }

        try {

        } catch(Exception e) {}
    }
}
