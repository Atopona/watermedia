# 视频格式支持指南

## 📋 概述

WaterMedia 通过 LibVLC 提供广泛的视频格式支持。本文档列出了所有支持的格式及其特性。

## ✅ 完全支持的容器格式

| 格式 | 扩展名 | MIME 类型 | 说明 |
|------|--------|-----------|------|
| **Matroska** | `.mkv`, `.mka`, `.mks` | `video/x-matroska`, `audio/x-matroska` | 开源容器，支持多音轨、字幕、章节 |
| **MP4** | `.mp4`, `.m4v`, `.m4a` | `video/mp4`, `audio/mp4` | 最常用的格式，广泛兼容 |
| **AVI** | `.avi` | `video/x-msvideo` | 传统格式，广泛支持 |
| **MOV** | `.mov` | `video/quicktime` | QuickTime 格式 |
| **WebM** | `.webm` | `video/webm` | 开源格式，适合网络流媒体 |
| **FLV** | `.flv` | `video/x-flv` | Flash 视频格式 |
| **WMV** | `.wmv` | `video/x-ms-wmv` | Windows Media Video |
| **MPEG** | `.mpg`, `.mpeg`, `.m2v` | `video/mpeg` | MPEG-1/2 格式 |
| **3GP** | `.3gp`, `.3g2` | `video/3gpp` | 移动设备格式 |
| **OGG** | `.ogv`, `.ogg` | `video/ogg` | Ogg 容器 |
| **TS** | `.ts`, `.m2ts`, `.mts` | `video/mp2t` | MPEG 传输流 |

## 🎬 支持的视频编解码器

### 常用编解码器
- **H.264 / AVC** - 最广泛使用的编解码器
- **H.265 / HEVC** - 高效率视频编码，更好的压缩率
- **VP8** - Google 开源编解码器
- **VP9** - VP8 的继任者，用于 WebM
- **AV1** - 新一代开源编解码器
- **MPEG-4** - 传统 MPEG-4 Part 2
- **MPEG-2** - DVD 和广播标准
- **Theora** - Ogg 视频编解码器
- **WMV** - Windows Media Video 编解码器

### 专业编解码器
- **ProRes** - Apple 专业视频编解码器
- **DNxHD / DNxHR** - Avid 专业编解码器
- **MJPEG** - Motion JPEG

## 🔊 支持的音频编解码器

- **AAC** - 高级音频编码
- **MP3** - MPEG Audio Layer 3
- **Opus** - 现代开源音频编解码器
- **Vorbis** - Ogg 音频编解码器
- **FLAC** - 无损音频编解码器
- **AC3 / E-AC3** - Dolby Digital
- **DTS** - Digital Theater Systems
- **PCM** - 未压缩音频
- **WMA** - Windows Media Audio

## 📺 流媒体格式

| 格式 | 扩展名 | MIME 类型 | 说明 |
|------|--------|-----------|------|
| **HLS** | `.m3u8` | `application/vnd.apple.mpegurl` | HTTP Live Streaming |
| **DASH** | `.mpd` | `application/dash+xml` | Dynamic Adaptive Streaming |
| **RTSP** | - | `rtsp://` | Real Time Streaming Protocol |
| **RTMP** | - | `rtmp://` | Real-Time Messaging Protocol |
| **HTTP** | - | `http://`, `https://` | HTTP 流媒体 |

## 🎯 格式推荐

### 最佳兼容性
```
容器: MP4
视频: H.264 (Main/High Profile)
音频: AAC
```

### 最佳质量
```
容器: MKV
视频: H.265/HEVC 或 AV1
音频: FLAC 或 Opus
```

### 网络流媒体
```
容器: WebM
视频: VP9
音频: Opus
```

### 专业用途
```
容器: MKV 或 MOV
视频: ProRes 或 DNxHR
音频: PCM 或 FLAC
```

## 🔧 使用示例

### 播放不同格式

```java
import org.watermedia.api.player.videolan.VideoPlayer;
import org.watermedia.api.player.PlayerAPI;
import java.io.File;

// MKV 文件
VideoPlayer mkvPlayer = new VideoPlayer(PlayerAPI.getFactory());
mkvPlayer.start(new File("video.mkv").toURI(), false, 100);

// MP4 文件
VideoPlayer mp4Player = new VideoPlayer(PlayerAPI.getFactory());
mp4Player.start(new File("video.mp4").toURI(), false, 100);

// WebM 文件
VideoPlayer webmPlayer = new VideoPlayer(PlayerAPI.getFactory());
webmPlayer.start(new File("video.webm").toURI(), false, 100);

// HLS 流
VideoPlayer hlsPlayer = new VideoPlayer(PlayerAPI.getFactory());
hlsPlayer.start(new URI("https://example.com/stream.m3u8"), false, 100);
```

## ⚠️ 限制和注意事项

### 编解码器依赖
某些编解码器可能需要特定的 VLC 版本或插件：
- **H.265/HEVC**: 需要 VLC 2.2.0+
- **AV1**: 需要 VLC 3.0.0+
- **HDR**: 需要 VLC 3.0.0+ 和支持的显示器

### 性能考虑
- **4K/8K 视频**: 需要强大的 CPU/GPU
- **高比特率**: 可能需要 SSD 存储
- **硬件加速**: 在 VLC 设置中启用以获得更好性能

### 平台差异
- **Windows**: 完全支持，包含所有编解码器
- **macOS**: 需要手动安装 VLC
- **Linux**: 需要手动安装 VLC，某些编解码器可能需要额外包

## 🧪 测试格式支持

```java
import org.watermedia.api.player.PlayerAPI;

public class FormatTest {
    public static void testFormat(String filePath) {
        if (!PlayerAPI.isReady()) {
            System.err.println("VLC not available");
            return;
        }

        VideoPlayer player = new VideoPlayer(PlayerAPI.getFactory());
        try {
            player.start(new File(filePath).toURI(), false, 100);
            
            // 等待媒体信息加载
            Thread.sleep(500);
            
            System.out.println("Format: Supported");
            System.out.println("Resolution: " + player.getWidth() + "x" + player.getHeight());
            System.out.println("Duration: " + player.getDuration() + "ms");
            System.out.println("FPS: " + player.raw().mediaPlayer().status().fps());
            
            player.release();
        } catch (Exception e) {
            System.err.println("Format: Not supported or error - " + e.getMessage());
        }
    }
}
```

## 📚 相关资源

- [VLC 支持的格式完整列表](https://wiki.videolan.org/VLC_Features_Formats/)
- [MKV 格式详细文档](MKV_SUPPORT.md)
- [LibVLC 文档](https://www.videolan.org/developers/vlc/doc/doxygen/html/)
- [视频编解码器对比](https://en.wikipedia.org/wiki/Comparison_of_video_codecs)

## 🆘 故障排除

### 视频无法播放
1. 检查 VLC 是否正确安装
2. 验证文件是否损坏
3. 检查编解码器是否受支持
4. 查看 WaterMedia 日志获取详细错误

### 音频但无视频
- 可能是不支持的视频编解码器
- 尝试使用 VLC 播放器直接测试文件

### 视频但无音频
- 可能是不支持的音频编解码器
- 检查音频轨道是否存在

### 性能问题
- 启用硬件加速
- 降低视频分辨率
- 使用更高效的编解码器（如 H.264）

---

**最后更新**: 2024
**WaterMedia 版本**: 2.1.37+
