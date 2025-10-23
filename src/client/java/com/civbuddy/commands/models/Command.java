package com.civbuddy.commands.models;

public class Command implements Cloneable {
    private String command;

    public Command(String command) {
        this.command = command;
    }

    public String getCommand() { return command; }
    public void setCommand(String command) {
        this.command = command;
    }

    @Override
    public Command clone() {
        try {
            Command clone = (Command) super.clone();
            clone.command = command;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}