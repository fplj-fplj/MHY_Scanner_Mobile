package com.fplj.mhyscanner.core

/** 解析 Cookie 字符串为键值 Map,与原项目 CookieParser 一致 */
class CookieParser(cookie: String) {

    private val map = HashMap<String, String>()

    init {
        var pos = 0
        while (pos < cookie.length) {
            val equalPos = cookie.indexOf('=', pos)
            if (equalPos == -1) break
            var endPos = cookie.indexOf(';', pos)
            if (endPos == -1) endPos = cookie.length
            val key = cookie.substring(pos, equalPos).trim()
            val value = cookie.substring(equalPos + 1, endPos).trim()
            if (key.isNotEmpty()) map[key] = value
            pos = endPos + 1
        }
    }

    operator fun get(key: String): String? = map[key]

    fun keys(): Set<String> = map.keys
}
