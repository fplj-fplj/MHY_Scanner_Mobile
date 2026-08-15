package com.fplj.mhyscanner.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/** 复刻原项目 src/Core/MhyApi.hpp 的全部米哈游接口(纯阻塞,调用方自行切换 IO 线程) */
object MhyApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /** 每个进程生成一次的随机设备 id */
    val deviceId: String = UUID.randomUUID().toString()

    /** 随机小写字母+数字字符串,用于扫码登录设备 id */
    private val qrLoginDeviceId: String = randomLowerAndNumberString(53)

    // ---------- HTTP 辅助 ----------

    private fun buildHeaders(headers: Map<String, String>): Headers {
        val builder = Headers.Builder()
        headers.forEach { (k, v) -> if (v.isNotEmpty()) builder.add(k, v) }
        return builder.build()
    }

    private fun postJson(url: String, body: String, headers: Map<String, String>): JsonElement {
        val request = Request.Builder()
            .url(url)
            .headers(buildHeaders(headers))
            .post(body.toRequestBody(JSON))
            .build()
        return execute(request)
    }

    private fun getJson(url: String, headers: Map<String, String>): JsonElement {
        val request = Request.Builder()
            .url(url)
            .headers(buildHeaders(headers))
            .get()
            .build()
        return execute(request)
    }

    private fun execute(request: Request): JsonElement {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            val text = response.body?.string().orEmpty()
            return json.parseToJsonElement(text)
        }
    }

    /** 需要读取响应头(如 X-Rpc-Aigis)时使用 */
    private fun executeWithHeaders(request: Request): Pair<JsonElement, Headers> {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            val text = response.body?.string().orEmpty()
            return json.parseToJsonElement(text) to response.headers
        }
    }

    private fun JsonObject.str(key: String): String =
        this[key]?.let { if (it is JsonPrimitive) it.content else it.toString() } ?: ""

    private fun JsonObject.int(key: String): Int =
        this[key]?.let { (it as? JsonPrimitive)?.contentOrNull?.toIntOrNull() } ?: -1

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    // ---------- 请求头 ----------

    private val baseHeaders: Map<String, String> = mapOf(
        "Content-Type" to "application/json",
        "User-Agent" to "Mozilla/5.0 (Linux; Android 13) miHoYoBBS/${ApiDefs.MIHOYOBBS_VERSION}",
        "Accept" to "application/json",
        "x-rpc-aigis" to "",
        "x-rpc-app_id" to "bll8iq97cem8",
        "x-rpc-app_version" to ApiDefs.MIHOYOBBS_VERSION,
        "x-rpc-client_type" to "2",
        "x-rpc-device_id" to deviceId,
        "x-rpc-device_name" to "",
        "x-rpc-game_biz" to "bbs_cn",
        "x-rpc-sdk_version" to "2.16.0"
    )

    private fun scanHeaders(): Map<String, String> {
        val h = baseHeaders.toMutableMap()
        h["Content-Type"] = "application/json"
        h["x-rpc-app_id"] = "bll8iq97cem8"
        h["x-rpc-game_biz"] = "bbs_cn"
        return h
    }

    private fun randomLowerAndNumberString(length: Int): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length).map { chars.random() }.joinToString("")
    }

    private val JSON = "application/json; charset=utf-8".toMediaType()

    // ---------- 米游社扫码登录(添加账号,由手机米游社APP扫,HoyoPlay/passport 启动器方案) ----------

    private fun passportQrHeaders(): Map<String, String> = mapOf(
        "Content-Type" to "application/json",
        "User-Agent" to "HYPContainer/1.1.4.133",
        "Accept" to "application/json",
        "x-rpc-app_id" to "ddxf5dufpuyo",
        "x-rpc-client_type" to "3",
        "x-rpc-device_id" to qrLoginDeviceId
    )

    /** 创建米游社登录二维码,返回 (url, ticket) */
    fun createPassportQRLogin(): Pair<String, String> {
        val j = postJson(ApiDefs.Passport.CREATE_QR_LOGIN, "{}", passportQrHeaders()).jsonObject
        if (j.int("retcode") != 0) return "" to ""
        return (j.obj("data")?.str("url") ?: "") to (j.obj("data")?.str("ticket") ?: "")
    }

    /** 查询扫码状态,确认后返回 stoken_v2 + aid + mid(不再依赖 game_token) */
    fun queryPassportQRLoginStatus(ticket: String): PassportQrQueryResult {
        val body = buildJsonObject {
            put("ticket", ticket)
        }.toString()
        val j = postJson(ApiDefs.Passport.QUERY_QR_LOGIN_STATUS, body, passportQrHeaders()).jsonObject
        val retcode = j.int("retcode")
        if (retcode != 0) return PassportQrQueryResult(retcode)
        val data = j.obj("data")
        val status = data?.str("status") ?: ""
        var stoken = ""
        var aid = ""
        var mid = ""
        if (status == "Confirmed") {
            val tokens = data?.get("tokens")
            if (tokens is JsonArray) {
                for (element in tokens) {
                    val item = element.jsonObject
                    if (item.int("token_type") == 1) stoken = item.str("token")
                }
            }
            aid = data?.obj("user_info")?.str("aid") ?: ""
            mid = data?.obj("user_info")?.str("mid") ?: ""
        }
        return PassportQrQueryResult(retcode, status, stoken, aid, mid)
    }

    data class PassportQrQueryResult(
        val retcode: Int,
        val status: String = "",
        val stoken: String = "",
        val aid: String = "",
        val mid: String = ""
    )

    // ---------- 基础 ----------

    fun getMysUserName(uid: String): String {
        val headers = baseHeaders.toMutableMap()
        headers["DS"] = DS.gen2("", "uid=$uid", ApiDefs.MIHOYOBBS_SALT_X4)
        val j = getJson("${ApiDefs.Mys.USERINFO}?uid=$uid", headers).jsonObject
        if (j.int("retcode") != 0) return ""
        return j.obj("data")?.obj("user_info")?.str("nickname") ?: ""
    }

    /** 用 login_ticket 换 stoken+mid,返回 (retcode, stoken, mid) */
    fun getStokenByLoginTicket(loginTicket: String, loginUid: String): Triple<Int, String, String> {
        val url = "${ApiDefs.Takumi.MULTI_TOKEN}?login_ticket=$loginTicket&uid=$loginUid&token_types=3"
        val j = getJson(url, emptyMap()).jsonObject
        val retcode = j.int("retcode")
        if (retcode != 0) return Triple(retcode, "", "")
        var stoken = ""
        var mid = ""
        val list = j.obj("data")?.get("list")
        if (list is JsonArray) {
            for (element in list) {
                val item = element.jsonObject
                when (item.str("name")) {
                    "stoken" -> stoken = item.str("token")
                    "mid" -> mid = item.str("token")
                }
            }
        }
        return Triple(0, stoken, mid)
    }

    // ---------- 手机号验证码登录 ----------

    /** 创建手机号登录验证码,返回 (retcode, GeetestData),retcode==-3101 时需极验 */
    fun createLoginCaptcha(mobile: String, aigis: String = ""): Pair<Int, GeetestData> {
        val body = buildJsonObject {
            put("area_code", CryptoKit.rsaEncrypt("+86"))
            put("mobile", CryptoKit.rsaEncrypt(mobile))
        }.toString()
        val headers = baseHeaders.toMutableMap()
        headers["DS"] = DS.gen2(body, "", ApiDefs.MIHOYOBBS_SALT_PROD)
        if (aigis.isNotEmpty()) headers["X-Rpc-Aigis"] = aigis

        val request = Request.Builder()
            .url(ApiDefs.Passport.CREATE_CAPTCHA)
            .headers(buildHeaders(headers))
            .post(body.toRequestBody(JSON))
            .build()

        val (el, respHeaders) = executeWithHeaders(request)
        val j = el.jsonObject
        val retcode = j.int("retcode")
        val result = GeetestData()
        result.message = j.str("message")
        if (retcode == 0) {
            result.actionType = j.obj("data")?.str("action_type") ?: ""
            return retcode to result
        }
        if (retcode == -3101) {
            val aigisHeader = respHeaders["X-Rpc-Aigis"]
            if (!aigisHeader.isNullOrEmpty()) {
                try {
                    val aigisJson = json.parseToJsonElement(aigisHeader).jsonObject
                    val captchaJson = json.parseToJsonElement(aigisJson.str("data")).jsonObject
                    result.sessionId = aigisJson.str("session_id")
                    result.mmtType = aigisJson.int("mmt_type")
                    result.gt = captchaJson.str("gt")
                    result.challenge = captchaJson.str("challenge")
                    result.geetestType = ServerType.OFFICIAL
                } catch (_: Exception) {
                }
            }
        }
        return retcode to result
    }

    data class MobileCaptchaLoginResult(
        val retcode: Int,
        val V2Token: String = "",
        val aid: String = "",
        val mid: String = "",
        val name: String = ""
    )

    fun loginByMobileCaptcha(
        actionType: String,
        mobile: String,
        captcha: String,
        aigis: String = ""
    ): MobileCaptchaLoginResult {
        val body = buildJsonObject {
            put("area_code", CryptoKit.rsaEncrypt("+86"))
            put("action_type", actionType)
            put("captcha", captcha)
            put("mobile", CryptoKit.rsaEncrypt(mobile))
        }.toString()
        val headers = baseHeaders.toMutableMap()
        headers["DS"] = DS.gen2(body, "", ApiDefs.MIHOYOBBS_SALT_PROD)
        if (aigis.isNotEmpty()) headers["X-Rpc-Aigis"] = aigis

        val j = postJson(ApiDefs.Passport.LOGIN_BY_MOBILE_CAPTCHA, body, headers).jsonObject
        val retcode = j.int("retcode")
        if (retcode != 0) return MobileCaptchaLoginResult(retcode)
        val data = j.obj("data")
        return MobileCaptchaLoginResult(
            retcode = 0,
            V2Token = data?.obj("token")?.str("token") ?: "",
            aid = data?.obj("user_info")?.str("aid") ?: "",
            mid = data?.obj("user_info")?.str("mid") ?: "",
            name = data?.obj("user_info")?.str("account_name") ?: ""
        )
    }

    // ---------- 抢码(官服):panda scan 换 passport 二维码 + passport 确认登录 ----------

    /** 第一步:panda qrcode/scan,返回 passport_qr_url(空表示失败) */
    fun pandaScanQrLogin(url: String, ticket: String, gameType: GameType): String {
        val body = buildJsonObject {
            put("passport_app_id", "bll8iq97cem8")
            put("ticket", ticket)
            put("app_id", gameType.value)
            put("device", deviceId)
            put("ts", System.currentTimeMillis() / 1000)
        }.toString()
        val j = postJson(url, body, scanHeaders()).jsonObject
        if (j.int("retcode") != 0) return ""
        return j.obj("data")?.str("passport_qr_url") ?: ""
    }

    /** 从 passport 二维码 URL 中解析参数 */
    private fun passportQrParam(qrCode: String, key: String, terminators: String): String {
        val needle = "$key="
        val begin = qrCode.indexOf(needle)
        if (begin == -1) return ""
        val valueBegin = begin + needle.length
        var valueEnd = qrCode.length
        for (c in terminators) {
            val idx = qrCode.indexOf(c, valueBegin)
            if (idx != -1 && idx < valueEnd) valueEnd = idx
        }
        return qrCode.substring(valueBegin, valueEnd)
    }

    /** 第二步:passport scan / confirm,用 stoken+mid 确认登录(passport 两阶段,无需 game_token) */
    private fun passportQrLogin(qrCode: String, stoken: String, mid: String, confirm: Boolean): Boolean {
        val ticket = passportQrParam(qrCode, "tk", "&")
        val tokenTypes = passportQrParam(qrCode, "token_types", "#")
        if (ticket.isEmpty() || tokenTypes.isEmpty()) return false
        val body = buildJsonObject {
            put("ticket", ticket)
            put("token_types", buildJsonArray { add(tokenTypes) })
        }.toString()
        val headers = scanHeaders().toMutableMap()
        headers["Cookie"] = "stoken=$stoken; mid=$mid"
        val url = if (confirm) ApiDefs.Passport.CONFIRM_QR_LOGIN else ApiDefs.Passport.SCAN_QR_LOGIN
        val j = postJson(url, body, headers).jsonObject
        return j.int("retcode") == 0
    }

    /** 抢码确认登录(官服),stoken+mid 换游戏登录(两阶段 panda→passport) */
    fun confirmOfficialQrLogin(
        scanUrl: String,
        ticket: String,
        gameType: GameType,
        stoken: String,
        mid: String
    ): Boolean {
        val passportUrl = pandaScanQrLogin(scanUrl, ticket, gameType)
        if (passportUrl.isEmpty()) return false
        return confirmPassportQrLogin(passportUrl, stoken, mid)
    }

    /** 使用已换取的 passport 二维码完成 scan+confirm(passport 两阶段) */
    fun confirmPassportQrLogin(passportUrl: String, stoken: String, mid: String): Boolean {
        if (!passportQrLogin(passportUrl, stoken, mid, confirm = false)) return false
        return passportQrLogin(passportUrl, stoken, mid, confirm = true)
    }

    // ---------- 崩坏3B服 抢码 ----------

    private fun makeSign(data: JsonObject): String {
        val sb = StringBuilder()
        for ((key, value) in data) {
            if (key == "sign") continue
            val strVal = if (value is JsonPrimitive) value.content else value.toString()
            sb.append(key).append("=").append(strVal).append("&")
        }
        if (sb.isNotEmpty()) sb.deleteCharAt(sb.length - 1)
        return CryptoKit.hmacSha256(sb.toString(), "0ebc517adb1b62c6b408df153331f9aa")
    }

    private var oaCache: String? = null

    /** 获取崩坏3B服 OA 字符串(带缓存) */
    fun getOAString(): String {
        oaCache?.let { return it }
        return try {
            val text = client.newCall(Request.Builder().url(ApiDefs.BH3_OA_API).get().build())
                .execute().use { it.body?.string().orEmpty() }
            oaCache = text.ifEmpty { null }
            text
        } catch (_: Exception) {
            ""
        }
    }

    /** 崩坏3外部登录信息,返回 (retcode, open_id, combo_token, combo_id) */
    fun getBH3ExternalLoginInfo(uid: String, accessKey: String): BH3LoginInfo {
        val bodyData = """{"access_key":"$accessKey","uid":$uid}"""
        val body = buildJsonObject {
            put("device", "0000000000000000")
            put("app_id", 1)
            put("channel_id", 14)
            put("data", bodyData)
        }
        val sign = makeSign(body)
        val signed = LinkedHashMap<String, JsonElement>(body.entries.associate { it.key to it.value })
        signed["sign"] = JsonPrimitive(sign)
        val j = postJson(ApiDefs.Bh3.V2_LOGIN, JsonObject(signed).toString(), scanHeaders()).jsonObject
        val retcode = j.int("retcode")
        if (retcode != 0) return BH3LoginInfo(retcode)
        val data = j.obj("data")
        return BH3LoginInfo(
            retcode = 0,
            openId = data?.str("open_id") ?: "",
            comboToken = data?.str("combo_token") ?: "",
            comboId = data?.str("combo_id") ?: ""
        )
    }

    data class BH3LoginInfo(
        val retcode: Int,
        val openId: String = "",
        val comboToken: String = "",
        val comboId: String = ""
    )

    fun scanCheck(ticket: String): ScanRet {
        val body = buildJsonObject {
            put("app_id", "1")
            put("device", "0000000000000000")
            put("ticket", ticket)
            put("ts", System.currentTimeMillis() / 1000)
        }.toString()
        val j = postJson(ApiDefs.Bh3.QRCODE_SCAN, body, scanHeaders()).jsonObject
        return if (j.int("retcode") == 0) ScanRet.SUCCESS else ScanRet.FAILURE_1
    }

    fun scanConfirm(ticket: String, uid: String, accessKey: String, name: String): ScanRet {
        val info = getBH3ExternalLoginInfo(uid, accessKey)
        if (info.retcode != 0) return ScanRet.FAILURE_2

        val raw = buildJsonObject {
            put("heartbeat", false)
            put("open_id", info.openId)
            put("device_id", "0000000000000000")
            put("app_id", "1")
            put("channel_id", "14")
            put("combo_token", info.comboToken)
            put("asterisk_name", name)
            put("combo_id", info.comboId)
            put("account_type", "2")
        }.toString()

        val extData = buildJsonObject {
            put("accountType", "2")
            put("accountID", "")
            put("c", info.openId)
            put("accountToken", info.comboToken)
            put("dispatch", getOAString())
        }
        val ext = buildJsonObject {
            put("data", extData)
        }.toString()

        val postBody = buildJsonObject {
            put("device", "0000000000000000")
            put("app_id", 1)
            put("ts", System.currentTimeMillis() / 1000)
            put("ticket", ticket)
            put("payload", buildJsonObject {
                put("proto", "Combo")
                put("raw", raw)
                put("ext", ext)
            })
        }.toString()

        val j = postJson(ApiDefs.Bh3.QRCODE_CONFIRM, postBody, scanHeaders()).jsonObject
        return if (j.int("retcode") == 0) ScanRet.SUCCESS else ScanRet.FAILURE_2
    }
}
