package com.civbuddy.commands.data;

import java.util.ArrayList;
import java.util.List;

public class CommandCategory {
    private long id = -1;
    private String name;
    private int color;
    private int sortOrder;
    private List<CommandEntry> entries;

    public CommandCategory(String name, int color) {
        this.name = name;
        this.color = color;
        this.entries = new ArrayList<>();
    }

    public CommandCategory(long id, String name, int color, int sortOrder) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.sortOrder = sortOrder;
        this.entries = new ArrayList<>();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public List<CommandEntry> getEntries() { return entries; }

    public void addEntry(CommandEntry entry) {
        for (CommandEntry existing : entries) {
            if (existing.getCommand().equals(entry.getCommand())) {
                return;
            }
        }
        entries.add(entry);
    }

    public void removeEntry(CommandEntry entry) { entries.remove(entry); }

    public boolean isProtected() {
        return "History".equals(name);
    }
}
