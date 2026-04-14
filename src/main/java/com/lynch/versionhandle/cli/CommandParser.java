package com.lynch.versionhandle.cli;

import com.lynch.versionhandle.service.*;

import java.nio.file.Files;
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

        Path repoPath = Path.of(".");

        String command = args[0];

        switch(command) {
            case "init":
                new RepositoryService().init(repoPath);
                break;

            case "add":
                if(args.length < 2) {
                    System.out.println("Nothing added, please specify files.");
                    return;
                }
                for(int i = 1; i < args.length; i++) {
                    String fileName = args[i];
                    new AddService().add(repoPath, fileName);
                }
                break;

            case "commit":
                if(args.length < 2) {
                    System.out.println("Error, please provide commit message");
                    return;
                }
                StringBuilder message = new StringBuilder();
                for(int i = 1; i < args.length; i++) {
                    message.append(args[i]).append(" ");
                }
                new CommitService().commit(repoPath, message.toString().trim());
                break;

            case "log":
                new LogService().log(repoPath);
                break;

            case "checkout":
                if(args.length < 2) {
                    System.out.println("Please provide commit id.");
                }
                CheckoutService checkoutService = new CheckoutService();
                if((args[1]).equals("CURRENT")) {
                    checkoutService.checkout(repoPath, new CommitService().readCurrent(repoPath));
                } else {
                    checkoutService.checkout(repoPath, args[1]);
                }
                break;

            default:
                System.out.println("Invalid command: " + command);
        }
    }
}
