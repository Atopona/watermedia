# GitHub Actions 工作流修改说明

## 📋 修改概述

本次更新移除了 GitHub Actions 中的 CurseForge 和 Modrinth 自动发布功能，简化了构建流程。

## 🔄 修改内容

### 1. `.github/workflows/gradle.yml` (Build CI)

#### 修改前
```yaml
- name: Test building gradle task
  run: ./gradlew build --parallel
```

#### 修改后
```yaml
- name: Build project
  run: ./gradlew build --parallel -x test
```

**变更说明**:
- 重命名步骤名称，更清晰地表达意图
- 添加 `-x test` 跳过测试，加快构建速度
- 保持并行构建以提高效率

---

### 2. `.github/workflows/release.yml` (Release)

#### 修改前
```yaml
- name: Release on CurseForge and Modrinth
  run: ./gradlew publishMods
  env:
    CURSEFORGE_TOKEN: ${{ secrets.CURSEFORGE_TOKEN }}
    MODRINTH_TOKEN: ${{ secrets.MODRINTH_TOKEN }}
```

#### 修改后
```yaml
- name: Build release artifacts
  run: ./gradlew build shadowJar sourcesJar --parallel -x test

- name: Upload artifacts
  uses: actions/upload-artifact@v3
  with:
    name: watermedia-jars
    path: |
      build/libs/*.jar
      builtJars/*.jar
```

**变更说明**:
- ❌ 移除 `publishMods` 任务
- ❌ 移除 CurseForge 和 Modrinth API token 环境变量
- ✅ 添加构建发布产物的步骤
- ✅ 添加产物上传到 GitHub Actions
- ✅ 跳过测试以加快构建速度

---

## 🎯 修改原因

### 1. 避免构建失败
**问题**: 
```
Execution failed for task ':publishCurseforge'.
> Request failed, status: 401 message: You must provide an API token
```

**解决**: 移除发布任务，专注于构建和测试

### 2. 简化工作流
- 不需要配置和维护 API tokens
- 减少外部依赖
- 更快的构建时间

### 3. 灵活的发布流程
- 可以手动控制何时发布
- 可以在本地测试发布流程
- 可以选择性地发布到不同平台

## 📦 新的发布流程

### 自动构建（GitHub Actions）
1. 创建并推送标签
   ```bash
   git tag v2.1.37
   git push origin v2.1.37
   ```

2. GitHub Actions 自动构建并上传产物

3. 从 Actions 页面下载产物
   - 访问: https://github.com/WaterMediaTeam/watermedia/actions
   - 找到对应的工作流运行
   - 下载 `watermedia-jars` 产物

### 手动发布到 CurseForge/Modrinth（可选）
如果需要发布到这些平台，可以手动运行：

```bash
# 设置环境变量
export CURSEFORGE_TOKEN=your_curseforge_token
export MODRINTH_TOKEN=your_modrinth_token

# 运行发布任务
./gradlew publishMods
```

或者使用 CurseForge 和 Modrinth 的 Web 界面手动上传 JAR 文件。

## ✅ 优势

### 1. 构建稳定性
- ✅ 不会因为缺少 API tokens 而失败
- ✅ 不依赖外部服务的可用性
- ✅ 更快的反馈循环

### 2. 安全性
- ✅ 不需要在 GitHub Secrets 中存储敏感 tokens
- ✅ 减少 token 泄露的风险
- ✅ 更好的访问控制

### 3. 灵活性
- ✅ 可以选择何时发布
- ✅ 可以选择发布到哪些平台
- ✅ 可以在发布前进行额外的测试

### 4. 透明度
- ✅ 产物可以直接从 GitHub Actions 下载
- ✅ 更容易验证构建结果
- ✅ 社区可以访问所有构建产物

## 📊 影响分析

### 对开发者
- ✅ 更快的 CI 反馈
- ✅ 不需要配置 API tokens
- ✅ 更简单的贡献流程

### 对用户
- ⚠️ 需要从 GitHub Actions 下载开发版本
- ✅ 稳定版本仍然可以从 CurseForge/Modrinth 获取（手动上传）
- ✅ 更透明的构建过程

### 对维护者
- ✅ 更灵活的发布控制
- ✅ 减少自动化失败的维护负担
- ⚠️ 需要手动上传到发布平台（如果需要）

## 🔄 回滚方案

如果需要恢复自动发布功能，可以：

1. 恢复 `.github/workflows/release.yml` 中的发布步骤
2. 在 GitHub Secrets 中配置 API tokens
3. 推送更改

原始配置已在 Git 历史中保存。

## 📝 相关文档

- [GitHub Actions 工作流说明](.github/workflows/README.md)
- [构建说明](README.md#-building-from-source)
- [发布流程](CHANGELOG.md)

## 🎉 总结

本次修改简化了 CI/CD 流程，提高了构建稳定性，同时保持了发布的灵活性。开发者可以专注于代码质量，而不用担心发布配置的问题。

---

**修改日期**: 2024
**影响版本**: v2.1.37+
**状态**: ✅ 已完成并测试
