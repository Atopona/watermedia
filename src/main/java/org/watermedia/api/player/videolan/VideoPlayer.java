package org.watermedia.api.player.videolan;

import org.lwjgl.opengl.GL12;
import org.watermedia.api.render.RenderAPI;
import org.watermedia.api.subtitle.SubtitleExtractor;
import org.watermedia.api.subtitle.SubtitleManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.watermedia.videolan4j.factory.MediaPlayerFactory;
import org.watermedia.videolan4j.player.base.MediaPlayer;
import org.watermedia.videolan4j.player.embedded.videosurface.callback.BufferCleanupCallback;
import org.watermedia.videolan4j.player.embedded.videosurface.callback.BufferFormat;
import org.watermedia.videolan4j.player.embedded.videosurface.callback.BufferFormatCallback;
import org.watermedia.videolan4j.player.embedded.videosurface.callback.RenderCallback;
import org.watermedia.videolan4j.tools.Chroma;

import java.awt.*;
import java.io.File;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.watermedia.WaterMedia.LOGGER;

public class VideoPlayer extends BasePlayer implements RenderCallback, BufferFormatCallback, BufferCleanupCallback {
    private static final Marker IT = MarkerManager.getMarker("VideoPlayer");

    private int width = 1;
    private int height = 1;
    private int size = width * height * 4;
    private boolean first = true;
    private final int texture;
    private final Semaphore semaphore = new Semaphore(1);
    private final Executor renderExecutor;
    private ByteBuffer[] buffers;
    
    // Subtitle support
    private final SubtitleManager subtitleManager;

    /**
     * Creates a player instance
     * @param renderExecutor executor of render thread for an async task (normally <code>Minecraft.getInstance()</code>)
     */
    public VideoPlayer(Executor renderExecutor) { this(null, renderExecutor); }

    /**
     * Creates a player instance
     * @param factory custom MediaPlayerFactory instance
     * @param renderExecutor executor of render thread for an async task (normally <code>Minecraft.getInstance()</code>)
     */
    public VideoPlayer(MediaPlayerFactory factory, Executor renderExecutor) {
        super();
        this.texture = RenderAPI.createTexture();
        this.renderExecutor = Objects.requireNonNull(renderExecutor, "Executor cannot be null");
        this.subtitleManager = new SubtitleManager();
        this.subtitleManager.setTimeSupplier(this::getTime);
        this.init(factory, this, this, this);
        if (raw() == null) {
            RenderAPI.deleteTexture(texture);
        } else {
            // HACK IN THE JANK
            this.raw().mediaPlayer().videoSurface().getVideoSurface().setSemaphore(semaphore);
        }
    }

    @Override
    public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
        renderExecutor.execute(() -> {
            RenderAPI.bindTexture(this.texture);
            try {
                if (semaphore.tryAcquire(1, TimeUnit.SECONDS)) {
                    if (buffers != null && buffers.length > 0) {
                        RenderAPI.uploadBuffer(buffers[0], texture, GL12.GL_RGBA, width, height, first);
                        first = false;
                    }
                    semaphore.release();
                } else {
                    LOGGER.error(IT, "{} took more than 1 second to synchronize with native threads", this, new InterruptedByTimeoutException());
                    if (first) { // first frames means no texture, this might cause serious problems
                        throw new IllegalStateException("Cannot handle interruption");
                    }
                    this.release();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void allocatedBuffers(ByteBuffer[] buffers) {
        this.buffers = buffers;
        this.first = true;
    }

    @Override
    public void cleanupBuffers(ByteBuffer[] buffers) {
        this.buffers = null;
    }

    @Override
    public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
        this.width = sourceWidth;
        this.height = sourceHeight;
        this.size = sourceWidth * sourceHeight * 4;
        this.first = true;

        // TODO: This might be wrong; https://wiki.videolan.org/Chroma/
        // TODO: is not wrong but is undocumented...
        //  WHY?
        return new BufferFormat(Chroma.RGBA, sourceWidth, sourceHeight);
    }

    public int size() {
        return size;
    }

    /**
     * Media Buffer width
     * @return Buffer texture width in px
     */
    public int width() { return width; }

    /**
     * Media Buffer height
     * @return Buffer texture height in px
     */
    public int height() { return height; }

    /**
     * Uploads the buffer in the current state
     *
     * <p>Ensure execution on RenderThread</p>
     * @deprecated as part of the v3 development, this was made OBSOLETE,
     * now pre-rendering its automatically done using render thread executor
     * @return gl texture identifier
     */
    @Deprecated(forRemoval = true)
    public int preRender() {
        return texture;
    }

    /**
     * Texture for OpenGL
     * @return texture id or -1 when player is in broken state
     */
    public int texture() {
        return texture;
    }

    /**
     * Returns a Dimension instance by VLC
     * @return current buffer dimensions, null if raw player isn't created or by any internal VLC error
     */
    public Dimension dimension() {
        if (raw() == null) return null;
        return raw().mediaPlayer().video().videoDimension();
    }

    // ==================== SUBTITLE METHODS ====================

    /**
     * Get the subtitle manager for this player
     * @return subtitle manager instance
     */
    public SubtitleManager getSubtitleManager() {
        return subtitleManager;
    }

    /**
     * Load subtitle from file
     * @param file subtitle file (SRT, ASS, SSA, VTT)
     * @return true if loaded successfully
     */
    public boolean loadSubtitle(File file) {
        return subtitleManager.loadSubtitle(file);
    }

    /**
     * Load subtitle from URI
     * @param uri subtitle URI
     * @return true if loaded successfully
     */
    public boolean loadSubtitle(URI uri) {
        return subtitleManager.loadSubtitle(uri);
    }

    /**
     * Load subtitle from string content
     * @param content subtitle content
     * @param name track name
     * @param format format (srt, ass, ssa, vtt)
     * @return true if loaded successfully
     */
    public boolean loadSubtitle(String content, String name, String format) {
        return subtitleManager.loadSubtitle(content, name, format);
    }

    /**
     * Get current subtitle text based on playback time
     * @return subtitle text or null if no subtitle at current time
     */
    public String getCurrentSubtitle() {
        return subtitleManager.getCurrentText();
    }

    /**
     * Get subtitle text at specific time
     * @param timeMs time in milliseconds
     * @return subtitle text or null
     */
    public String getSubtitleAt(long timeMs) {
        return subtitleManager.getTextAt(timeMs);
    }

    /**
     * Enable or disable subtitle display
     * @param enabled true to enable
     */
    public void setSubtitleEnabled(boolean enabled) {
        subtitleManager.setEnabled(enabled);
    }

    /**
     * Check if subtitles are enabled
     */
    public boolean isSubtitleEnabled() {
        return subtitleManager.isEnabled();
    }

    /**
     * Get number of loaded subtitle tracks
     */
    public int getSubtitleTrackCount() {
        return subtitleManager.getTrackCount();
    }

    /**
     * Set active subtitle track by index
     * @param index track index (0-based), -1 to disable
     */
    public void setSubtitleTrack(int index) {
        subtitleManager.setActiveTrack(index);
    }

    /**
     * Get active subtitle track index
     * @return index or -1 if no active track
     */
    public int getSubtitleTrack() {
        return subtitleManager.getActiveTrackIndex();
    }

    /**
     * Check if any subtitles are loaded
     */
    public boolean hasSubtitles() {
        return subtitleManager.hasSubtitles();
    }

    /**
     * Auto-load subtitles for a video file (looks for .srt, .ass, etc. next to video)
     * @param videoFile the video file
     * @return number of subtitles loaded
     */
    public int autoLoadSubtitles(File videoFile) {
        return subtitleManager.autoLoadSubtitles(videoFile);
    }

    /**
     * Extract and load embedded subtitles from the current video
     * Requires FFmpeg to be installed on the system
     * @return number of subtitle tracks extracted and loaded
     */
    public int extractEmbeddedSubtitles() {
        return subtitleManager.extractEmbeddedSubtitles(url);
    }

    /**
     * Extract and load embedded subtitles from a video file
     * Requires FFmpeg to be installed on the system
     * @param videoFile the video file
     * @return number of subtitle tracks extracted and loaded
     */
    public int extractEmbeddedSubtitles(File videoFile) {
        return subtitleManager.extractEmbeddedSubtitles(videoFile);
    }

    /**
     * Check if FFmpeg is available for subtitle extraction
     */
    public boolean isFFmpegAvailable() {
        return SubtitleExtractor.isFFmpegAvailable();
    }

    /**
     * Get list of embedded subtitle tracks in the current video
     * Requires FFprobe to be installed
     */
    public java.util.List<SubtitleExtractor.EmbeddedTrack> getEmbeddedSubtitleTracks() {
        if (url == null) return java.util.Collections.emptyList();
        return SubtitleExtractor.probeSubtitles(url);
    }

    /**
     * Releases all resources of the player
     */
    @Override
    public void release() {
        subtitleManager.clear();
        renderExecutor.execute(() -> RenderAPI.deleteTexture(texture));
        super.release();
    }
}