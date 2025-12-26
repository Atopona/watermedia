package org.watermedia.api.subtitle.parsers;

import org.watermedia.api.subtitle.SubtitleEntry;
import org.watermedia.api.subtitle.SubtitleTrack;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for SRT (SubRip) subtitle format
 * 
 * SRT Format:
 * 1
 * 00:00:01,000 --> 00:00:04,000
 * First subtitle text
 * 
 * 2
 * 00:00:05,000 --> 00:00:08,000
 * Second subtitle text
 */
public class SrtParser {
    
    // Pattern: 00:00:01,000 --> 00:00:04,000
    private static final Pattern TIME_PATTERN = Pattern.compile(
        "(\\d{1,2}):(\\d{2}):(\\d{2})[,\\.](\\d{3})\\s*-->\\s*(\\d{1,2}):(\\d{2}):(\\d{2})[,\\.](\\d{3})"
    );

    /**
     * Parse SRT content from string
     */
    public static SubtitleTrack parse(String content, String name) {
        SubtitleTrack track = new SubtitleTrack(name, null, SubtitleTrack.SubtitleFormat.SRT);
        
        String[] blocks = content.split("\\r?\\n\\r?\\n");
        
        for (String block : blocks) {
            block = block.trim();
            if (block.isEmpty()) continue;
            
            String[] lines = block.split("\\r?\\n");
            if (lines.length < 2) continue;
            
            // Find the timing line
            int timeLineIndex = -1;
            for (int i = 0; i < lines.length; i++) {
                if (TIME_PATTERN.matcher(lines[i]).find()) {
                    timeLineIndex = i;
                    break;
                }
            }
            
            if (timeLineIndex == -1) continue;
            
            Matcher matcher = TIME_PATTERN.matcher(lines[timeLineIndex]);
            if (!matcher.find()) continue;
            
            long startTime = parseTime(
                matcher.group(1), matcher.group(2), 
                matcher.group(3), matcher.group(4)
            );
            long endTime = parseTime(
                matcher.group(5), matcher.group(6), 
                matcher.group(7), matcher.group(8)
            );
            
            // Collect text lines (everything after timing)
            StringBuilder text = new StringBuilder();
            for (int i = timeLineIndex + 1; i < lines.length; i++) {
                if (text.length() > 0) text.append("\n");
                text.append(cleanSrtText(lines[i]));
            }
            
            if (text.length() > 0) {
                track.addEntry(new SubtitleEntry(startTime, endTime, text.toString()));
            }
        }
        
        track.sortByTime();
        return track;
    }

    /**
     * Parse SRT from InputStream
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
     * Parse SRT from File
     */
    public static SubtitleTrack parse(File file) throws IOException {
        Charset charset = detectCharset(file);
        try (FileInputStream fis = new FileInputStream(file)) {
            return parse(fis, file.getName(), charset);
        }
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
     * Clean SRT text by removing formatting tags
     */
    private static String cleanSrtText(String text) {
        // Remove HTML-like tags: <b>, </b>, <i>, </i>, <u>, </u>, <font...>, </font>
        text = text.replaceAll("<[^>]+>", "");
        // Remove ASS-style tags: {\an8}, {\pos(x,y)}, etc.
        text = text.replaceAll("\\{[^}]+\\}", "");
        return text.trim();
    }

    /**
     * Try to detect file charset (BOM detection)
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
        return StandardCharsets.UTF_8; // Default to UTF-8
    }
}
