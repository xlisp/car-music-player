# 车载音乐播放器 (Car Music Player)

Android 车机音乐播放客户端，支持读取本地 U 盘音乐进行播放，具备搜索、分类、智能推荐等功能。

## 功能特性

### 基础功能
- **U盘音乐读取** — 自动扫描 USB 外部存储设备中的音乐文件（mp3/flac/wav/aac/ogg/wma/m4a/ape）
- **音乐播放** — 前台服务播放，支持通知栏控制，MediaSession 集成
- **搜索** — 实时搜索歌曲名、歌手名、专辑名
- **USB 热插拔** — 自动检测 USB 设备插入/拔出并刷新歌曲列表

### 高级功能

#### 1. 歌手全部播放
点击歌手标签页中的任意歌手，进入该歌手的歌曲详情页，支持一键"全部播放"。

#### 2. 分类播放
自动将歌曲按流派和年代归类为 15 种音乐类型：

| 分类 | 匹配规则 |
|------|----------|
| 华语流行 | mandopop, c-pop, 流行, 国语 |
| 粤语流行 | cantopop, 粤语 |
| 摇滚 | rock, punk, metal, alternative |
| 抒情 | ballad, 情歌 |
| 电子 | electronic, edm, dance, techno |
| R&B/Soul | r&b, soul, funk |
| 说唱/嘻哈 | hip-hop, rap, 嘻哈 |
| 爵士 | jazz, blues |
| 古典 | classical, symphony |
| 民谣 | folk, acoustic, 民歌 |
| 日韩 | j-pop, k-pop, anime |
| 欧美流行 | pop, western |
| 00后经典 | 2000-2010 年发行 |
| 90年代 | 1990-1999 年发行 |
| 其他 | 未匹配的歌曲 |

#### 3. 智能推荐播放
切换到"智能推荐"播放模式后，播完当前歌曲会基于以下维度自动推荐下一首：

- **同歌手** (+30分)
- **同分类/流派** (+25分)
- **年代相近** (5年内+20, 10年内+10)
- **同专辑** (+15分)
- **用户偏好歌手** (基于播放历史, 最高+10分)
- **时长相近** (+5分)
- **歌手关联** (featuring 合作关系, +8分)

从评分 Top10 中按权重概率抽取，兼顾相关性与多样性。

#### 4. 排序功能
支持 8 种排序方式：
- 歌曲名 A-Z / Z-A
- 歌手 A-Z / Z-A
- 最新添加 / 最早添加
- 时长 短→长 / 长→短

### 播放模式
顺序播放 → 列表循环 → 单曲循环 → 随机播放 → 智能推荐，点击切换。

## 项目结构

```
com.carlauncher.musicplayer/
├── model/Song.kt                  # 数据模型
├── repository/MusicRepository.kt  # USB/外部存储音乐扫描
├── service/
│   ├── MusicPlayerService.kt      # 前台播放服务
│   └── UsbReceiver.kt             # USB 热插拔监听
├── recommendation/
│   └── RecommendationEngine.kt    # 智能推荐引擎
├── viewmodel/MusicViewModel.kt    # 搜索/排序/筛选逻辑
├── adapter/
│   ├── SongAdapter.kt             # 歌曲列表
│   ├── ArtistAdapter.kt           # 歌手列表
│   └── CategoryAdapter.kt         # 分类网格
├── ui/
│   ├── SongListFragment.kt        # 主列表 (歌曲/歌手/分类标签页)
│   └── ArtistSongsFragment.kt     # 歌手/分类歌曲详情
└── MainActivity.kt                # 主界面
```

## UI 设计

- **横屏布局** — 左侧播放器面板 + 右侧内容区，适配车机屏幕
- **深色主题** — 暗色配色方案，减少夜间驾驶眩光
- **大触摸目标** — 按钮最小 56dp，播放按钮 80dp，便于驾驶中操作
- **跑马灯歌名** — 长歌名自动滚动显示

## 技术栈

- Kotlin
- Android SDK 34 (minSdk 26)
- MediaPlayer + MediaSession
- MVVM 架构 (ViewModel + LiveData)
- ViewBinding
- RecyclerView + ListAdapter + DiffUtil

## 构建与运行

1. 使用 Android Studio 打开项目
2. 等待 Gradle 同步完成
3. 连接车机设备或模拟器
4. 点击 Run 编译安装

## 权限说明

| 权限 | 用途 |
|------|------|
| `READ_EXTERNAL_STORAGE` / `READ_MEDIA_AUDIO` | 读取 USB/存储中的音乐文件 |
| `FOREGROUND_SERVICE` | 后台播放音乐 |
| `POST_NOTIFICATIONS` | 显示播放通知栏控制 |
