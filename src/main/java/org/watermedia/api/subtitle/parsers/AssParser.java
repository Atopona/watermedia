package org.watermedia.api.subtitle.parsers;

import org.watermedia.api.subtitle.SubtitleEntry;
import org.watermedia.api.subtitle.SubtitleTrack;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for ASS/SSA (Advanced SubStation Alpha) subtitle format
 * 
 * ASS Format:
 * [Script Info]
 * Title: Example
 * 
 * [V4+ Styles]
 * Format: Name, Fontname, Fontsize, ...
 * Style: Default,Arial,20,...
 * 
 * [Events]
 * Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
 * Dialogue: 0,0:00:01.00,0:00:04.00,Default,,0,0,0,,First subtitle
 */
public class AssParser {
    
    // Pattern for Dialogue lines: Dialogue: 0,0:00:01.00,0:00:04.00,Style,,0,0,0,,Text
    private static final Pattern DIALOGUE_PATTERN = Pattern.compile(
        "^Dialogue:\\s*\\d+,\\s*(\\d+):(\\d{2}):(\\d{2})\\.(\\d{2}),\\s*(\\d+):(\\d{2}):(\\d{2})\\.(\\d{2}),([^,]*),([^,]*),\\d+,\\d+,\\d+,([^,]*),(.*)$"
    );
    
    // Simpler pattern for basic parsing
    private static final Pattern SIMPLE_DIALOGUE_PATTERN = Pattern.compile(
        "^Dialogue:\\s*\\d+,\\s*(\\d+:\\d{2}:\\d{2}\\.\\d{2}),\\s*(\\d+:\\d{2}:\\d{2}\\.\\d{2}),.*?,.*?,\\d+,\\d+,\\d+,.*?,(.*)$"
    );

    /**
     * Parse ASS/SSA content from string
     */
    public static SubtitleTrack parse(String content, String name, boolean isSSA) {
        SubtitleTrack.SubtitleFormat format = isSSA ? SubtitleTrack.SubtitleFormat.SSA : SubtitleTrack.SubtitleFormat.ASS;
        SubtitleTrack track = new SubtitleTrack(name, null, format);
        
        String[] lines = content.split("\\r?\\n");
        boolean inEvents = false;
        
        for (String line : lines) {
            line = line.trim();
            
            // Check for [Events] section
            if (line.equalsIgnoreCase("[Events]")) {
                inEvents = true;
                continue;
            }
            
            // Check for other sections
            if (line.startsWith("[") && line.endsWith("]")) {
                inEvents = false;
                continue;
            }
            
            // Parse Dialogue lines
            if (inEvents && line.startsWith("Dialogue:")) {
                SubtitleEntry entry = parseDialogueLine(line);
                if (entry != null) {
                    track.addEntry(entry);
                }
            }
        }
        
        track.sortByTime();
        return track;
    }

    /**
     * Parse a single Dialogue line
     */
    private static SubtitleEntry parseDialogueLine(String line) {
        Matcher matcher = DIALOGUE_PATTERN.matcher(line);
        
        if (matcher.find()) {
            long startTime = parseTime(
                matcher.group(1), matcher.group(2), 
                matcher.group(3), matcher.group(4)
            );
            long endTime = parseTime(
                matcher.group(5), matcher.group(6), 
                matcher.group(7), matcher.group(8)
            );
            String style = matcher.group(9);
            String text = cleanAssText(matcher.group(11));
            
            if (!text.isEmpty()) {
                return new SubtitleEntry(startTime, endTime, text, style);
            }
        } else {
            // Try simpler pattern
            Matcher simpleMatcher = SIMPLE_DIALOGUE_PATTERN.matcher(line);
            if (simpleMatcher.find()) {
                long startTime = parseTimeString(simpleMatcher.group(1));
                long endTime = parseTimeString(simpleMatcher.group(2));
                String text = cleanAssText(simpleMatcher.group(3));
                
                if (!text.isEmpty()) {
                    return new SubtitleEntry(startTime, endTime, text);
                }
            }
        }
        
        return null;
    }

    /**
     * Parse time string "H:MM:SS.cc" to milliseconds
     */
    private static long parseTimeString(String timeStr) {
        String[] parts = timeStr.split("[:.]");
        if (parts.length >= 4) {
            return parseTime(parts[0], parts[1], parts[2], parts[3]);
        }
        return 0;
    }

    /**
     * Convert time components to milliseconds (ASS uses centiseconds)
     */
    private static long parseTime(String hours, String minutes, String seconds, String centis) {
        return Long.parseLong(hours) * 3600000L +
               Long.parseLong(minutes) * 60000L +
               Long.parseLong(seconds) * 1000L +
               Long.parseLong(centis) * 10L;  // centiseconds to milliseconds
    }

    /**
     * Clean ASS text by removing formatting tags and converting line breaks
     */
    private static String cleanAssText(String text) {
        // Replace \N and \n with actual newlines
        text = text.replace("\\N", "\n").replace("\\n", "\n");
        // Remove ASS override tags: {\tag}
        text = text.replaceAll("\\{[^}]*\\}", "");
        // Remove drawing commands
        text = text.replaceAll("\\{[^}]*\\\\p[^}]*\\}[^{]*\\{[^}]*\\\\p0[^}]*\\}", "");
        return text.trim();
    }

    /**
     * Parse ASS from InputStream
     */
    public static SubtitleTrack parse(InputStream is, String name, Charset charset, boolean isSSA) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return parse(content.toString(), name, isSSA);
    }

    /**
     * Parse ASS/SSA from File
     */
    public static SubtitleTrack parse(File file) throws IOException {
        boolean isSSA = file.getName().toLowerCase().endsWith(".ssa");
        Charset charset = detectCharset(file);
        try (FileInputStream fis = new FileInputStream(file)) {
            return parse(fis, file.getName(), charset, isSSA);
        }
    }

    /**
     * Try to detect file charset
     */
    private static Charset detectCharset(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bom = new byte[4];
            int read = fis.read(bom);
            
            if (read >= 3 && bom[0] == (byte)0xEF && bom[1] == (byte)0xBB && bom[2] == (byte)0xBF) {
                return StandardCharsets.UTF_8;
            }
            if (read >= 2 && bom[0] == (byte)0xFF && bom[1] == (byte)0xFE) {
                return StandardCharsets.UTF_16LE;
            }
            if (read >= 2 && bom[0] == (byte)0xFE && bom[1] == (byte)0xFF) {
                return StandardCharsets.UTF_16BE;
            }
        }
        return StandardCharsets.UTF_8;
    }
}
