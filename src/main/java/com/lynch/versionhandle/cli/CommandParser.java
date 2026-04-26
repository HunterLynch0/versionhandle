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
            case "help":
                if(args.length != 1) {
                    System.out.print("Error: unknown additional arguments: ");
                    for(int i = 1; i < args.length; i++) {
                        System.out.print(args[i] + " ");
                    }
                    System.out.println("\nTip: run help to list valid command usage.");
                    return;
                }
                printHelp();
                break;
            case "init":
                if(args.length != 1) {
                    System.out.print("Error: unknown additional arguments: ");
                    for(int i = 1; i < args.length; i++) {
                        System.out.print(args[i] + " ");
                    }
                    System.out.println("\nTip: run help to list valid command usage.");
                    return;
                }
                new RepositoryService().init(repoPath);
                break;

            case "add":
                if(args.length < 2) {
                    System.out.println("Error: no filename provided." +
                            "\nTip: run help to list valid command usage.");
                    return;
                }
                for(int i = 1; i < args.length; i++) {
                    String fileName = args[i];
                    new AddService().add(repoPath, fileName, i);
                }
                break;

            case "commit":
                if(args.length < 2) {
                    System.out.println("Error: no commit message." +
                            "\nTip: run help to list valid command usage.");
                    return;
                }
                StringBuilder message = new StringBuilder();
                for(int i = 1; i < args.length; i++) {
                    message.append(args[i]).append(" ");
                }
                new CommitService().commit(repoPath, message.toString().trim());
                break;

            case "log":
                LogService logService = new LogService();
                if(args.length == 2 && args[1].equals("-a")) {
                    logService.logAll(repoPath);
                } else if (args.length == 1){
                    logService.log(repoPath);
                } else {
                    System.out.print("Error: unknown additional arguments: ");
                    for(int i = 1; i < args.length; i++) {
                        System.out.print(args[i] + " ");
                    }
                    System.out.println("\nTip: run help to list valid command usage.");
                }
                break;

            case "checkout":
                if(args.length < 2 || args.length > 3) {
                    System.out.println("Error: unknown usage." +
                            "\nTip: run help to list valid command usage.");
                    return;
                }

                boolean force = false;
                if(args.length == 3) {
                    if(!args[2].equals("-f")) {
                        System.out.println("Error: unknown usage." +
                                "\nTip: run help to list valid command usage.");
                        return;
                    }
                    force = true;
                }
                new CheckoutService().checkout(repoPath, args[1], force);
                break;

            case "status":
                if(args.length != 1) {
                    System.out.print("Error: unknown additional arguments: ");
                    for(int i = 1; i < args.length; i++) {
                        System.out.print(args[i] + " ");
                    }
                    System.out.println("\nTip: run help to list valid command usage.");
                    return;
                }
                new StatusService().status(repoPath);
                break;

            case "branch":
                if(args.length == 1) {
                    System.out.println("Error: no branch name provided." +
                            "\nTip: run help to list valid command usage.");
                    return;
                } else if(args.length > 2) {
                    System.out.print("Error: unknown additional arguments (branch name cannot include empty spaces): ");
                    for(int i = 1; i < args.length; i++) {
                        System.out.print(args[i] + " ");
                    }
                    System.out.println("\nTip: run help to list valid command usage.");
                    return;
                } else {
                    new BranchService().branch(repoPath, args[1]);
                }
                break;

            case "remove":
                if(args.length == 1) {
                    System.out.println("Error: no filename provided." +
                            "\nTip: run help to list valid command usage.");
                    return;
                }
                for(int i = 1; i < args.length; i++) {
                    new RemoveService().remove(repoPath, args[i]);
                }
                break;

            case "merge":
                if(args.length == 1) {
                    System.out.println("Error: no target branch provided." +
                            "\nTip: run help to list valid command usage.");
                    return;
                } else if(args.length > 2) {
                    System.out.print("Error: unknown additional arguments): ");
                    for(int i = 1; i < args.length; i++) {
                        System.out.print(args[i] + " ");
                    }
                    System.out.println("\nTip: run help to list valid command usage.");
                    return;
                } else {
                    new MergeService().merge(repoPath, args[1]);
                }
                break;

            default:
                System.out.println("Invalid command: " + command +
                        "\nTip: run 'help' list valid command usage.");
        }
    }

    /**
     * Print CLI help menu
     */
    public void printHelp() {
        System.out.println("Versionhandle commands:");
        System.out.println("   init");
        System.out.println("   add <filename> [more files] || add .");
        System.out.println("   remove <filename> [more files]");
        System.out.println("   commit <message>");
        System.out.println("   log [-a]");
        System.out.println("   status");
        System.out.println("   checkout <commitId|branchName> [-f]");
        System.out.println("   branch <branchName>");
        System.out.println("   merge <branchName>");
        System.out.println("   help");
    }
}
