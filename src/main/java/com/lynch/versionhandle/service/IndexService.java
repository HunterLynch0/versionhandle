package com.lynch.versionhandle.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class IndexService {

    /**
     * Loads index from file
     * @param repoPath relevant repository
     * @return index - a map from each filename to its latest add
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

        } catch(IOException e) {
            throw new RuntimeException("Failed to load index.");
        }

        return index;
    }
}
