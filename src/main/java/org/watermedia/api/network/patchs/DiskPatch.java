package org.watermedia.api.network.patchs;

import org.watermedia.WaterMedia;

import java.io.File;
import java.net.URI;

public class DiskPatch extends AbstractPatch {
    @Override
    public String platform() {
        return "Local";
    }

    @Override
    public boolean isValid(URI uri) {
        String scheme = uri.getScheme();
        return scheme != null && scheme.equals("water");
    }

    @Override
    public Result patch(URI uri, Quality prefQuality) throws FixingURLException {
        super.patch(uri, prefQuality);

        try {

            if (uri.getScheme().equals("water")) {
                URI resolvedUri;
                switch (uri.getHost()) {
                    case "local":
                        resolvedUri = new File("").toPath().resolve(uri.getPath().substring(1)).toUri();
                        return new AbstractPatch.Result(resolvedUri, isVideoOrAudioFile(resolvedUri), false);
                    case "user":
                        break; // NO-OP for now, requires security checks
                    case "temp":
                        resolvedUri = WaterMedia.getLoader().tempDir().resolve(uri.getPath().substring(1)).toUri();
                        return new AbstractPatch.Result(resolvedUri, isVideoOrAudioFile(resolvedUri), false);
                }
            }

            throw new IllegalArgumentException("invalid water protocol");
        } catch (Exception e) {
            throw new FixingURLException(uri, e);
        }
    }
    
    /**
     * Check if URI points to a video or audio file based on file extension
     */
    private boolean isVideoOrAudioFile(URI uri) {
        String path = uri.getPath();
        if (path == null) return false;
        
        String lowerPath = path.toLowerCase();
        
        // Check for video file extensions
        if (lowerPath.endsWith(".mkv") || lowerPath.endsWith(".mp4") || 
            lowerPath.endsWith(".avi") || lowerPath.endsWith(".mov") || 
            lowerPath.endsWith(".webm") || lowerPath.endsWith(".flv") ||
            lowerPath.endsWith(".wmv") || lowerPath.endsWith(".m4v") ||
            lowerPath.endsWith(".mpg") || lowerPath.endsWith(".mpeg") ||
            lowerPath.endsWith(".m3u8") || lowerPath.endsWith(".m3u") ||
            lowerPath.endsWith(".ts") || lowerPath.endsWith(".m2ts") ||
            lowerPath.endsWith(".3gp") || lowerPath.endsWith(".ogv")) {
            return true;
        }
        
        // Check for audio file extensions
        return lowerPath.endsWith(".mp3") || lowerPath.endsWith(".wav") ||
                lowerPath.endsWith(".ogg") || lowerPath.endsWith(".flac") ||
                lowerPath.endsWith(".aac") || lowerPath.endsWith(".m4a") ||
                lowerPath.endsWith(".wma") || lowerPath.endsWith(".opus");
    }
}
