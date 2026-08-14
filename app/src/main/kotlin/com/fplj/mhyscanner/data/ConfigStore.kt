package com.fplj.mhyscanner.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fplj.mhyscanner.core.CookieParser
import com.fplj.mhyscanner.core.MhyApi
import com.fplj.mhyscanner.core.ServerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "mhy_config")

/** 多账号配置存储,对应原项目 ConfigDate(config.json) */
class ConfigStore(private val context: Context) {

    private val configKey = stringPreferencesKey("config")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val configFlow: Flow<Config> = context.dataStore.data.map { prefs ->
        val raw = prefs[configKey] ?: return@map Config()
        try {
            json.decodeFromString<Config>(raw)
        } catch (_: Exception) {
            Config()
        }
    }

    suspend fun getConfig(): Config = configFlow.first()

    suspend fun save(config: Config) {
        context.dataStore.edit { prefs ->
            prefs[configKey] = json.encodeToString(Config.serializer(), config)
        }
    }

    suspend fun upsertAccount(account: Account) {
        val config = getConfig()
        val list = config.account.toMutableList()
        val index = list.indexOfFirst { it.uid.isNotEmpty() && it.uid == account.uid }
        if (index >= 0) list[index] = account else list.add(account)
        val selected = if (index >= 0) index else list.size - 1
        save(config.copy(account = list, num = list.size, lastAccount = selected))
    }

    suspend fun removeAccount(uid: String) {
        val config = getConfig()
        val list = config.account.filterNot { it.uid == uid }
        val last = if (list.isEmpty()) 0 else config.lastAccount.coerceIn(0, list.size - 1)
        save(config.copy(account = list, num = list.size, lastAccount = last))
    }

    suspend fun updateNote(uid: String, note: String) {
        val config = getConfig()
        save(config.copy(account = config.account.map { if (it.uid == uid) it.copy(note = note) else it }))
    }

    suspend fun setLastAccount(index: Int) {
        save(getConfig().copy(lastAccount = index))
    }

    suspend fun updateSettings(autoLogin: Boolean = false, autoExit: Boolean = false, autoStart: Boolean = false) {
        val config = getConfig()
        save(
            config.copy(
                autoLogin = autoLogin,
                autoExit = autoExit,
                autoStart = autoStart
            )
        )
    }
}

/** 从 Cookie 字符串解析出账号信息(复刻原项目 Tab2 流程) */
object CookieAccountParser {

    sealed interface Result {
        data class Success(val account: Account, val typeName: String) : Result
        data class Error(val message: String) : Result
    }

    fun parse(cookie: String, onStokenByTicket: (String, String) -> Pair<Int, Pair<String, String>>): Result {
        val cp = CookieParser(cookie)
        val uid = listOf("stuid", "ltuid", "account_id").firstNotNullOfOrNull { cp[it] }
            ?: return Result.Error("Cookie格式错误")
        val stoken: String
        val mid: String
        val cookieStoken = cp["stoken"]
        if (cookieStoken != null) {
            stoken = cookieStoken
            mid = cp["mid"] ?: ""
        } else {
            val loginTicket = cp["login_ticket"]
                ?: return Result.Error("Cookie中没有有效的SToken")
            val (code, tokens) = onStokenByTicket(loginTicket, uid)
            if (code != 0 || tokens.first.isEmpty()) return Result.Error("Cookie中没有有效的SToken")
            stoken = tokens.first
            mid = tokens.second
        }
        if (mid.isEmpty()) return Result.Error("Cookie格式错误")
        return Result.Success(Account(accessKey = stoken, uid = uid, mid = mid), "官服")
    }

    fun resolveName(uid: String): String = runCatching { MhyApi.getMysUserName(uid) }.getOrElse { "" }
}

fun ServerType.toDisplayName(): String = when (this) {
    ServerType.OFFICIAL -> "官服"
    ServerType.BH3_BILI -> "崩坏3B服"
    else -> "未知"
}