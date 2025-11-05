# MKV 视频格式支持说明

## ✅ 支持状态

WaterMedia **已完全支持 MKV 格式**！

## 🔧 技术实现

### 1. LibVLC 原生支持
WaterMedia 使用 **LibVLC** 作为视频后端，LibVLC 原生支持以下容器格式：
- **MKV** (Matroska) - `video/x-matroska`
- MP4
- AVI
- MOV
- WebM
- FLV
- 以及更多格式...

### 2. MIME 类型识别
在 `ImageFetch.java` 中，已添加对 MKV MIME 类型的明确支持：

```java
private static final String[] VID_MIMETYPES = new String[] { 
    "video",                        // 通用视频类型
    "audio",                        // 音频类型
    "application/vnd.apple.mpegurl", // HLS 流
    "application/x-mpegurl",        // M3U8 流
    "video/x-matroska"              // MKV 格式（明确支持）
};
```

### 3. 播放器支持
- **VideoPlayer**: 支持带视频轨道的 MKV 文件
- **MusicPlayer**: 支持仅音频的 MKV 文件

## 📝 使用示例

### 播放本地 MKV 文件
```java
import org.watermedia.api.player.videolan.VideoPlayer;
import org.watermedia.api.player.PlayerAPI;
import java.net.URI;
import java.io.File;

// 创建播放器
VideoPlayer player = new VideoPlayer(PlayerAPI.getFactory());

// 加载 MKV 文件
File mkvFile = new File("path/to/video.mkv");
URI uri = mkvFile.toURI();
player.start(uri, false, 100); // URI, 不循环, 音量100%

// 渲染视频帧
player.preRender(); // 准备渲染
// 在渲染循环中调用 player.getTexture() 获取纹理ID
```

### 播放在线 MKV 文件
```java
import org.watermedia.api.network.NetworkAPI;

// 解析 URL
URI uri = NetworkAPI.parseURI("https://example.com/video.mkv");

// 使用播放器播放
VideoPlayer player = new VideoPlayer(PlayerAPI.getFactory());
player.start(uri, false, 100);
```

## 🎯 支持的 MKV 特性

### ✅ 已支持
- [x] 视频编解码器（H.264, H.265/HEVC, VP8, VP9, AV1 等）
- [x] 音频编解码器（AAC, MP3, Opus, Vorbis, FLAC 等）
- [x] 多音轨
- [x] 字幕轨道（SRT, ASS, SSA 等）
- [x] 章节信息
- [x] 元数据
- [x] 高分辨率视频（1080p, 4K, 8K）
- [x] 可变帧率（VFR）
- [x] HDR 视频（取决于 VLC 版本）

### ⚠️ 限制
- 字幕渲染需要通过 VLC 的字幕 API 单独处理
- 某些高级 MKV 特性可能需要特定版本的 VLC

## 🧪 测试

### 验证 MKV 支持
```java
import org.watermedia.api.player.PlayerAPI;

// 检查 VLC 是否已加载
if (PlayerAPI.isReady()) {
    System.out.println("VLC 已就绪，支持 MKV 格式");
} else {
    System.out.println("VLC 未加载");
}
```

### 测试文件格式
推荐使用以下测试文件：
- 标准 H.264 + AAC 编码的 MKV
- 高分辨率 HEVC 编码的 MKV
- 包含多音轨的 MKV
- 包含字幕的 MKV

## 📋 常见问题

### Q: 为什么我的 MKV 文件无法播放？
A: 检查以下几点：
1. 确保 VLC 已正确安装（Windows x64 自动包含）
2. 检查文件是否损坏
3. 验证编解码器是否被 VLC 支持
4. 查看日志输出获取详细错误信息

### Q: 如何处理 MKV 中的字幕？
A: 使用 VLC 的字幕 API：
```java
// 获取字幕轨道
int subtitleCount = player.raw().mediaPlayer().subpictures().trackCount();

// 设置字幕轨道
player.raw().mediaPlayer().subpictures().setTrack(trackId);
```

### Q: MKV 文件的性能如何？
A: MKV 是一个容器格式，性能取决于：
- 视频编解码器（H.264 通常性能最好）
- 分辨率和比特率
- 系统硬件（GPU 加速）
- VLC 版本和配置

## 🔗 相关资源

- [Matroska 官方网站](https://www.matroska.org/)
- [VLC 支持的格式列表](https://wiki.videolan.org/VLC_Features_Formats/)
- [WaterMedia GitHub](https://github.com/WaterMediaTeam/watermedia)

## 📅 更新日志

### v2.1.36+
- ✨ 明确添加 `video/x-matroska` MIME 类型支持
- 📝 添加 MKV 格式支持文档

---

**注意**: MKV 支持是通过 LibVLC 实现的，因此所有 LibVLC 支持的 MKV 特性都可以在 WaterMedia 中使用。
