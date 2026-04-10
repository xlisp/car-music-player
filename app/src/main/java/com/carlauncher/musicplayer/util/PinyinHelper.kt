package com.carlauncher.musicplayer.util

import android.content.Context
import android.icu.text.Transliterator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object PinyinHelper {

    private val transliterator by lazy {
        Transliterator.getInstance("Han-Latin; Latin-ASCII; Any-Lower")
    }

    private val pinyinCache = HashMap<String, String>()
    private val initialsCache = HashMap<String, String>()
    private val gson = Gson()

    /**
     * 从磁盘加载拼音缓存
     */
    fun loadCacheFromDisk(context: Context) {
        try {
            val pinyinFile = File(context.filesDir, "pinyin_cache.json")
            val initialsFile = File(context.filesDir, "pinyin_initials_cache.json")
            if (pinyinFile.exists()) {
                val type = object : TypeToken<HashMap<String, String>>() {}.type
                val loaded: HashMap<String, String>? = gson.fromJson(pinyinFile.readText(), type)
                if (loaded != null) pinyinCache.putAll(loaded)
            }
            if (initialsFile.exists()) {
                val type = object : TypeToken<HashMap<String, String>>() {}.type
                val loaded: HashMap<String, String>? = gson.fromJson(initialsFile.readText(), type)
                if (loaded != null) initialsCache.putAll(loaded)
            }
        } catch (_: Exception) {}
    }

    /**
     * 将拼音缓存保存到磁盘
     */
    fun saveCacheToDisk(context: Context) {
        try {
            File(context.filesDir, "pinyin_cache.json").writeText(gson.toJson(pinyinCache))
            File(context.filesDir, "pinyin_initials_cache.json").writeText(gson.toJson(initialsCache))
        } catch (_: Exception) {}
    }

    /**
     * 预热拼音缓存：为所有文本预计算拼音
     */
    fun preWarm(texts: List<String>) {
        for (text in texts) {
            getPinyin(text)
            getPinyinInitials(text)
        }
    }

    /**
     * 获取中文文本的完整拼音（无空格）
     * 例如："周杰伦" -> "zhoujielun"
     */
    fun getPinyin(text: String): String {
        return pinyinCache.getOrPut(text) {
            try {
                transliterator.transliterate(text)
                    .replace(Regex("[^a-zA-Z0-9]"), "")
                    .lowercase()
            } catch (e: Exception) {
                text.lowercase()
            }
        }
    }

    /**
     * 获取中文文本的拼音首字母
     * 例如："周杰伦" -> "zjl"，"罗大佑" -> "ldy"
     */
    fun getPinyinInitials(text: String): String {
        return initialsCache.getOrPut(text) {
            try {
                val sb = StringBuilder()
                for (c in text) {
                    if (c.code in 0x4E00..0x9FFF) {
                        val pinyin = transliterator.transliterate(c.toString()).trim()
                        if (pinyin.isNotEmpty() && pinyin[0].isLetter()) {
                            sb.append(pinyin[0].lowercaseChar())
                        }
                    } else if (c.isLetterOrDigit()) {
                        sb.append(c.lowercaseChar())
                    }
                }
                sb.toString()
            } catch (e: Exception) {
                ""
            }
        }
    }

    /**
     * 检查查询是否可能是拼音（仅含小写字母）
     */
    private fun isPinyinQuery(query: String): Boolean {
        return query.all { it in 'a'..'z' }
    }

    /**
     * 检查拼音查询是否匹配文本（支持首字母和全拼）
     */
    fun matchesPinyin(text: String, query: String): Boolean {
        if (!isPinyinQuery(query)) return false

        // 首字母匹配：zjl -> 周杰伦
        val initials = getPinyinInitials(text)
        if (initials.contains(query)) return true

        // 全拼匹配：luo -> 罗大佑，zhou -> 周杰伦
        val fullPinyin = getPinyin(text)
        if (fullPinyin.contains(query)) return true

        return false
    }
}
