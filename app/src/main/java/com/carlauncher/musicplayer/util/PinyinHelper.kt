package com.carlauncher.musicplayer.util

import android.icu.text.Transliterator

object PinyinHelper {

    private val transliterator by lazy {
        Transliterator.getInstance("Han-Latin; Latin-ASCII; Any-Lower")
    }

    private val pinyinCache = HashMap<String, String>()
    private val initialsCache = HashMap<String, String>()

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
