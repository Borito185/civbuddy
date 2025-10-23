package com.civbuddy.commands.models;

import java.util.ArrayList;
import java.util.List;

public class CommandGroup {
    private String name;
    private int color;
    private List<Command> entries;

    public CommandGroup(String name, int color) {
        this.name = name;
        this.color = color;
        this.entries = new ArrayList<>();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
    public List<Command> getEntries() { return entries; }

    public void addEntry(Command entry) {
        // Check for duplicates - don't add if command already exists
        for (Command existing : entries) {
            if (existing.getCommand().equals(entry.getCommand())) {
                return; // Duplicate found, don't add
            }
        }
        entries.add(entry);
    }

    public void removeEntry(Command entry) { entries.remove(entry); }

    public boolean isHistoryGroup() {
        return "History".equals(name);
    }
}