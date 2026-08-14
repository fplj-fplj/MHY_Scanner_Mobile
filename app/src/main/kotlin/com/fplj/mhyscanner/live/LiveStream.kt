package com.fplj.mhyscanner.live

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class LiveStreamInfo(
    val ok: Boolean,
    val url: String = "",
    val statusText: String = "",
    val headers: Map<String, String> = emptyMap()
)

object LiveStream {

    enum class Platform { BILIBILI, DOUYIN }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getStreamInfo(platform: Platform, roomId: String): LiveStreamInfo =
        withContext(Dispatchers.IO) {
            when (platform) {
                Platform.BILIBILI -> resolveBilibili(roomId)
                Platform.DOUYIN -> resolveDouyin(roomId)
            }
        }

    // ---------- B 站 ----------

    private fun resolveBilibili(roomId: String): LiveStreamInfo {
        val initUrl = "${ApiLive.BILI_ROOM_INIT}?id=$roomId"
        val roomInfo = getJson(initUrl) ?: return err("网络请求失败")
        val code = roomInfo.int("code")
        if (code == 60004) return err("直播间不存在")
        if (code != 0) return err("直播间信息错误(code=$code)")
        val data = roomInfo.obj("data") ?: return err("数据解析失败")
        val liveStatus = data.int("live_status")
        if (liveStatus != 1) return err("未开播")
        val realRoomId = data.int("room_id").toString()

        val playUrl = getJson(
            "${ApiLive.BILI_PLAY_INFO}?codec=0&format=0,2&only_audio=0&only_video=0&protocol=0,1&qn=400&room_id=$realRoomId"
        ) ?: return err("获取流地址失败")
        val streamList = playUrl.obj("data")?.obj("playurl_info")?.obj("playurl")?.get("stream") as? JsonArray
            ?: return err("流数据解析失败")

        var link = findStreamLink(streamList, preferHls = true)
        if (link == null) link = findStreamLink(streamList, preferHls = false)
        if (link.isNullOrEmpty()) return err("未找到可用的直播流")
        return LiveStreamInfo(ok = true, url = link)
    }

    private fun findStreamLink(streams: JsonArray, preferHls: Boolean): String? {
        for (stream in streams) {
            val formats = stream.jsonObject.get("format") as? JsonArray ?: continue
            val format = if (preferHls) {
                formats.firstOrNull { it.jsonObject.str("format_name") == "hls" }
                    ?: formats.firstOrNull { it.jsonObject.str("format_name") == "flv" }
            } else {
                formats.firstOrNull { it.jsonObject.str("format_name") == "flv" }
                    ?: formats.firstOrNull { it.jsonObject.str("format_name") == "hls" }
            } ?: continue
            val codecs = format.jsonObject.get("codec") as? JsonArray ?: continue
            for (codec in codecs) {
                val co = codec.jsonObject
                val info = co.get("url_info") as? JsonArray ?: continue
                val first = info.firstOrNull()?.jsonObject ?: continue
                val host = first.str("host")
                val baseUrl = co.str("base_url")
                val extra = first.str("extra")
                if (baseUrl.isNotEmpty()) return host + baseUrl + extra
            }
        }
        return null
    }

    // ---------- 抖音 ----------

    private const val DOUYIN_PARAMS = "aid=6383&app_name=douyin_web&live_id=1&device_platform=web&" +
        "browser_language=zh-CN&browser_platform=Win32&browser_name=Edge&browser_version=139.0.0.0&" +
        "is_need_double_stream=false&web_rid="

    private const val DOUYIN_COOKIE = "enter_pc_once=1; UIFID_TEMP=29a1f63ec682dc0a0df227dd163e2b46e3a6390e403335fa4c2c6d1dc0ec5ffa7a288170e8828ecb8b2f0f16b3219daa18ad5d7faf7fb5fbb64df454c3b471cc1db9c0b5eb2cbc8e0cb1e690f5c1fbd6"

    private fun resolveDouyin(roomId: String): LiveStreamInfo {
        val url = ApiLive.DOUYIN_ENTER + DOUYIN_PARAMS + roomId
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36",
            "referer" to "https://live.douyin.com/",
            "cookie" to DOUYIN_COOKIE
        )
        val resp = getJson(url, headers) ?: return err("网络请求失败")
        val statusCode = resp.int("status_code")
        if (statusCode != 0) return err("直播间不存在(status_code=$statusCode)")
        val room = resp.obj("data")?.get("data") as? JsonArray ?: return err("数据解析失败")
        val first = room.firstOrNull()?.jsonObject ?: return err("数据解析失败")
        val status = first.int("status")
        if (status != 2) return err("未开播")
        val link = douyinStreamLink(first.obj("stream_url")) ?: return err("未找到可用的直播流")
        return LiveStreamInfo(ok = true, url = link)
    }

    private fun douyinStreamLink(streamUrl: JsonObject?): String? {
        if (streamUrl == null) return null
        try {
            val pullDatas = streamUrl.get("pull_datas") as? JsonObject
            if (pullDatas != null && pullDatas.isNotEmpty()) {
                val streamDataStr = pullDatas.values.firstOrNull()?.jsonObject?.str("stream_data")
                parseDouyinMain(streamDataStr)?.let { return it }
            }
            val sdkData = streamUrl.obj("live_core_sdk_data")?.obj("pull_data")?.str("stream_data")
            parseDouyinMain(sdkData)?.let { return it }
        } catch (_: Exception) {
        }
        return null
    }

    private fun parseDouyinMain(streamDataStr: String?): String? {
        if (streamDataStr.isNullOrEmpty()) return null
        return runCatching {
            val data = json.parseToJsonElement(streamDataStr).jsonObject
            val main = data.obj("data")?.obj("origin")?.obj("main") ?: return null
            val hls = main.str("hls")
            if (hls.isNotEmpty()) hls else main.str("flv")
        }.getOrNull()
    }

    // ---------- 工具 ----------

    private fun getJson(url: String, headers: Map<String, String> = emptyMap()): JsonObject? {
        return try {
            val builder = Request.Builder().url(url).get()
            headers.forEach { (k, v) -> builder.addHeader(k, v) }
            val text = client.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return null
                resp.body?.string().orEmpty()
            }
            json.parseToJsonElement(text).jsonObject
        } catch (e: IOException) {
            null
        }
    }

    private fun err(text: String) = LiveStreamInfo(ok = false, statusText = text)

    private fun JsonObject.str(key: String): String =
        this[key]?.let { if (it is JsonPrimitive) it.content else it.toString() } ?: ""

    private fun JsonObject.int(key: String): Int =
        this[key]?.let { (it as? JsonPrimitive)?.contentOrNull?.toIntOrNull() } ?: -1

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
}

private object ApiLive {
    const val BILI_ROOM_INIT = "https://api.live.bilibili.com/room/v1/Room/room_init"
    const val BILI_PLAY_INFO = "https://api.live.bilibili.com/xlive/web-room/v2/index/getRoomPlayInfo"
    const val DOUYIN_ENTER = "https://live.douyin.com/webcast/room/web/enter/?"
}