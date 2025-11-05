package examples;

import org.watermedia.api.player.videolan.VideoPlayer;
import org.watermedia.api.player.videolan.MusicPlayer;
import org.watermedia.api.player.PlayerAPI;
import org.watermedia.api.network.NetworkAPI;

import java.io.File;
import java.net.URI;

/**
 * Example demonstrating how to play MKV files using WaterMedia
 * 
 * MKV (Matroska) is a flexible, open-standard video container format that can hold
 * unlimited numbers of video, audio, picture, or subtitle tracks in one file.
 */
public class MKVPlaybackExample {

    /**
     * Example 1: Play a local MKV file with video
     */
    public static void playLocalMKVVideo() throws Exception {
        // Check if VLC is available
        if (!PlayerAPI.isReady()) {
            System.err.println("VLC is not available. Please install VLC or check your installation.");
            return;
        }

        // Create a video player instance
        VideoPlayer player = new VideoPlayer(PlayerAPI.getFactory());

        // Load a local MKV file
        File mkvFile = new File("path/to/your/video.mkv");
        if (!mkvFile.exists()) {
            System.err.println("MKV file not found: " + mkvFile.getAbsolutePath());
            return;
        }

        URI uri = mkvFile.toURI();
        
        // Start playback
        // Parameters: URI, loop (false), volume (100%)
        player.start(uri, false, 100);

        System.out.println("Playing MKV video: " + mkvFile.getName());
        System.out.println("Duration: " + player.getDuration() + "ms");
        System.out.println("Resolution: " + player.getWidth() + "x" + player.getHeight());

        // In your render loop, you would call:
        // player.preRender(); // Prepare the next frame
        // int textureId = player.getTexture(); // Get OpenGL texture ID
        // Then render the texture to your screen

        // When done, release the player
        // player.release();
    }

    /**
     * Example 2: Play an online MKV file
     */
    public static void playOnlineMKV() throws Exception {
        if (!PlayerAPI.isReady()) {
            System.err.println("VLC is not available.");
            return;
        }

        VideoPlayer player = new VideoPlayer(PlayerAPI.getFactory());

        // Parse the URL (WaterMedia will handle URL patching if needed)
        URI uri = NetworkAPI.parseURI("https://example.com/video.mkv");

        // Start playback with loop enabled
        player.start(uri, true, 80); // 80% volume

        System.out.println("Streaming MKV from: " + uri);
    }

    /**
     * Example 3: Play MKV audio-only (music player)
     */
    public static void playMKVAudio() throws Exception {
        if (!PlayerAPI.isReady()) {
            System.err.println("VLC is not available.");
            return;
        }

        // Use MusicPlayer for audio-only playback (more efficient)
        MusicPlayer player = new MusicPlayer(PlayerAPI.getFactorySoundOnly());

        File audioFile = new File("path/to/audio.mkv");
        URI uri = audioFile.toURI();

        player.start(uri, false, 100);

        System.out.println("Playing MKV audio: " + audioFile.getName());
    }

    /**
     * Example 4: Advanced MKV playback with subtitle and audio track selection
     */
    public static void playMKVWithTracks() throws Exception {
        if (!PlayerAPI.isReady()) {
            System.err.println("VLC is not available.");
            return;
        }

        VideoPlayer player = new VideoPlayer(PlayerAPI.getFactory());
        File mkvFile = new File("path/to/movie.mkv");
        
        player.start(mkvFile.toURI(), false, 100);

        // Wait a moment for media info to load
        Thread.sleep(500);

        // Get available audio tracks
        int audioTrackCount = player.raw().mediaPlayer().audio().trackCount();
        System.out.println("Available audio tracks: " + audioTrackCount);

        // List audio tracks
        for (int i = 0; i < audioTrackCount; i++) {
            String trackDescription = player.raw().mediaPlayer().audio().trackDescription(i);
            System.out.println("  Track " + i + ": " + trackDescription);
        }

        // Switch to a different audio track (e.g., track 1)
        if (audioTrackCount > 1) {
            player.raw().mediaPlayer().audio().setTrack(1);
            System.out.println("Switched to audio track 1");
        }

        // Get available subtitle tracks
        int subtitleCount = player.raw().mediaPlayer().subpictures().trackCount();
        System.out.println("Available subtitle tracks: " + subtitleCount);

        // Enable subtitles (track 1)
        if (subtitleCount > 0) {
            player.raw().mediaPlayer().subpictures().setTrack(1);
            System.out.println("Enabled subtitle track 1");
        }

        // Get video information
        System.out.println("\nVideo Information:");
        System.out.println("  Resolution: " + player.getWidth() + "x" + player.getHeight());
        System.out.println("  Duration: " + (player.getDuration() / 1000) + " seconds");
        System.out.println("  FPS: " + player.raw().mediaPlayer().status().fps());
    }

    /**
     * Example 5: Control playback
     */
    public static void controlPlayback() throws Exception {
        if (!PlayerAPI.isReady()) {
            System.err.println("VLC is not available.");
            return;
        }

        VideoPlayer player = new VideoPlayer(PlayerAPI.getFactory());
        File mkvFile = new File("path/to/video.mkv");
        
        player.start(mkvFile.toURI(), false, 100);

        // Pause playback
        player.setPaused(true);
        System.out.println("Paused");

        // Resume playback
        Thread.sleep(2000);
        player.setPaused(false);
        System.out.println("Resumed");

        // Seek to 30 seconds
        player.setTime(30000); // Time in milliseconds
        System.out.println("Seeked to 30 seconds");

        // Adjust volume
        player.setVolume(50); // 50%
        System.out.println("Volume set to 50%");

        // Check playback state
        System.out.println("Is playing: " + player.isPlaying());
        System.out.println("Is paused: " + player.isPaused());
        System.out.println("Current time: " + (player.getTime() / 1000) + "s");

        // Stop and release
        player.release();
        System.out.println("Playback stopped and resources released");
    }

    /**
     * Example 6: Handle MKV with high resolution (4K)
     */
    public static void play4KMKV() throws Exception {
        if (!PlayerAPI.isReady()) {
            System.err.println("VLC is not available.");
            return;
        }

        // For high-resolution videos, you might want to use custom VLC arguments
        // This is already configured in PlayerAPI, but you can create custom factories
        VideoPlayer player = new VideoPlayer(PlayerAPI.getFactory());

        File uhd4kFile = new File("path/to/4k_video.mkv");
        player.start(uhd4kFile.toURI(), false, 100);

        System.out.println("Playing 4K MKV video");
        System.out.println("Resolution: " + player.getWidth() + "x" + player.getHeight());
        
        // Note: 4K playback requires sufficient system resources
        // Consider hardware acceleration settings in VLC
    }

    /**
     * Main method to run examples
     */
    public static void main(String[] args) {
        try {
            System.out.println("=== WaterMedia MKV Playback Examples ===\n");

            // Uncomment the example you want to run:
            
            // playLocalMKVVideo();
            // playOnlineMKV();
            // playMKVAudio();
            // playMKVWithTracks();
            // controlPlayback();
            // play4KMKV();

            System.out.println("\nExamples completed!");

        } catch (Exception e) {
            System.err.println("Error during playback: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
