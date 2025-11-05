# Bug 修复: ImageFetch 视频文件检测

## 🐛 问题描述

### 错误日志
```
[ImageFetch-Worker-1/ERROR] [watermedia/ImageAPI]: Invalid image source from 'file:/C:/Users/20901/Downloads/1.mkv'
org.watermedia.api.image.ImageFetch$NoImageException: null
```

### 问题分析
当尝试加载本地 MKV 文件时，`ImageFetch` 会错误地尝试将其作为图像处理，导致抛出 `NoImageException`。

### 根本原因
问题出现在两个地方：

1. **NetworkAPI.patch()** - 对于本地文件（`file://` 协议），没有匹配的 URL patcher，返回 `assumeVideo=false`
2. **ImageFetch.java** - 代码依赖 `URLConnection.getContentType()` 来判断文件类型，但对于本地文件这个方法经常返回 `null`

导致：
1. `NetworkAPI.patch()` 没有识别本地视频文件
2. `ImageFetch` 尝试将视频文件作为图像处理
3. 抛出 `NoImageException`

### 代码问题位置
```java
// 原始代码
String type = conn.getContentType();
if (type != null) {
    if (DataTool.startsWith(type, VID_MIMETYPES))
        throw new VideoTypeException();
    
    if (!type.startsWith("image"))
        throw new NoImageException();
} else {
    throw new NoImageException();  // ❌ 问题：没有检查文件扩展名
}
```

## ✅ 解决方案

### 修复策略
在两个地方添加文件扩展名检查：
1. **NetworkAPI.patch()** - 在返回结果前检查文件扩展名，设置正确的 `assumeVideo` 值
2. **ImageFetch.java** - 添加文件扩展名检查作为 MIME 类型检测的后备方案（双重保险）

### 修复后的代码

#### 1. NetworkAPI.patch() 修复
```java
public static AbstractPatch.Result patch(URI uri) {
    try {
        for (AbstractPatch fixer: FIXERS) {
            if (fixer.isValid(uri)) {
                AbstractPatch.Result r = CACHE.get(uri);
                if (r != null) return r;

                r = fixer.patch(uri, null);
                CACHE.put(uri, r);
                return r;
            }
        }
        
        // ✅ 新增：检查文件扩展名
        boolean assumeVideo = isVideoOrAudioFile(uri);
        return new AbstractPatch.Result(uri, assumeVideo, false);
    } catch (Exception e) {
        LOGGER.error(IT, "Exception occurred fixing URL", e);
        return null;
    }
}

private static boolean isVideoOrAudioFile(URI uri) {
    String path = uri.getPath();
    if (path == null) return false;
    
    String lowerPath = path.toLowerCase();
    
    // Check for video/audio extensions
    return lowerPath.endsWith(".mkv") || lowerPath.endsWith(".mp4") || 
           lowerPath.endsWith(".avi") || /* ... more extensions ... */;
}
```

#### 2. ImageFetch.java 修复（后备方案）
```java
String type = conn.getContentType();
if (type != null) {
    // 优先使用 MIME 类型
    if (DataTool.startsWith(type, VID_MIMETYPES))
        throw new VideoTypeException();
    
    if (!type.startsWith("image"))
        throw new NoImageException();
} else {
    // 后备方案：检查文件扩展名
    String path = patchUri.getPath();
    if (path != null) {
        String lowerPath = path.toLowerCase();
        
        // 检查视频文件扩展名
        if (lowerPath.endsWith(".mkv") || lowerPath.endsWith(".mp4") || 
            lowerPath.endsWith(".avi") || lowerPath.endsWith(".mov") || 
            lowerPath.endsWith(".webm") || lowerPath.endsWith(".flv") ||
            lowerPath.endsWith(".wmv") || lowerPath.endsWith(".m4v") ||
            lowerPath.endsWith(".mpg") || lowerPath.endsWith(".mpeg") ||
            lowerPath.endsWith(".m3u8") || lowerPath.endsWith(".m3u") ||
            lowerPath.endsWith(".ts") || lowerPath.endsWith(".m2ts")) {
            throw new VideoTypeException();
        }
        
        // 检查音频文件扩展名
        if (lowerPath.endsWith(".mp3") || lowerPath.endsWith(".wav") || 
            lowerPath.endsWith(".ogg") || lowerPath.endsWith(".flac") ||
            lowerPath.endsWith(".aac") || lowerPath.endsWith(".m4a") ||
            lowerPath.endsWith(".wma") || lowerPath.endsWith(".opus")) {
            throw new VideoTypeException();
        }
        
        // 检查是否为已知的图像扩展名
        if (!lowerPath.endsWith(".png") && !lowerPath.endsWith(".jpg") && 
            !lowerPath.endsWith(".jpeg") && !lowerPath.endsWith(".gif") && 
            !lowerPath.endsWith(".bmp") && !lowerPath.endsWith(".webp")) {
            throw new NoImageException();
        }
    } else {
        throw new NoImageException();
    }
}
```

## 🎯 支持的文件扩展名

### 视频格式
- `.mkv` - Matroska
- `.mp4` - MPEG-4
- `.avi` - AVI
- `.mov` - QuickTime
- `.webm` - WebM
- `.flv` - Flash Video
- `.wmv` - Windows Media Video
- `.m4v` - iTunes Video
- `.mpg`, `.mpeg` - MPEG
- `.m3u8`, `.m3u` - HLS Playlist
- `.ts`, `.m2ts` - MPEG Transport Stream

### 音频格式
- `.mp3` - MP3
- `.wav` - WAV
- `.ogg` - Ogg Vorbis
- `.flac` - FLAC
- `.aac` - AAC
- `.m4a` - MPEG-4 Audio
- `.wma` - Windows Media Audio
- `.opus` - Opus

### 图像格式
- `.png` - PNG
- `.jpg`, `.jpeg` - JPEG
- `.gif` - GIF
- `.bmp` - Bitmap
- `.webp` - WebP

## 📊 修复效果

### 修复前
```
❌ 本地 MKV 文件 → ImageFetch 尝试作为图像处理 → NoImageException
❌ 本地 MP4 文件 → ImageFetch 尝试作为图像处理 → NoImageException
❌ 任何本地视频文件都会失败
```

### 修复后
```
✅ 本地 MKV 文件 → 检测到视频扩展名 → VideoTypeException → 正确处理
✅ 本地 MP4 文件 → 检测到视频扩展名 → VideoTypeException → 正确处理
✅ 网络视频文件 → MIME 类型检测 → VideoTypeException → 正确处理
✅ 本地图像文件 → 检测到图像扩展名 → 正常加载
✅ 网络图像文件 → MIME 类型检测 → 正常加载
```

## 🧪 测试场景

### 场景 1: 本地 MKV 文件
```java
URI uri = new File("C:/Users/20901/Downloads/1.mkv").toURI();
// 结果: 正确识别为视频，抛出 VideoTypeException
```

### 场景 2: 网络 MKV 文件
```java
URI uri = new URI("https://example.com/video.mkv");
// 结果: 通过 MIME 类型或扩展名识别为视频
```

### 场景 3: 本地图像文件
```java
URI uri = new File("C:/Users/20901/Downloads/image.png").toURI();
// 结果: 正确识别为图像，正常加载
```

### 场景 4: 无扩展名的文件
```java
URI uri = new File("C:/Users/20901/Downloads/file").toURI();
// 结果: 抛出 NoImageException（预期行为）
```

## 🔍 技术细节

### 为什么本地文件的 MIME 类型为 null？
Java 的 `URLConnection.getContentType()` 对于 `file://` 协议的实现依赖于：
1. 文件扩展名映射
2. 系统的 MIME 类型数据库
3. 文件内容探测（可选）

在很多情况下，这些机制都不可靠，导致返回 `null`。

### 为什么使用扩展名检查？
- ✅ 简单可靠
- ✅ 性能好（不需要读取文件内容）
- ✅ 覆盖常见格式
- ✅ 与用户期望一致

### 为什么不使用文件内容检测？
- ❌ 性能开销大
- ❌ 需要读取文件头
- ❌ 对于大文件可能很慢
- ❌ 扩展名检查已经足够

## 📝 相关问题

### Q: 如果文件扩展名错误怎么办？
A: 这是用户的责任。如果文件扩展名与实际内容不匹配，可能会导致播放失败，但这是预期行为。

### Q: 为什么不支持所有视频格式？
A: 列表包含了最常见的格式。如果需要支持更多格式，可以轻松添加到检查列表中。

### Q: 这会影响性能吗？
A: 不会。字符串比较的性能开销可以忽略不计，而且只在 MIME 类型为 null 时才执行。

## 🎉 总结

这个修复解决了本地视频文件被错误识别为图像的问题，通过添加文件扩展名检查作为 MIME 类型检测的后备方案。现在 WaterMedia 可以正确处理：

- ✅ 本地视频文件（通过扩展名）
- ✅ 网络视频文件（通过 MIME 类型）
- ✅ 本地图像文件（通过扩展名）
- ✅ 网络图像文件（通过 MIME 类型）

---

**修复版本**: v2.1.37
**修复日期**: 2024
**影响文件**: `src/main/java/org/watermedia/api/image/ImageFetch.java`
