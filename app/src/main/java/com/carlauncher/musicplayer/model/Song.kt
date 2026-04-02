package com.carlauncher.musicplayer.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,        // milliseconds
    val uri: Uri,
    val path: String,
    val genre: String = "",
    val year: Int = 0,
    val dateAdded: Long = 0,   // timestamp
    val size: Long = 0
) {
    val durationText: String
        get() {
            val minutes = duration / 1000 / 60
            val seconds = duration / 1000 % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val category: MusicCategory
        get() = MusicCategory.categorize(artist, title, path)
}

data class Artist(
    val name: String,
    val songCount: Int,
    val songs: List<Song> = emptyList()
)

data class Album(
    val name: String,
    val artist: String,
    val songs: List<Song> = emptyList()
)

/**
 * 音乐分类 - 基于歌手名称智能分类
 * 根据U盘实际歌曲内容设计分类
 */
enum class MusicCategory(val displayName: String, val artists: Set<String>) {
    JAY_CHOU("周杰伦", setOf(
        "周杰伦", "周jie伦", "jay chou", "十二新作", "范特西", "魔杰座"
    )),
    CANTONESE("粤语金曲", setOf(
        "张学友", "刘德华", "谭咏麟", "beyond", "张国荣", "梅艳芳",
        "陈慧娴", "陈奕迅", "李克勤", "罗文", "叶丽仪", "叶倩文",
        "叶蒨文", "关淑怡", "关正杰", "许冠杰", "林子祥", "徐小凤",
        "黄凯芹", "容祖儿", "古巨基", "郑少秋", "汪明荃", "黎明",
        "郭富城", "杨千嬅", "甄妮", "草蜢", "钟镇涛", "叶振棠",
        "吕方", "温兆伦", "吕珊", "黎瑞恩", "邝美云", "彭羚",
        "梁朝伟", "郑秀文", "王馨平", "陈浩民", "陈慧琳", "蔡国权",
        "周慧敏", "林忆莲", "周启生", "谢霆锋", "侧田", "薛凯琪",
        "卫兰", "连诗雅", "林奕匡", "梁咏琪", "李幸倪", "达明一派",
        "黄耀明", "刘小慧", "郑中基", "卢冠廷", "莫文蔚", "王菲",
        "aga", "mc 张天赋", "阿梨粤", "陈小春", "twins"
    )),
    NOSTALGIC("怀旧金曲", setOf(
        "邓丽君", "蔡琴", "韩宝仪", "费玉清", "高胜美", "龙飘飘",
        "蔡幸娟", "凤飞飞", "杨钰莹", "毛宁", "毛阿敏", "韦唯",
        "李丽芬", "蒋大为", "李谷一", "宋祖英", "欧阳菲菲", "李翊君",
        "千百惠", "陈红", "蔡秋凤", "小虎队", "童安格", "姜育恒",
        "齐秦", "齐豫", "苏芮", "陈淑桦", "辛晓琪", "赵传",
        "潘越云", "潘美辰", "裘海正", "张蔷", "罗大佑", "屠洪刚",
        "崔健", "张雨生", "王杰", "卓依婷", "郑智化", "张镐哲",
        "孟庭苇", "张洪量", "老狼", "张明敏", "郑绪岚", "唐朝乐队",
        "黑豹乐队", "左宏元", "张慧清", "李春波", "高林生", "高枫",
        "江涛", "江珊", "满文军", "孙浩", "戴军", "林依轮", "张真",
        "光头李进", "火风", "陈琳", "文章", "谢东", "李琛", "杨林"
    )),
    CHINESE_POP("华语流行", setOf(
        "周华健", "张信哲", "庾澄庆", "伍佰", "任贤齐", "李宗盛",
        "吴奇隆", "刘欢", "王力宏", "林俊杰", "张宇", "孙燕姿",
        "蔡依林", "张韶涵", "梁静茹", "王心凌", "萧亚轩", "张惠妹",
        "那英", "刘若英", "徐怀钰", "温岚", "林志炫", "林志颖",
        "凤凰传奇", "刀郎", "游鸿明", "邰正宵", "杜德伟", "伍思凯",
        "水木年华", "朴树", "汪峰", "许巍", "信乐团", "李圣杰",
        "周传雄", "张栋梁", "唐磊", "许茹芸", "赵咏华", "彭佳慧",
        "田震", "孙露", "祁隆", "程响", "萧敬腾", "薛之谦",
        "李荣浩", "周深", "陈粒", "赵雷", "陈楚生", "胡彦斌",
        "张震岳", "林宥嘉", "卢广仲", "潘玮柏", "费翔", "黄品源",
        "沙宝亮", "黄征", "腾格尔", "韩红", "韩磊", "韩雪",
        "庞龙", "郑源", "郑钧", "雷佳", "苏运莹", "邓紫棋",
        "胡杨林", "马郁", "张妙格", "单依纯", "张碧晨", "许慧欣",
        "许美静", "许佳慧", "王蓉", "阿桑", "海来阿木", "阿冗", "阿木",
        "五月天", "八三夭乐团", "筷子兄弟", "逃跑计划", "迪克牛仔",
        "飞儿乐团", "s.h.e", "f4", "tfboys", "陈冠蒲", "张敬轩",
        "弦子", "田馥甄", "黄霄雲", "刘宇宁", "孙悦", "陈星",
        "刘雨昕", "云朵", "广东雨神", "付豪", "半吨兄弟", "马健涛",
        "王琪", "杨烁", "狼戈", "柏松", "校长", "苏谭谭", "苏晗",
        "黑龙", "誓言", "汤潮", "易欣", "魏新雨", "魏晓雪",
        "尚亿哥", "兰雨", "浩瀚", "莫叫姐姐", "弹棉花的小花",
        "橘络", "耳朵便利店", "司南", "大籽", "大张伟", "曾浠妍",
        "倪尔萍", "崔子格", "崔栩维", "柯受良", "至上励合", "交通国",
        "小阿枫", "格格", "公主病", "二小姐", "晴天姐妹", "亚东",
        "央金兰泽", "布仁巴雅尔", "降央卓玛", "科尔沁夫", "姚璎格",
        "任夏", "梅小琴", "汪东城", "范逸臣", "王贰浪",
        "朱铭捷", "金润吉", "徐良", "徐薇", "徐誉滕", "李天华",
        "李梦瑶", "李玉刚", "李玲玉", "王宇宙", "周柯宇", "王强",
        "赵洋", "郭欢", "郭美美", "彭家丽", "成龙", "js", "t.r.y.",
        "try", "双笙", "陈元汐", "品冠", "孙楠", "许佳慧"
    )),
    WESTERN("欧美金曲", setOf(
        "taylor swift", "the beatles", "michael jackson", "adele",
        "celine dion", "whitney houston", "backstreet boys", "westlife",
        "m2m", "avril lavigne", "queen", "nirvana", "richard marx",
        "bertie higgins", "sarah brightman", "carpenters", "boney m",
        "groove coverage", "emilia", "rick astley", "ricky martin",
        "george benson", "danny mc carthy", "em beihold",
        "richard clayderman", "lionel richie", "usher", "blue",
        "vangelis", "beyoncé", "helene", "ailee", "wonder girls"
    )),
    OTHER("其他", setOf());

    companion object {
        fun categorize(artist: String, title: String, path: String): MusicCategory {
            val searchText = "${artist} ${title}".lowercase()

            // 按优先级匹配歌手名（周杰伦优先）
            for (cat in entries) {
                if (cat == OTHER) continue
                if (cat.artists.any { searchText.contains(it.lowercase()) }) {
                    return cat
                }
            }

            // 路径兜底：周杰伦文件夹里的未知歌曲
            if (path.contains("周杰伦")) return JAY_CHOU

            return OTHER
        }
    }
}

enum class SortOrder(val displayName: String) {
    TITLE_ASC("歌曲名 A-Z"),
    TITLE_DESC("歌曲名 Z-A"),
    ARTIST_ASC("歌手 A-Z"),
    ARTIST_DESC("歌手 Z-A"),
    DATE_NEWEST("最新添加"),
    DATE_OLDEST("最早添加"),
    DURATION_SHORT("时长 短→长"),
    DURATION_LONG("时长 长→短")
}

enum class PlayMode(val displayName: String) {
    SEQUENTIAL("顺序播放"),
    LOOP_ALL("列表循环"),
    LOOP_SINGLE("单曲循环"),
    SHUFFLE("随机播放"),
    SMART_RECOMMEND("智能推荐")
}
