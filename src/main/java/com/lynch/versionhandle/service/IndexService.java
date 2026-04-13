package com.lynch.versionhandle.service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class IndexService {

    /**
     * Loads index from file
     * @param repoPath project root repository
     * @return index - a map from each filename to its latest hash
     */
    public Map<String, String> loadIndex(Path repoPath) {

        Map<String, String> index = new HashMap<>();

        Path indexPath = repoPath.resolve(".versionhandle").resolve("index");

        if(!Files.exists(indexPath)) {
            return index;
        }

        try {
            Scanner fileScan = new Scanner(indexPath.toFile());

            while(fileScan.hasNextLine()) {
                String line = fileScan.nextLine();
                String[] parts = line.split(" ");

                if(parts.length == 2) {
                    index.put(parts[0], parts[1]);
                }
            }

            fileScan.close();

        } catch(IOException e) {
            throw new RuntimeException("Failed to load index.", e);
        }

        return index;
    }

    /**
     * Saves index to file
     * @param repoPath project root repository
     * @param index map from each filename to its latest hash
     */
    public void saveIndex(Path repoPath, Map<String, String> index) {

        Path indexPath = repoPath.resolve(".versionhandle").resolve("index");

        try (FileWriter writer = new FileWriter(indexPath.toFile())) {

            for (Map.Entry<String, String> entry : index.entrySet()) {
                String fileName = entry.getKey();
                String hash = entry.getValue();
                writer.write(fileName + " " + hash + "\n");
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to save index.", e);
        }
    }

    /**
     *
     * @param repoPath relevant repository
     * @param fileName file to be added and staged
     * @param hash hashed file contents as hex string
     */
    public void stageFile(Path repoPath, String fileName, String hash) {
        Map<String, String> index = loadIndex(repoPath);
        index.put(fileName, hash);
        saveIndex(repoPath, index);
    }
}
