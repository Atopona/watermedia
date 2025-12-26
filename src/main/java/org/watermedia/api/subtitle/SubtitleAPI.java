package org.watermedia.api.subtitle;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.watermedia.api.WaterMediaAPI;
import org.watermedia.api.subtitle.parsers.AssParser;
import org.watermedia.api.subtitle.parsers.SrtParser;
import org.watermedia.api.subtitle.parsers.VttParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.watermedia.WaterMedia.LOGGER;

/**
 * API for parsing and managing subtitles
 * Supports SRT, ASS, SSA, and WebVTT formats
 */
public class SubtitleAPI extends WaterMediaAPI {
    
    private static final Marker IT = MarkerManager.getMarker("SubtitleAPI");

    /**
     * Parse subtitle from file path
     * @param filePath path to subtitle file
     * @return parsed SubtitleTrack or null on error
     */
    public static SubtitleTrack parseFile(String filePath) {
        return parseFile(new File(filePath));
    }

    /**
     * Parse subtitle from File
     * @param file subtitle file
     * @return parsed SubtitleTrack or null on error
     */
    public static SubtitleTrack parseFile(File file) {
        if (!file.exists()) {
            LOGGER.warn(IT, "Subtitle file not found: {}", file.getAbsolutePath());
            return null;
        }

        String name = file.getName().toLowerCase();
        try {
            if (name.endsWith(".srt")) {
                return SrtParser.parse(file);
            } else if (name.endsWith(".ass") || name.endsWith(".ssa")) {
                return AssParser.parse(file);
            } else if (name.endsWith(".vtt")) {
                return VttParser.parse(file);
            } else {
                LOGGER.warn(IT, "Unknown subtitle format: {}", name);
                return null;
            }
        } catch (IOException e) {
            LOGGER.error(IT, "Failed to parse subtitle file: {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    /**
     * Parse subtitle from URI (supports file:// and http(s)://)
     * @param uri subtitle URI
     * @return parsed SubtitleTrack or null on error
     */
    public static SubtitleTrack parseUri(URI uri) {
        String scheme = uri.getScheme();
        
        if (scheme == null || scheme.equals("file")) {
            return parseFile(new File(uri.getPath()));
        }
        
        if (scheme.equals("http") || scheme.equals("https")) {
            return parseUrl(uri.toString());
        }
        
        LOGGER.warn(IT, "Unsupported URI scheme: {}", scheme);
        return null;
    }

    /**
     * Parse subtitle from URL string
     * @param urlString URL to subtitle file
     * @return parsed SubtitleTrack or null on error
     */
    public static SubtitleTrack parseUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "WaterMedia/2.1");
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                LOGGER.warn(IT, "Failed to download subtitle, HTTP {}: {}", responseCode, urlString);
                return null;
            }
            
            // Detect format from URL or content-type
            String format = detectFormat(urlString, conn.getContentType());
            
            try (InputStream is = conn.getInputStream()) {
                return parseStream(is, urlString, format, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            LOGGER.error(IT, "Failed to parse subtitle from URL: {}", urlString, e);
            return null;
        }
    }

    /**
     * Parse subtitle from InputStream
     * @param is input stream
     * @param name name for the track
     * @param format format string (srt, ass, ssa, vtt)
     * @param charset character encoding
     * @return parsed SubtitleTrack or null on error
     */
    public static SubtitleTrack parseStream(InputStream is, String name, String format, Charset charset) {
        try {
            switch (format.toLowerCase()) {
                case "srt":
                    return SrtParser.parse(is, name, charset);
                case "ass":
                    return AssParser.parse(is, name, charset, false);
                case "ssa":
                    return AssParser.parse(is, name, charset, true);
                case "vtt":
                case "webvtt":
                    return VttParser.parse(is, name, charset);
                default:
                    LOGGER.warn(IT, "Unknown subtitle format: {}", format);
                    return null;
            }
        } catch (IOException e) {
            LOGGER.error(IT, "Failed to parse subtitle stream", e);
            return null;
        }
    }

    /**
     * Parse subtitle from string content
     * @param content subtitle content
     * @param name name for the track
     * @param format format string (srt, ass, ssa, vtt)
     * @return parsed SubtitleTrack or null on error
     */
    public static SubtitleTrack parseString(String content, String name, String format) {
        switch (format.toLowerCase()) {
            case "srt":
                return SrtParser.parse(content, name);
            case "ass":
                return AssParser.parse(content, name, false);
            case "ssa":
                return AssParser.parse(content, name, true);
            case "vtt":
            case "webvtt":
                return VttParser.parse(content, name);
            default:
                LOGGER.warn(IT, "Unknown subtitle format: {}", format);
                return null;
        }
    }

    /**
     * Detect subtitle format from filename or content-type
     */
    private static String detectFormat(String filename, String contentType) {
        // Try filename extension first
        String lower = filename.toLowerCase();
        if (lower.endsWith(".srt")) return "srt";
        if (lower.endsWith(".ass")) return "ass";
        if (lower.endsWith(".ssa")) return "ssa";
        if (lower.endsWith(".vtt")) return "vtt";
        
        // Try content-type
        if (contentType != null) {
            contentType = contentType.toLowerCase();
            if (contentType.contains("srt") || contentType.contains("subrip")) return "srt";
            if (contentType.contains("ass") || contentType.contains("ssa")) return "ass";
            if (contentType.contains("vtt") || contentType.contains("webvtt")) return "vtt";
        }
        
        // Default to SRT
        return "srt";
    }

    /**
     * Find subtitle files next to a video file
     * @param videoFile the video file
     * @return list of found subtitle files
     */
    public static List<File> findSubtitlesForVideo(File videoFile) {
        List<File> subtitles = new ArrayList<>();
        
        if (!videoFile.exists()) return subtitles;
        
        File parent = videoFile.getParentFile();
        if (parent == null) return subtitles;
        
        String videoName = videoFile.getName();
        int dotIndex = videoName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? videoName.substring(0, dotIndex) : videoName;
        
        String[] extensions = {".srt", ".ass", ".ssa", ".vtt"};
        String[] languages = {"", ".en", ".zh", ".ja", ".ko", ".chs", ".cht", ".chi", ".eng", ".jpn"};
        
        for (String lang : languages) {
            for (String ext : extensions) {
                File subFile = new File(parent, baseName + lang + ext);
                if (subFile.exists()) {
                    subtitles.add(subFile);
                }
            }
        }
        
        return subtitles;
    }

    /**
     * Get format from file extension
     */
    public static SubtitleTrack.SubtitleFormat getFormatFromExtension(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".srt")) return SubtitleTrack.SubtitleFormat.SRT;
        if (lower.endsWith(".ass")) return SubtitleTrack.SubtitleFormat.ASS;
        if (lower.endsWith(".ssa")) return SubtitleTrack.SubtitleFormat.SSA;
        if (lower.endsWith(".vtt")) return SubtitleTrack.SubtitleFormat.VTT;
        return SubtitleTrack.SubtitleFormat.UNKNOWN;
    }
}
