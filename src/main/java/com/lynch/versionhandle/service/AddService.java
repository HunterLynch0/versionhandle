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
            System.out.println("Not a versionhandle repository. Initialise project first.");
            return;
        }

        // checks file exists in relevant directory
        if(!Files.exists(filePath)) {
            System.out.println("File does not exist: " + fileName);
            return;
        }

        // hash content then store to objects + stage
        try {
            byte[] content = Files.readAllBytes(filePath);
            String hash = HashUtil.sha256(content);

            Path objectPath = vhPath.resolve("objects").resolve(hash);

            // store
            if(!Files.exists(objectPath)) {
                Files.write(objectPath, content);
            }

            // stage
            new IndexService().stageFile(repoPath, fileName, hash);

            System.out.println("File staged: " + fileName);
            System.out.println("Object hash: " + hash);

        } catch(IOException e) {
            throw new RuntimeException("Failed to add file: " + fileName, e);
        }


    }
}
