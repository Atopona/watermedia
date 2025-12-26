package org.watermedia.api.subtitle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a subtitle track containing multiple subtitle entries
 */
public class SubtitleTrack {
    private final String name;
    private final String language;
    private final List<SubtitleEntry> entries;
    private final SubtitleFormat format;

    public SubtitleTrack(String name, String language, SubtitleFormat format) {
        this.name = name;
        this.language = language;
        this.format = format;
        this.entries = new ArrayList<>();
    }

    public void addEntry(SubtitleEntry entry) {
        entries.add(entry);
    }

    public void sortByTime() {
        entries.sort((a, b) -> Long.compare(a.getStartTime(), b.getStartTime()));
    }

    public String getName() {
        return name;
    }

    public String getLanguage() {
        return language;
    }

    public SubtitleFormat getFormat() {
        return format;
    }

    public List<SubtitleEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public int getEntryCount() {
        return entries.size();
    }

    /**
     * Get the subtitle text that should be displayed at the given time
     * @param timeMs current playback time in milliseconds
     * @return subtitle text or null if no subtitle at this time
     */
    public String getTextAt(long timeMs) {
        for (SubtitleEntry entry : entries) {
            if (entry.isActiveAt(timeMs)) {
                return entry.getText();
            }
        }
        return null;
    }

    /**
     * Get all active subtitle entries at the given time
     * @param timeMs current playback time in milliseconds
     * @return list of active entries (may be empty)
     */
    public List<SubtitleEntry> getEntriesAt(long timeMs) {
        List<SubtitleEntry> active = new ArrayList<>();
        for (SubtitleEntry entry : entries) {
            if (entry.isActiveAt(timeMs)) {
                active.add(entry);
            }
        }
        return active;
    }

    /**
     * Binary search to find subtitle entry at given time (more efficient for large files)
     * @param timeMs current playback time in milliseconds
     * @return subtitle entry or null
     */
    public SubtitleEntry findEntryAt(long timeMs) {
        int low = 0;
        int high = entries.size() - 1;

        while (low <= high) {
            int mid = (low + high) / 2;
            SubtitleEntry entry = entries.get(mid);

            if (entry.isActiveAt(timeMs)) {
                return entry;
            } else if (timeMs < entry.getStartTime()) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return null;
    }

    public enum SubtitleFormat {
        SRT,
        ASS,
        SSA,
        VTT,
        UNKNOWN
    }
}
