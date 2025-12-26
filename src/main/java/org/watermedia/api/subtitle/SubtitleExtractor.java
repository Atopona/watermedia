package org.watermedia.api.subtitle;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.watermedia.WaterMedia.LOGGER;

/**
 * Extracts embedded subtitles from video files using FFmpeg/FFprobe
 * Supports MKV, MP4, AVI and other container formats
 */
public final class SubtitleExtractor {
    
    private static final Marker IT = MarkerManager.getMarker("SubtitleExtractor");
    
    // FFmpeg executable names
    private static final String FFMPEG = isWindows() ? "ffmpeg.exe" : "ffmpeg";
    private static final String FFPROBE = isWindows() ? "ffprobe.exe" : "ffprobe";
    
    // Timeout for FFmpeg operations (seconds)
    private static final int PROBE_TIMEOUT = 30;
    private static final int EXTRACT_TIMEOUT = 120;
    
    // Cache directory for extracted subtitles
    private static Path cacheDir;
    
    // Track if FFmpeg is available
    private static Boolean ffmpegAvailable = null;
    private static Boolean ffprobeAvailable = null;
    
    private SubtitleExtractor() {}
    
    /**
     * Check if FFmpeg is available on the system
     */
    public static boolean isFFmpegAvailable() {
        if (ffmpegAvailable == null) {
            ffmpegAvailable = checkExecutable(FFMPEG);
            if (ffmpegAvailable) {
                LOGGER.info(IT, "FFmpeg found and available");
            } else {
                LOGGER.warn(IT, "FFmpeg not found - embedded subtitle extraction disabled");
            }
        }
        return ffmpegAvailable;
    }
    
    /**
     * Check if FFprobe is available on the system
     */
    public static boolean isFFprobeAvailable() {
        if (ffprobeAvailable == null) {
            ffprobeAvailable = checkExecutable(FFPROBE);
            if (ffprobeAvailable) {
                LOGGER.info(IT, "FFprobe found and available");
            } else {
                LOGGER.warn(IT, "FFprobe not found - subtitle track detection disabled");
            }
        }
        return ffprobeAvailable;
    }
    
    /**
     * Set the cache directory for extracted subtitles
     */
    public static void setCacheDirectory(Path dir) {
        cacheDir = dir;
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.error(IT, "Failed to create cache directory: {}", dir, e);
        }
    }
    
    /**
     * Get or create the cache directory
     */
    private static Path getCacheDir() {
        if (cacheDir == null) {
            cacheDir = Path.of(System.getProperty("java.io.tmpdir"), "watermedia", "subtitles");
            try {
                Files.createDirectories(cacheDir);
            } catch (IOException e) {
                LOGGER.error(IT, "Failed to create cache directory", e);
            }
        }
        return cacheDir;
    }

    
    /**
     * Information about an embedded subtitle track
     */
    public static class EmbeddedTrack {
        public final int index;           // Stream index in the file
        public final int subtitleIndex;   // Subtitle-specific index (0, 1, 2...)
        public final String codec;        // Codec name (subrip, ass, hdmv_pgs_subtitle, etc.)
        public final String language;     // Language code (eng, chi, jpn, etc.)
        public final String title;        // Track title if available
        public final boolean isTextBased; // True for SRT/ASS/VTT, false for bitmap subtitles
        
        public EmbeddedTrack(int index, int subtitleIndex, String codec, String language, String title) {
            this.index = index;
            this.subtitleIndex = subtitleIndex;
            this.codec = codec;
            this.language = language;
            this.title = title;
            this.isTextBased = isTextBasedCodec(codec);
        }
        
        private static boolean isTextBasedCodec(String codec) {
            if (codec == null) return false;
            String lower = codec.toLowerCase();
            return lower.contains("subrip") || lower.contains("srt") ||
                   lower.contains("ass") || lower.contains("ssa") ||
                   lower.contains("webvtt") || lower.contains("vtt") ||
                   lower.contains("mov_text") || lower.contains("text");
        }
        
        public String getDisplayName() {
            StringBuilder sb = new StringBuilder();
            if (title != null && !title.isEmpty()) {
                sb.append(title);
            } else {
                sb.append("Track ").append(subtitleIndex + 1);
            }
            if (language != null && !language.isEmpty()) {
                sb.append(" (").append(language).append(")");
            }
            return sb.toString();
        }
        
        @Override
        public String toString() {
            return String.format("EmbeddedTrack[%d: %s, codec=%s, lang=%s, text=%b]", 
                index, getDisplayName(), codec, language, isTextBased);
        }
    }
    
    /**
     * Probe a video file for embedded subtitle tracks
     * @param videoPath path to the video file
     * @return list of embedded subtitle tracks
     */
    public static List<EmbeddedTrack> probeSubtitles(String videoPath) {
        return probeSubtitles(new File(videoPath));
    }
    
    /**
     * Probe a video file for embedded subtitle tracks
     * @param videoFile the video file
     * @return list of embedded subtitle tracks
     */
    public static List<EmbeddedTrack> probeSubtitles(File videoFile) {
        List<EmbeddedTrack> tracks = new ArrayList<>();
        
        if (!isFFprobeAvailable()) {
            LOGGER.warn(IT, "FFprobe not available, cannot probe subtitles");
            return tracks;
        }
        
        if (!videoFile.exists()) {
            LOGGER.warn(IT, "Video file not found: {}", videoFile);
            return tracks;
        }
        
        try {
            // Run ffprobe to get stream information
            ProcessBuilder pb = new ProcessBuilder(
                FFPROBE,
                "-v", "quiet",
                "-print_format", "json",
                "-show_streams",
                "-select_streams", "s",  // Only subtitle streams
                videoFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            String output = readProcessOutput(process, PROBE_TIMEOUT);
            
            if (process.waitFor(PROBE_TIMEOUT, TimeUnit.SECONDS)) {
                tracks = parseFFprobeOutput(output);
                LOGGER.info(IT, "Found {} subtitle tracks in {}", tracks.size(), videoFile.getName());
            } else {
                process.destroyForcibly();
                LOGGER.warn(IT, "FFprobe timed out for {}", videoFile);
            }
        } catch (Exception e) {
            LOGGER.error(IT, "Failed to probe subtitles: {}", videoFile, e);
        }
        
        return tracks;
    }
    
    /**
     * Probe a video URL for embedded subtitle tracks
     * Note: This may be slow for remote URLs as FFprobe needs to download headers
     */
    public static List<EmbeddedTrack> probeSubtitles(URI videoUri) {
        List<EmbeddedTrack> tracks = new ArrayList<>();
        
        String scheme = videoUri.getScheme();
        if (scheme != null && scheme.equals("file")) {
            return probeSubtitles(new File(videoUri.getPath()));
        }
        
        if (!isFFprobeAvailable()) {
            return tracks;
        }
        
        try {
            ProcessBuilder pb = new ProcessBuilder(
                FFPROBE,
                "-v", "quiet",
                "-print_format", "json",
                "-show_streams",
                "-select_streams", "s",
                videoUri.toString()
            );
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            String output = readProcessOutput(process, PROBE_TIMEOUT);
            
            if (process.waitFor(PROBE_TIMEOUT, TimeUnit.SECONDS)) {
                tracks = parseFFprobeOutput(output);
            } else {
                process.destroyForcibly();
            }
        } catch (Exception e) {
            LOGGER.error(IT, "Failed to probe subtitles from URI: {}", videoUri, e);
        }
        
        return tracks;
    }

    
    /**
     * Extract a subtitle track from a video file
     * @param videoFile the video file
     * @param track the track to extract
     * @return extracted SubtitleTrack or null on failure
     */
    public static SubtitleTrack extractSubtitle(File videoFile, EmbeddedTrack track) {
        if (!isFFmpegAvailable()) {
            LOGGER.warn(IT, "FFmpeg not available, cannot extract subtitles");
            return null;
        }
        
        if (!track.isTextBased) {
            LOGGER.warn(IT, "Cannot extract bitmap subtitle track: {}", track);
            return null;
        }
        
        try {
            // Determine output format based on codec
            String outputFormat = getOutputFormat(track.codec);
            String outputExt = "." + outputFormat;
            
            // Create cache file path
            String cacheKey = generateCacheKey(videoFile, track);
            Path outputPath = getCacheDir().resolve(cacheKey + outputExt);
            
            // Check if already cached
            if (Files.exists(outputPath)) {
                LOGGER.debug(IT, "Using cached subtitle: {}", outputPath);
                return SubtitleAPI.parseFile(outputPath.toFile());
            }
            
            // Extract subtitle using FFmpeg
            ProcessBuilder pb = new ProcessBuilder(
                FFMPEG,
                "-y",  // Overwrite output
                "-i", videoFile.getAbsolutePath(),
                "-map", "0:" + track.index,  // Select specific stream
                "-c:s", getOutputCodec(outputFormat),  // Output codec
                outputPath.toString()
            );
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            String output = readProcessOutput(process, EXTRACT_TIMEOUT);
            
            if (process.waitFor(EXTRACT_TIMEOUT, TimeUnit.SECONDS) && process.exitValue() == 0) {
                LOGGER.info(IT, "Extracted subtitle to: {}", outputPath);
                SubtitleTrack result = SubtitleAPI.parseFile(outputPath.toFile());
                if (result != null) {
                    // Update track name with original info
                    return new SubtitleTrack(track.getDisplayName(), track.language, result.getFormat()) {{
                        for (SubtitleEntry entry : result.getEntries()) {
                            addEntry(entry);
                        }
                    }};
                }
                return result;
            } else {
                process.destroyForcibly();
                LOGGER.error(IT, "FFmpeg extraction failed: {}", output);
            }
        } catch (Exception e) {
            LOGGER.error(IT, "Failed to extract subtitle track: {}", track, e);
        }
        
        return null;
    }
    
    /**
     * Extract a subtitle track from a video URI
     */
    public static SubtitleTrack extractSubtitle(URI videoUri, EmbeddedTrack track) {
        String scheme = videoUri.getScheme();
        if (scheme != null && scheme.equals("file")) {
            return extractSubtitle(new File(videoUri.getPath()), track);
        }
        
        if (!isFFmpegAvailable() || !track.isTextBased) {
            return null;
        }
        
        try {
            String outputFormat = getOutputFormat(track.codec);
            String outputExt = "." + outputFormat;
            String cacheKey = generateCacheKey(videoUri, track);
            Path outputPath = getCacheDir().resolve(cacheKey + outputExt);
            
            if (Files.exists(outputPath)) {
                return SubtitleAPI.parseFile(outputPath.toFile());
            }
            
            ProcessBuilder pb = new ProcessBuilder(
                FFMPEG,
                "-y",
                "-i", videoUri.toString(),
                "-map", "0:" + track.index,
                "-c:s", getOutputCodec(outputFormat),
                outputPath.toString()
            );
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            readProcessOutput(process, EXTRACT_TIMEOUT);
            
            if (process.waitFor(EXTRACT_TIMEOUT, TimeUnit.SECONDS) && process.exitValue() == 0) {
                SubtitleTrack result = SubtitleAPI.parseFile(outputPath.toFile());
                if (result != null) {
                    return new SubtitleTrack(track.getDisplayName(), track.language, result.getFormat()) {{
                        for (SubtitleEntry entry : result.getEntries()) {
                            addEntry(entry);
                        }
                    }};
                }
            } else {
                process.destroyForcibly();
            }
        } catch (Exception e) {
            LOGGER.error(IT, "Failed to extract subtitle from URI: {}", videoUri, e);
        }
        
        return null;
    }
    
    /**
     * Extract all text-based subtitle tracks from a video file
     */
    public static List<SubtitleTrack> extractAllSubtitles(File videoFile) {
        List<SubtitleTrack> results = new ArrayList<>();
        List<EmbeddedTrack> tracks = probeSubtitles(videoFile);
        
        for (EmbeddedTrack track : tracks) {
            if (track.isTextBased) {
                SubtitleTrack extracted = extractSubtitle(videoFile, track);
                if (extracted != null) {
                    results.add(extracted);
                }
            }
        }
        
        return results;
    }
    
    /**
     * Extract all text-based subtitle tracks from a video URI
     */
    public static List<SubtitleTrack> extractAllSubtitles(URI videoUri) {
        String scheme = videoUri.getScheme();
        if (scheme != null && scheme.equals("file")) {
            return extractAllSubtitles(new File(videoUri.getPath()));
        }
        
        List<SubtitleTrack> results = new ArrayList<>();
        List<EmbeddedTrack> tracks = probeSubtitles(videoUri);
        
        for (EmbeddedTrack track : tracks) {
            if (track.isTextBased) {
                SubtitleTrack extracted = extractSubtitle(videoUri, track);
                if (extracted != null) {
                    results.add(extracted);
                }
            }
        }
        
        return results;
    }

    
    // ==================== Helper Methods ====================
    
    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
    
    private static boolean checkExecutable(String name) {
        try {
            ProcessBuilder pb = new ProcessBuilder(name, "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (finished) {
                return process.exitValue() == 0;
            }
            process.destroyForcibly();
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static String readProcessOutput(Process process, int timeoutSeconds) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            long startTime = System.currentTimeMillis();
            long timeout = timeoutSeconds * 1000L;
            
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                if (System.currentTimeMillis() - startTime > timeout) {
                    break;
                }
            }
        }
        return output.toString();
    }
    
    /**
     * Parse FFprobe JSON output to extract subtitle track information
     */
    private static List<EmbeddedTrack> parseFFprobeOutput(String json) {
        List<EmbeddedTrack> tracks = new ArrayList<>();
        
        // Simple JSON parsing without external library
        // Looking for "streams" array with subtitle streams
        Pattern streamPattern = Pattern.compile(
            "\\{[^{}]*\"codec_type\"\\s*:\\s*\"subtitle\"[^{}]*\\}",
            Pattern.DOTALL
        );
        
        Matcher matcher = streamPattern.matcher(json);
        int subtitleIndex = 0;
        
        while (matcher.find()) {
            String streamJson = matcher.group();
            
            int index = extractInt(streamJson, "index", -1);
            String codec = extractString(streamJson, "codec_name");
            String language = extractTagString(streamJson, "language");
            String title = extractTagString(streamJson, "title");
            
            if (index >= 0) {
                tracks.add(new EmbeddedTrack(index, subtitleIndex++, codec, language, title));
            }
        }
        
        return tracks;
    }
    
    private static int extractInt(String json, String key, int defaultValue) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    private static String extractString(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    private static String extractTagString(String json, String key) {
        // Tags are nested in a "tags" object
        Pattern tagsPattern = Pattern.compile("\"tags\"\\s*:\\s*\\{([^}]*)\\}");
        Matcher tagsMatcher = tagsPattern.matcher(json);
        if (tagsMatcher.find()) {
            String tagsJson = tagsMatcher.group(1);
            return extractString("{" + tagsJson + "}", key);
        }
        return null;
    }
    
    private static String getOutputFormat(String codec) {
        if (codec == null) return "srt";
        String lower = codec.toLowerCase();
        if (lower.contains("ass") || lower.contains("ssa")) return "ass";
        if (lower.contains("webvtt") || lower.contains("vtt")) return "vtt";
        return "srt";  // Default to SRT for most text subtitles
    }
    
    private static String getOutputCodec(String format) {
        switch (format) {
            case "ass": return "ass";
            case "vtt": return "webvtt";
            default: return "srt";
        }
    }
    
    private static String generateCacheKey(File videoFile, EmbeddedTrack track) {
        String name = videoFile.getName();
        int dotIndex = name.lastIndexOf('.');
        String baseName = dotIndex > 0 ? name.substring(0, dotIndex) : name;
        long lastModified = videoFile.lastModified();
        return String.format("%s_%d_track%d", sanitizeFilename(baseName), lastModified, track.index);
    }
    
    private static String generateCacheKey(URI videoUri, EmbeddedTrack track) {
        String path = videoUri.getPath();
        if (path == null) path = videoUri.toString();
        int lastSlash = path.lastIndexOf('/');
        String name = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        int dotIndex = name.lastIndexOf('.');
        String baseName = dotIndex > 0 ? name.substring(0, dotIndex) : name;
        return String.format("%s_%d_track%d", sanitizeFilename(baseName), videoUri.hashCode(), track.index);
    }
    
    private static String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    
    /**
     * Clear the subtitle cache
     */
    public static void clearCache() {
        try {
            Path dir = getCacheDir();
            if (Files.exists(dir)) {
                Files.walk(dir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            LOGGER.warn(IT, "Failed to delete cache file: {}", path);
                        }
                    });
            }
        } catch (IOException e) {
            LOGGER.error(IT, "Failed to clear cache", e);
        }
    }
}
