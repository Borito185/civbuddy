package com.civbuddy.commands;

import java.util.ArrayList;
import java.util.List;

public class BookmarkCategory {
    private String name;
    private int color;
    private List<BookmarkEntry> entries;

    public BookmarkCategory(String name, int color) {
        this.name = name;
        this.color = color;
        this.entries = new ArrayList<>();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
    public List<BookmarkEntry> getEntries() { return entries; }

    public void addEntry(BookmarkEntry entry) {
        // Check for duplicates - don't add if command already exists
        for (BookmarkEntry existing : entries) {
            if (existing.getCommand().equals(entry.getCommand())) {
                return; // Duplicate found, don't add
            }
        }
        entries.add(entry);
    }

    public void removeEntry(BookmarkEntry entry) { entries.remove(entry); }
}