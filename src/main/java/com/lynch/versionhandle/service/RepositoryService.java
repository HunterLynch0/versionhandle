package com.lynch.versionhandle.service;

import com.lynch.versionhandle.core.Repository;
import java.nio.file.Path;

public class RepositoryService {

    /**
     * Executes initialise from repository in the current directory
     */
    public void init() {
        Repository repo = new Repository();
        repo.initialise(Path.of("."));
    }
}
