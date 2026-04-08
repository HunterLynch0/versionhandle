package com.lynch.versionhandle.service;

import com.lynch.versionhandle.core.Repository;
import java.nio.file.Path;

public class RepositoryService {

    public void init() {
        Repository repo = new Repository();
        repo.initialise(Path.of("."));
    }
}
