package org.watermedia.api.subtitle;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.watermedia.WaterMedia.LOGGER;

/**
 * Manages subtitle tracks for a video player
 * Handles loading, switching, and querying subtitles
 */
public class SubtitleManager {
    
    private static final Marker IT = MarkerManager.getMarker("SubtitleManager");
    
    private final List<SubtitleTrack> tracks = new ArrayList<>();
    private SubtitleTrack activeTrack = null;
    private boolean enabled = true;
    private Supplier<Long> timeSupplier;
    
    // Cached current subtitle for performance
    private String cachedText = null;
    private long cachedTime = -1;
    private static final long CACHE_THRESHOLD = 50; // ms

    public SubtitleManager() {
    }

    /**
     * Set the time supplier (usually player.getTime())
     * @param timeSupplier function that returns current playback time in ms
     */
    public void setTimeSupplier(Supplier<Long> timeSupplier) {
        this.timeSupplier = timeSupplier;
    }

    /**
     * Load subtitle from file
     * @param file subtitle file
     * @return true if loaded successfully
     */
    public boolean loadSubtitle(File file) {
        SubtitleTrack track = SubtitleAPI.parseFile(file);
        if (track != null) {
            tracks.add(track);
            LOGGER.info(IT, "Loaded subtitle: {} ({} entries)", file.getName(), track.getEntryCount());
            
            // Auto-select first track if none selected
            if (activeTrack == null) {
                activeTrack = track;
            }
            return true;
        }
        return false;
    }

    /**
     * Load subtitle from URI
     * @param uri subtitle URI
     * @return true if loaded successfully
     */
    public boolean loadSubtitle(URI uri) {
        SubtitleTrack track = SubtitleAPI.parseUri(uri);
        if (track != null) {
            tracks.add(track);
            LOGGER.info(IT, "Loaded subtitle from URI: {} ({} entries)", uri, track.getEntryCount());
            
            if (activeTrack == null) {
                activeTrack = track;
            }
            return true;
        }
        return false;
    }

    /**
     * Load subtitle from string content
     * @param content subtitle content
     * @param name track name
     * @param format format (srt, ass, ssa, vtt)
     * @return true if loaded successfully
     */
    public boolean loadSubtitle(String content, String name, String format) {
        SubtitleTrack track = SubtitleAPI.parseString(content, name, format);
        if (track != null) {
            tracks.add(track);
            LOGGER.info(IT, "Loaded subtitle: {} ({} entries)", name, track.getEntryCount());
            
            if (activeTrack == null) {
                activeTrack = track;
            }
            return true;
        }
        return false;
    }

    /**
     * Add an already parsed track
     * @param track subtitle track
     */
    public void addTrack(SubtitleTrack track) {
        if (track != null) {
            tracks.add(track);
            if (activeTrack == null) {
                activeTrack = track;
            }
        }
    }

    /**
     * Get all loaded tracks
     */
    public List<SubtitleTrack> getTracks() {
        return Collections.unmodifiableList(tracks);
    }

    /**
     * Get number of loaded tracks
     */
    public int getTrackCount() {
        return tracks.size();
    }

    /**
     * Get active track
     */
    public SubtitleTrack getActiveTrack() {
        return activeTrack;
    }

    /**
     * Set active track by index
     * @param index track index (0-based), -1 to disable
     */
    public void setActiveTrack(int index) {
        if (index < 0 || index >= tracks.size()) {
            activeTrack = null;
        } else {
            activeTrack = tracks.get(index);
        }
        clearCache();
    }

    /**
     * Set active track directly
     * @param track the track to activate, null to disable
     */
    public void setActiveTrack(SubtitleTrack track) {
        this.activeTrack = track;
        clearCache();
    }

    /**
     * Get active track index
     * @return index or -1 if no active track
     */
    public int getActiveTrackIndex() {
        return tracks.indexOf(activeTrack);
    }

    /**
     * Enable or disable subtitle display
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Check if subtitles are enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get current subtitle text based on playback time
     * @return subtitle text or null if no subtitle at current time
     */
    public String getCurrentText() {
        if (!enabled || activeTrack == null || timeSupplier == null) {
            return null;
        }
        
        long time = timeSupplier.get();
        
        // Use cache if time hasn't changed much
        if (Math.abs(time - cachedTime) < CACHE_THRESHOLD && cachedTime >= 0) {
            return cachedText;
        }
        
        // Update cache
        cachedTime = time;
        cachedText = activeTrack.getTextAt(time);
        return cachedText;
    }

    /**
     * Get subtitle text at specific time
     * @param timeMs time in milliseconds
     * @return subtitle text or null
     */
    public String getTextAt(long timeMs) {
        if (!enabled || activeTrack == null) {
            return null;
        }
        return activeTrack.getTextAt(timeMs);
    }

    /**
     * Get current subtitle entry
     * @return subtitle entry or null
     */
    public SubtitleEntry getCurrentEntry() {
        if (!enabled || activeTrack == null || timeSupplier == null) {
            return null;
        }
        return activeTrack.findEntryAt(timeSupplier.get());
    }

    /**
     * Get all active entries at current time (for overlapping subtitles)
     */
    public List<SubtitleEntry> getCurrentEntries() {
        if (!enabled || activeTrack == null || timeSupplier == null) {
            return Collections.emptyList();
        }
        return activeTrack.getEntriesAt(timeSupplier.get());
    }

    /**
     * Clear all tracks
     */
    public void clear() {
        tracks.clear();
        activeTrack = null;
        clearCache();
    }

    /**
     * Remove a specific track
     */
    public void removeTrack(int index) {
        if (index >= 0 && index < tracks.size()) {
            SubtitleTrack removed = tracks.remove(index);
            if (removed == activeTrack) {
                activeTrack = tracks.isEmpty() ? null : tracks.get(0);
            }
            clearCache();
        }
    }

    /**
     * Clear the subtitle cache
     */
    private void clearCache() {
        cachedText = null;
        cachedTime = -1;
    }

    /**
     * Check if any subtitles are loaded
     */
    public boolean hasSubtitles() {
        return !tracks.isEmpty();
    }

    /**
     * Auto-load subtitles for a video file
     * @param videoFile the video file
     * @return number of subtitles loaded
     */
    public int autoLoadSubtitles(File videoFile) {
        List<File> subtitleFiles = SubtitleAPI.findSubtitlesForVideo(videoFile);
        int loaded = 0;
        for (File subFile : subtitleFiles) {
            if (loadSubtitle(subFile)) {
                loaded++;
            }
        }
        return loaded;
    }
}
