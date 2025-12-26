package org.watermedia.api.subtitle.parsers;

import org.watermedia.api.subtitle.SubtitleEntry;
import org.watermedia.api.subtitle.SubtitleTrack;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for WebVTT (Web Video Text Tracks) subtitle format
 * 
 * VTT Format:
 * WEBVTT
 * 
 * 00:00:01.000 --> 00:00:04.000
 * First subtitle text
 * 
 * 00:00:05.000 --> 00:00:08.000
 * Second subtitle text
 */
public class VttParser {
    
    // Pattern: 00:00:01.000 --> 00:00:04.000 or 00:01.000 --> 00:04.000
    private static final Pattern TIME_PATTERN = Pattern.compile(
        "(?:(\\d{1,2}):)?(\\d{2}):(\\d{2})\\.(\\d{3})\\s*-->\\s*(?:(\\d{1,2}):)?(\\d{2}):(\\d{2})\\.(\\d{3})"
    );

    /**
     * Parse VTT content from string
     */
    public static SubtitleTrack parse(String content, String name) {
        SubtitleTrack track = new SubtitleTrack(name, null, SubtitleTrack.SubtitleFormat.VTT);
        
        // Remove WEBVTT header and any metadata
        String[] lines = content.split("\\r?\\n");
        StringBuilder currentBlock = new StringBuilder();
        boolean foundHeader = false;
        
        for (String line : lines) {
            // Skip WEBVTT header
            if (line.trim().startsWith("WEBVTT")) {
                foundHeader = true;
                continue;
            }
            
            // Skip NOTE comments
            if (line.trim().startsWith("NOTE")) {
                continue;
            }
            
            // Skip STYLE blocks
            if (line.trim().equals("STYLE")) {
                // Skip until empty line
                continue;
            }
            
            if (line.trim().isEmpty()) {
                // Process current block
                if (currentBlock.length() > 0) {
                    SubtitleEntry entry = parseBlock(currentBlock.toString());
                    if (entry != null) {
                        track.addEntry(entry);
                    }
                    currentBlock = new StringBuilder();
                }
            } else {
                currentBlock.append(line).append("\n");
            }
        }
        
        // Process last block
        if (currentBlock.length() > 0) {
            SubtitleEntry entry = parseBlock(currentBlock.toString());
            if (entry != null) {
                track.addEntry(entry);
            }
        }
        
        track.sortByTime();
        return track;
    }

    /**
     * Parse a single cue block
     */
    private static SubtitleEntry parseBlock(String block) {
        String[] lines = block.trim().split("\\r?\\n");
        if (lines.length < 2) return null;
        
        // Find timing line
        int timeLineIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            if (TIME_PATTERN.matcher(lines[i]).find()) {
                timeLineIndex = i;
                break;
            }
        }
        
        if (timeLineIndex == -1) return null;
        
        Matcher matcher = TIME_PATTERN.matcher(lines[timeLineIndex]);
        if (!matcher.find()) return null;
        
        // Parse start time
        String startHours = matcher.group(1);
        long startTime = parseTime(
            startHours != null ? startHours : "0",
            matcher.group(2), matcher.group(3), matcher.group(4)
        );
        
        // Parse end time
        String endHours = matcher.group(5);
        long endTime = parseTime(
            endHours != null ? endHours : "0",
            matcher.group(6), matcher.group(7), matcher.group(8)
        );
        
        // Collect text lines
        StringBuilder text = new StringBuilder();
        for (int i = timeLineIndex + 1; i < lines.length; i++) {
            if (text.length() > 0) text.append("\n");
            text.append(cleanVttText(lines[i]));
        }
        
        if (text.length() > 0) {
            return new SubtitleEntry(startTime, endTime, text.toString());
        }
        
        return null;
    }

    /**
     * Convert time components to milliseconds
     */
    private static long parseTime(String hours, String minutes, String seconds, String millis) {
        return Long.parseLong(hours) * 3600000L +
               Long.parseLong(minutes) * 60000L +
               Long.parseLong(seconds) * 1000L +
               Long.parseLong(millis);
    }

    /**
     * Clean VTT text by removing formatting tags
     */
    private static String cleanVttText(String text) {
        // Remove VTT tags: <c>, </c>, <v Name>, <b>, <i>, <u>, <ruby>, etc.
        text = text.replaceAll("<[^>]+>", "");
        // Remove timestamp tags
        text = text.replaceAll("<\\d{2}:\\d{2}:\\d{2}\\.\\d{3}>", "");
        return text.trim();
    }

    /**
     * Parse VTT from InputStream
     */
    public static SubtitleTrack parse(InputStream is, String name, Charset charset) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return parse(content.toString(), name);
    }

    /**
     * Parse VTT from File
     */
    public static SubtitleTrack parse(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return parse(fis, file.getName(), StandardCharsets.UTF_8);
        }
    }
}
