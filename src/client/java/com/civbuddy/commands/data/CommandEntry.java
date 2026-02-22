package com.civbuddy.commands.data;

public class CommandEntry {
    private long id = -1;
    private String command;
    private int sortOrder;

    public CommandEntry(String command) {
        this.command = command;
    }

    public CommandEntry(long id, String command, int sortOrder) {
        this.id = id;
        this.command = command;
        this.sortOrder = sortOrder;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
