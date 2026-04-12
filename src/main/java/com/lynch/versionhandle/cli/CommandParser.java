package com.lynch.versionhandle.cli;

import com.lynch.versionhandle.service.AddService;
import com.lynch.versionhandle.service.RepositoryService;

import java.nio.file.Path;

public class CommandParser {

    /**
     * Processes command inputs and determines whether they are valid or not,
     * then executes given action if valid
     * @param args input command and arguments
     */
    public void parse(String[] args) {

        if(args.length == 0) {
            System.out.println("Enter a command.");
            return;
        }

        String command = args[0];

        switch(command) {
            case "init":
                new RepositoryService().init(Path.of("."));
                break;

            case "add":
                if(args.length < 2) {
                    System.out.println("Nothing added, please specify files.");
                    return;
                }
                for(int i = 1; i < args.length; i++) {
                    String fileName = args[i];
                    new AddService().add(fileName);
                }
                break;

            case "commit":
                break;

            case "log":
                break;

            case "branch":
                break;

            default:
                System.out.println("Invalid command: " + command);
        }
    }
}
