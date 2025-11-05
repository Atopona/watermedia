# GitHub Actions 工作流说明

## 📋 工作流概述

本项目包含两个 GitHub Actions 工作流：

### 1. Build CI (`gradle.yml`)
**触发条件**: 
- Push 到 `main` 或 `2.0.x` 分支
- Pull Request 到 `main` 或 `2.0.x` 分支

**功能**:
- 检出代码
- 设置 JDK 21 (Microsoft 发行版)
- 构建项目（跳过测试以加快速度）

**命令**: `./gradlew build --parallel -x test`

### 2. Release Jar (`release.yml`)
**触发条件**: 
- Push 带有任何标签的提交

**功能**:
- 检出标签代码
- 设置 JDK 21 (Microsoft 发行版)
- 构建发布版本的 JAR 文件
- 上传构建产物到 GitHub Actions

**命令**: `./gradlew build shadowJar sourcesJar --parallel -x test`

**产物**:
- 主 JAR 文件
- Shadow JAR（包含所有依赖）
- 源码 JAR

## 🔄 工作流变更历史

### 2024 - 移除 CurseForge 和 Modrinth 发布
**原因**: 
- 简化构建流程
- 避免因缺少 API tokens 导致的构建失败
- 专注于构建和测试

**变更内容**:
- ✅ 移除 `publishMods` 任务
- ✅ 移除 `CURSEFORGE_TOKEN` 和 `MODRINTH_TOKEN` 环境变量
- ✅ 添加 GitHub Actions 产物上传
- ✅ 跳过测试以加快构建速度

**如果需要发布到 CurseForge/Modrinth**:
可以手动运行以下命令（需要配置 API tokens）:
```bash
export CURSEFORGE_TOKEN=your_token
export MODRINTH_TOKEN=your_token
./gradlew publishMods
```

## 📦 构建产物

### CI 构建 (gradle.yml)
- 验证代码可以成功编译
- 不生成可下载的产物

### 发布构建 (release.yml)
产物位置: GitHub Actions 的 Artifacts 部分

包含文件:
- `watermedia-{version}.jar` - 主 JAR
- `watermedia-{version}-sources.jar` - 源码 JAR
- Shadow JAR（在 `builtJars/` 目录）

## 🚀 如何创建发布

1. **更新版本号**
   ```properties
   # gradle.properties
   version=2.1.37
   ```

2. **提交更改**
   ```bash
   git add .
   git commit -m "Release v2.1.37"
   ```

3. **创建标签**
   ```bash
   git tag v2.1.37
   git push origin v2.1.37
   ```

4. **下载产物**
   - 访问 GitHub Actions 页面
   - 找到对应的工作流运行
   - 下载 `watermedia-jars` 产物

## 🔧 本地构建

### 完整构建
```bash
./gradlew build
```

### 仅构建 JAR（跳过测试）
```bash
./gradlew build -x test
```

### 构建 Shadow JAR
```bash
./gradlew shadowJar
```

### 构建所有产物
```bash
./gradlew build shadowJar sourcesJar
```

## 📝 注意事项

- 所有工作流都使用 JDK 21 进行构建
- 项目目标是 Java 8 兼容性
- 测试在 CI 中被跳过以加快构建速度
- 发布产物会自动上传到 GitHub Actions

## 🐛 故障排除

### 构建失败
1. 检查 Java 版本是否正确
2. 确保 gradlew 有执行权限
3. 查看构建日志获取详细错误

### 产物未生成
1. 确认工作流已成功完成
2. 检查 Actions 页面的 Artifacts 部分
3. 验证构建命令是否正确执行

---

**最后更新**: 2024
**维护者**: WaterMedia Team
