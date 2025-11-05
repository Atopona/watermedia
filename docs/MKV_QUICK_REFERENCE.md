# MKV 快速参考

## 🚀 5 分钟快速上手

### 1. 检查 VLC 是否可用
```java
if (PlayerAPI.isReady()) {
    System.out.println("✅ VLC 已就绪，可以播放 MKV");
} else {
    System.out.println("❌ VLC 未安装");
}
```

### 2. 播放本地 MKV 文件
```java
VideoPlayer player = new VideoPlayer(PlayerAPI.getFactory());
player.start(new File("video.mkv").toURI(), false, 100);
```

### 3. 播放在线 MKV
```java
VideoPlayer player = new VideoPlayer(PlayerAPI.getFactory());
player.start(NetworkAPI.parseURI("https://example.com/video.mkv"), false, 100);
```

### 4. 控制播放
```java
player.setPaused(true);           // 暂停
player.setPaused(false);          // 继续
player.setTime(30000);            // 跳转到 30 秒
player.setVolume(50);             // 音量 50%
```

### 5. 获取信息
```java
int width = player.getWidth();           // 宽度
int height = player.getHeight();         // 高度
long duration = player.getDuration();    // 时长（毫秒）
long currentTime = player.getTime();     // 当前时间
boolean playing = player.isPlaying();    // 是否播放中
```

### 6. 切换音轨
```java
int trackCount = player.raw().mediaPlayer().audio().trackCount();
player.raw().mediaPlayer().audio().setTrack(1);  // 切换到音轨 1
```

### 7. 启用字幕
```java
int subCount = player.raw().mediaPlayer().subpictures().trackCount();
player.raw().mediaPlayer().subpictures().setTrack(1);  // 启用字幕轨道 1
```

### 8. 释放资源
```java
player.release();  // 停止播放并释放资源
```

## 📋 常用代码片段

### 完整播放流程
```java
// 初始化
VideoPlayer player = new VideoPlayer(PlayerAPI.getFactory());

// 开始播放
File mkvFile = new File("movie.mkv");
player.start(mkvFile.toURI(), false, 100);

// 等待加载
Thread.sleep(500);

// 获取信息
System.out.println("分辨率: " + player.getWidth() + "x" + player.getHeight());
System.out.println("时长: " + (player.getDuration() / 1000) + " 秒");

// 在游戏/应用渲染循环中
while (running) {
    player.preRender();              // 准备下一帧
    int textureId = player.getTexture();  // 获取 OpenGL 纹理 ID
    // 使用 textureId 渲染到屏幕...
}

// 清理
player.release();
```

### 仅播放音频
```java
MusicPlayer player = new MusicPlayer(PlayerAPI.getFactorySoundOnly());
player.start(new File("audio.mkv").toURI(), false, 100);
```

### 循环播放
```java
VideoPlayer player = new VideoPlayer(PlayerAPI.getFactory());
player.start(new File("loop.mkv").toURI(), true, 100);  // true = 循环
```

### 错误处理
```java
try {
    VideoPlayer player = new VideoPlayer(PlayerAPI.getFactory());
    player.start(new File("video.mkv").toURI(), false, 100);
} catch (Exception e) {
    System.err.println("播放失败: " + e.getMessage());
    e.printStackTrace();
}
```

## 🎯 支持的 MKV 编解码器

### 视频
- H.264 / AVC ⭐ (推荐)
- H.265 / HEVC
- VP8
- VP9
- AV1
- MPEG-4

### 音频
- AAC ⭐ (推荐)
- MP3
- Opus
- Vorbis
- FLAC
- AC3
- DTS

### 字幕
- SRT
- ASS
- SSA
- WebVTT

## ⚡ 性能提示

1. **使用 H.264** - 最好的兼容性和性能
2. **启用硬件加速** - 在 VLC 设置中配置
3. **合理的分辨率** - 1080p 通常是最佳选择
4. **预加载** - 给媒体一些时间加载信息

## 🐛 常见问题

### 问题: 无法播放
```java
// 检查 VLC
if (!PlayerAPI.isReady()) {
    System.err.println("VLC 未安装或未找到");
}

// 检查文件
File file = new File("video.mkv");
if (!file.exists()) {
    System.err.println("文件不存在");
}
```

### 问题: 有音频但无视频
```java
// 检查是否使用了正确的播放器
// 使用 VideoPlayer 而不是 MusicPlayer
VideoPlayer player = new VideoPlayer(PlayerAPI.getFactory());
```

### 问题: 性能差
```java
// 使用默认工厂（已优化）
MediaPlayerFactory factory = PlayerAPI.getFactory();

// 或创建自定义工厂
String[] args = {"--avcodec-hw=any"};  // 启用硬件加速
PlayerAPI.registerFactory("custom", args);
```

## 📚 更多资源

- **完整文档**: [MKV_SUPPORT.md](../MKV_SUPPORT.md)
- **所有格式**: [VIDEO_FORMATS.md](VIDEO_FORMATS.md)
- **示例代码**: [examples/MKVPlaybackExample.java](../examples/MKVPlaybackExample.java)
- **测试代码**: [src/test/java/org/watermedia/api/player/MKVSupportTest.java](../src/test/java/org/watermedia/api/player/MKVSupportTest.java)

## 💡 最佳实践

1. ✅ 始终检查 `PlayerAPI.isReady()`
2. ✅ 使用 try-catch 处理异常
3. ✅ 播放完成后调用 `player.release()`
4. ✅ 给媒体时间加载（Thread.sleep 或异步）
5. ✅ 在渲染线程中调用 `preRender()`
6. ✅ 使用合适的播放器类型（VideoPlayer vs MusicPlayer）

---

**快速参考版本**: 1.0
**WaterMedia 版本**: 2.1.37+
