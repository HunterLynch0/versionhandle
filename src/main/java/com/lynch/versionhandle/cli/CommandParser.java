package com.lynch.versionhandle.cli;

import com.lynch.versionhandle.service.RepositoryService;

public class CommandParser {

    public void parse(String[] args) {

        if(args.length == 0) {
            System.out.println("Enter a command.");
            return;
        }

        String command = args[0];

        switch(command) {
            case("init"):
                new RepositoryService().init();
                break;
            case("add"):
                break;
            case("commit"):
                break;
            case("log"):
                break;
            case("branch"):
                break;
            default:
                System.out.println("Invalid command: " + command);
        }
    }
}
