package org.watermedia.api.subtitle;

/**
 * Represents a single subtitle entry with timing and text content
 */
public class SubtitleEntry {
    private final long startTime;  // in milliseconds
    private final long endTime;    // in milliseconds
    private final String text;
    private final String style;    // for ASS/SSA styles

    public SubtitleEntry(long startTime, long endTime, String text) {
        this(startTime, endTime, text, null);
    }

    public SubtitleEntry(long startTime, long endTime, String text, String style) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.text = text;
        this.style = style;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public String getText() {
        return text;
    }

    public String getStyle() {
        return style;
    }

    /**
     * Check if this subtitle should be displayed at the given time
     * @param timeMs current playback time in milliseconds
     * @return true if subtitle should be visible
     */
    public boolean isActiveAt(long timeMs) {
        return timeMs >= startTime && timeMs <= endTime;
    }

    @Override
    public String toString() {
        return String.format("[%d-%d] %s", startTime, endTime, text);
    }
}
