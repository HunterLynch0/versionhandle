package com.lynch.versionhandle;

import com.lynch.versionhandle.cli.CommandParser;

public class Main {
    public static void main(String[] args) {

        CommandParser parser = new CommandParser();
        parser.parse(args);

    }
}