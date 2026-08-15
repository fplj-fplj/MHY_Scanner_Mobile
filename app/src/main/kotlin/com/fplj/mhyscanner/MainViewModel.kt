package com.fplj.mhyscanner

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.media.projection.MediaProjection
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fplj.mhyscanner.core.MhyApi
import com.fplj.mhyscanner.core.ScanRet
import com.fplj.mhyscanner.core.ServerType
import com.fplj.mhyscanner.data.Account
import com.fplj.mhyscanner.data.Config
import com.fplj.mhyscanner.data.ConfigStore
import com.fplj.mhyscanner.data.CookieAccountParser
import com.fplj.mhyscanner.engine.ScanEngine
import com.fplj.mhyscanner.live.LiveStream
import com.fplj.mhyscanner.log.AppLog
import com.fplj.mhyscanner.scanner.QrScanner
import com.fplj.mhyscanner.service.ScanService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val config: Config = Config(),
    val scanning: Boolean = false,
    val status: String = "",
    val qrImage: Bitmap? = null,
    val qrStatus: String = "",
    val addingAccount: Boolean = false,
    val phoneState: PhoneLoginState = PhoneLoginState(),
    val pendingConfirmGame: String? = null
)

data class PhoneLoginState(
    val step: Int = 0, // 0:idle 1:sms已发送 2:待极验
    val actionType: String = "",
    val gt: String = "",
    val challenge: String = "",
    val sessionId: String = ""
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val logTag = "VM"
    private val configStore = ConfigStore(application)
    private val engine = ScanEngine(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** 账号添加成功信号,用于让"添加账号"弹窗自动关闭 */
    private val _accountAdded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val accountAdded: SharedFlow<Unit> = _accountAdded.asSharedFlow()

    private var projection: MediaProjection? = null
    private var screenActive = false
    private var qrLoginJob: Job? = null

    init {
        viewModelScope.launch {
            configStore.configFlow.collectLatest { cfg ->
                _uiState.value = _uiState.value.copy(config = cfg)
            }
        }
        viewModelScope.launch {
            engine.events.collect { event ->
                when (event) {
                    is ScanEngine.ScanEvent.Status -> setStatus(event.text)
                    is ScanEngine.ScanEvent.QrDetected -> setStatus("成功识别到二维码(${event.game})")
                    is ScanEngine.ScanEvent.ConfirmRequest -> {
                        _uiState.value = _uiState.value.copy(scanning = false, pendingConfirmGame = event.game)
                        stopProjection()
                    }
                    is ScanEngine.ScanEvent.Result -> {
                        notifyResult(event.ret)
                        stopProjection()
                    }
                    is ScanEngine.ScanEvent.Error -> {
                        setStatus("扫描错误:${event.text}")
                        stopProjection()
                    }
                }
            }
        }
    }

    // ---------- 账号选择 ----------

    fun selectAccount(index: Int) {
        viewModelScope.launch {
            configStore.setLastAccount(index)
        }
    }

    fun deleteAccount(uid: String) {
        viewModelScope.launch { configStore.removeAccount(uid) }
    }

    fun updateNote(uid: String, note: String) {
        viewModelScope.launch { configStore.updateNote(uid, note) }
    }

    fun updateSettings(
        autoLogin: Boolean? = null,
        autoExit: Boolean? = null,
        autoStart: Boolean? = null,
        floatingLogEnabled: Boolean? = null
    ) {
        val c = _uiState.value.config
        viewModelScope.launch {
            configStore.updateSettings(
                autoLogin = autoLogin ?: c.autoLogin,
                autoExit = autoExit ?: c.autoExit,
                autoStart = autoStart ?: c.autoStart,
                floatingLogEnabled = floatingLogEnabled ?: c.floatingLogEnabled
            )
        }
    }

    // ---------- 添加账号:手机扫码 ----------

    fun startQrLogin() {
        qrLoginJob?.cancel()
        _uiState.value = _uiState.value.copy(addingAccount = true)
        qrLoginJob = viewModelScope.launch {
            try {
                AppLog.info(logTag, "创建 passport 登录二维码")
                val (url, ticket) = withContext(Dispatchers.IO) { MhyApi.createPassportQRLogin() }
                if (url.isEmpty() || ticket.isEmpty()) {
                    AppLog.warn(logTag, "创建 passport 二维码失败(url=${url.length} ticket=${ticket.length})")
                    _messages.emit("获取二维码失败")
                    finishAdd()
                    return@launch
                }
                val image = QrScanner.renderQr(url)
                if (image == null) {
                    AppLog.error(logTag, "二维码渲染失败")
                    _messages.emit("二维码渲染失败")
                    finishAdd()
                    return@launch
                }
                _uiState.value = _uiState.value.copy(qrImage = image, qrStatus = "请使用米游社扫码")

                while (isActive) {
                    kotlinx.coroutines.delay(1000)
                    val r = withContext(Dispatchers.IO) { MhyApi.queryPassportQRLoginStatus(ticket) }
                    if (r.retcode != 0 || r.status == "Expired") {
                        AppLog.warn(logTag, "二维码状态异常: retcode=${r.retcode} status=${r.status}")
                        _uiState.value = _uiState.value.copy(qrStatus = "二维码已过期,请重新添加")
                        break
                    }
                    when (r.status) {
                        "Created" -> {}
                        "Scanned" -> {
                            AppLog.info(logTag, "二维码已被扫码,等待确认")
                            _uiState.value = _uiState.value.copy(qrStatus = "已扫码,请在手机上确认")
                        }
                        "Confirmed" -> {
                            if (r.stoken.isEmpty() || r.aid.isEmpty()) {
                                AppLog.error(logTag, "确认后缺少 stoken/aid")
                                _uiState.value = _uiState.value.copy(qrStatus = "获取凭证失败,请重新添加")
                                break
                            }
                            val name = withContext(Dispatchers.IO) {
                                runCatching { MhyApi.getMysUserName(r.aid) }.getOrDefault(r.aid)
                            }
                            configStore.upsertAccount(
                                Account(
                                    accessKey = r.stoken,
                                    uid = r.aid,
                                    mid = r.mid,
                                    name = name,
                                    type = ServerType.OFFICIAL.toTypeName()
                                )
                            )
                            AppLog.info(logTag, "passport 扫码登录成功: uid=${r.aid} name=$name")
                            _messages.emit("账号添加成功")
                            _accountAdded.emit(Unit)
                            finishAdd()
                            return@launch
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                AppLog.error(logTag, "扫码登录网络异常: ${e.message}")
                _messages.emit("网络异常:${e.message ?: "未知错误"}")
                finishAdd()
            }
        }
    }

    fun stopQrLogin() {
        qrLoginJob?.cancel()
        qrLoginJob = null
        finishAdd()
    }

    private fun finishAdd() {
        _uiState.value = _uiState.value.copy(addingAccount = false, qrImage = null, qrStatus = "")
    }

    // ---------- 添加账号:手机号验证码 ----------

    fun sendSmsCode(phone: String) {
        if (phone.length != 11) {
            viewModelScope.launch { _messages.emit("请输入11位手机号") }
            return
        }
        _lastPhone = phone
        viewModelScope.launch {
            setStatus("发送验证码...")
            try {
                AppLog.info(logTag, "发送验证码 phone=$phone")
                val (retcode, data) = withContext(Dispatchers.IO) { MhyApi.createLoginCaptcha(phone) }
                when {
                    retcode == 0 -> {
                        setStatus("验证码已发送")
                        _uiState.value = _uiState.value.copy(
                            phoneState = PhoneLoginState(step = 1, actionType = data.actionType)
                        )
                    }
                    retcode == -3101 -> {
                        _uiState.value = _uiState.value.copy(
                            phoneState = PhoneLoginState(
                                step = 2,
                                gt = data.gt,
                                challenge = data.challenge,
                                sessionId = data.sessionId
                            )
                        )
                        setStatus("请完成滑块验证")
                    }
                    retcode == -3006 -> setStatus("请求过于频繁,请稍后再试")
                    retcode == -3008 -> setStatus("手机号错误")
                    else -> setStatus("发送失败(retcode=$retcode ${data.message})")
                }
                AppLog.warn(logTag, "发送验证码结果 retcode=$retcode")
            } catch (e: Exception) {
                AppLog.error(logTag, "发送验证码异常: ${e.message}")
                setStatus("网络异常:${e.message ?: "未知错误"}")
            }
        }
    }

    /** 极验滑块通过后回调 validate(JSON 字符串) */
    fun onGeetestPassed(geetestJson: String) {
        viewModelScope.launch {
            val state = _uiState.value.phoneState
            val aigis = state.sessionId + ";" +
                android.util.Base64.encodeToString(geetestJson.toByteArray(), android.util.Base64.NO_WRAP)
            try {
                val (retcode, data) = withContext(Dispatchers.IO) { MhyApi.createLoginCaptcha(_lastPhone, aigis) }
                if (retcode == 0) {
                    setStatus("验证码已发送")
                    _uiState.value = _uiState.value.copy(
                        phoneState = PhoneLoginState(step = 1, actionType = data.actionType)
                    )
                } else {
                    AppLog.warn(logTag, "极验回调失败 retcode=$retcode")
                    setStatus("验证失败(retcode=$retcode ${data.message})")
                    _uiState.value = _uiState.value.copy(phoneState = PhoneLoginState())
                }
            } catch (e: Exception) {
                AppLog.error(logTag, "极验回调异常: ${e.message}")
                setStatus("网络异常:${e.message ?: "未知错误"}")
            }
        }
    }

    fun cancelGeetest() {
        _uiState.value = _uiState.value.copy(phoneState = PhoneLoginState())
    }

    private var _lastPhone: String = ""

    fun submitSmsCode(phone: String, code: String) {
        _lastPhone = phone
        viewModelScope.launch {
            try {
                AppLog.info(logTag, "提交短信验证码 phone=$phone")
                val result = withContext(Dispatchers.IO) {
                    MhyApi.loginByMobileCaptcha(_uiState.value.phoneState.actionType, phone, code)
                }
                when {
                    result.retcode == -3205 -> setStatus("短信验证码错误")
                    result.retcode == 0 -> {
                        val name = result.name.ifEmpty { result.aid }
                        configStore.upsertAccount(
                            Account(
                                accessKey = result.V2Token,
                                uid = result.aid,
                                mid = result.mid,
                                name = name,
                                type = ServerType.OFFICIAL.toTypeName()
                            )
                        )
                        _uiState.value = _uiState.value.copy(phoneState = PhoneLoginState())
                        AppLog.info(logTag, "手机号登录成功 uid=${result.aid}")
                        _messages.emit("账号添加成功")
                        _accountAdded.emit(Unit)
                    }
                    else -> setStatus("登录失败,请稍后再试")
                }
                if (result.retcode != 0) AppLog.warn(logTag, "短信登录失败 retcode=${result.retcode}")
            } catch (e: Exception) {
                AppLog.error(logTag, "短信登录异常: ${e.message}")
                setStatus("网络异常:${e.message ?: "未知错误"}")
            }
        }
    }

    // ---------- 添加账号:Cookie ----------

    fun addAccountByCookie(cookieRaw: String) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val parsed = CookieAccountParser.parse(cookieRaw) { loginTicket, uid ->
                        val t = MhyApi.getStokenByLoginTicket(loginTicket, uid)
                        t.first to (t.second to t.third)
                    }
                    when (parsed) {
                        is CookieAccountParser.Result.Error -> parsed.message to null
                        is CookieAccountParser.Result.Success -> {
                            val name = CookieAccountParser.resolveName(parsed.account.uid)
                            parsed.account.copy(name = name).let { "" to it }
                        }
                    }
                }
                val (err, account) = result
                if (account == null) {
                    AppLog.warn(logTag, "Cookie 添加账号失败: $err")
                    _messages.emit(err.ifEmpty { "Cookie格式错误" })
                } else {
                    configStore.upsertAccount(account)
                    AppLog.info(logTag, "Cookie 添加账号成功 uid=${account.uid}")
                    _messages.emit("账号添加成功")
                    _accountAdded.emit(Unit)
                }
            } catch (e: Exception) {
                AppLog.error(logTag, "Cookie 添加账号异常: ${e.message}")
                _messages.emit("网络异常:${e.message ?: "未知错误"}")
            }
        }
    }

    // ---------- 抢码 ----------

    fun startStreamScan(platform: LiveStream.Platform, rid: String) {
        viewModelScope.launch {
            when (val t = resolveTarget()) {
                is TargetResult.NoAccount -> {
                    _messages.emit("请先在账号页选择账号")
                    return@launch
                }
                is TargetResult.Invalid -> {
                    AppLog.warn(logTag, "开始直播扫描失败: ${t.reason}")
                    _messages.emit(t.reason)
                    return@launch
                }
                is TargetResult.Ready -> {
                    AppLog.info(logTag, "开始直播扫描 platform=$platform rid=$rid uid=${t.target.uid}")
                    screenActive = false
                    setStatus("获取直播流地址...")
                    val info = withContext(Dispatchers.IO) { LiveStream.getStreamInfo(platform, rid) }
                    if (!info.ok) {
                        AppLog.warn(logTag, "获取直播流失败: ${info.statusText}")
                        setStatus(info.statusText)
                        return@launch
                    }
                    engine.startStreamScan(info.url, t.target, _uiState.value.config.autoLogin)
                    _uiState.value = _uiState.value.copy(scanning = true)
                }
            }
        }
    }

    fun startScreenScan(resultCode: Int, data: Intent) {
        viewModelScope.launch {
            when (val t = resolveTarget()) {
                is TargetResult.NoAccount -> {
                    _messages.emit("请先在账号页选择账号")
                    return@launch
                }
                is TargetResult.Invalid -> {
                    AppLog.warn(logTag, "开始屏幕扫描失败: ${t.reason}")
                    _messages.emit(t.reason)
                    return@launch
                }
                is TargetResult.Ready -> {
                    val proj = getProjection(resultCode, data)
                    if (proj == null) {
                        AppLog.error(logTag, "屏幕捕获授权失败")
                        _messages.emit("屏幕捕获授权失败")
                        return@launch
                    }
                    projection = proj
                    screenActive = true
                    engine.startScreenScan(proj, t.target, _uiState.value.config.autoLogin)
                    _uiState.value = _uiState.value.copy(scanning = true)
                }
            }
        }
    }

    fun stopScan() {
        AppLog.info(logTag, "手动停止扫描")
        engine.stop()
        stopProjection()
        setStatus("已停止")
    }

    fun confirmLogin(confirm: Boolean) {
        AppLog.info(logTag, "用户确认登录: confirm=$confirm")
        _uiState.value = _uiState.value.copy(pendingConfirmGame = null)
        engine.confirmLogin(confirm)
        stopProjection()
    }

    private sealed interface TargetResult {
        object NoAccount : TargetResult
        data class Invalid(val reason: String) : TargetResult
        data class Ready(val target: ScanEngine.ScanTarget) : TargetResult
    }

    private suspend fun resolveTarget(): TargetResult {
        val cfg = _uiState.value.config
        val account = cfg.account.getOrNull(cfg.lastAccount)
        if (account == null) {
            return if (cfg.account.isEmpty()) TargetResult.NoAccount
            else TargetResult.Invalid("所选账号不存在,请重新选择")
        }
        return withContext(Dispatchers.IO) {
            when (account.serverType) {
                ServerType.OFFICIAL -> {
                    if (account.accessKey.isEmpty()) {
                        TargetResult.Invalid("该账号缺少登录凭证,请删除后重新添加")
                    } else {
                        val stoken = account.accessKey
                        val mid = account.mid
                        if (stoken.isEmpty()) {
                            TargetResult.Invalid("该账号缺少登录凭证,请删除后重新添加")
                        } else {
                            TargetResult.Ready(
                                ScanEngine.ScanTarget(
                                    serverType = ServerType.OFFICIAL,
                                    uid = account.uid,
                                    stoken = stoken,
                                    mid = mid
                                )
                            )
                        }
                    }
                }
                ServerType.BH3_BILI -> {
                    if (account.accessKey.isEmpty()) {
                        TargetResult.Invalid("该账号缺少登录凭证,请删除后重新添加")
                    } else {
                        TargetResult.Ready(
                            ScanEngine.ScanTarget(
                                serverType = ServerType.BH3_BILI,
                                uid = account.uid,
                                stoken = account.accessKey,
                                uname = account.name
                            )
                        )
                    }
                }
                else -> TargetResult.Invalid("不支持的账号类型,请删除后重新添加")
            }
        }
    }

    private suspend fun getProjection(resultCode: Int, data: Intent): MediaProjection? =
        ScanService.startProjection(getApplication(), resultCode, data).await()

    private fun stopProjection() {
        screenActive = false
        ScanService.stopProjection()
        projection = null
        _uiState.value = _uiState.value.copy(scanning = false)
    }

    fun showMessage(text: String) {
        AppLog.info(logTag, "提示: $text")
        viewModelScope.launch { _messages.emit(text) }
    }

    private fun setStatus(text: String) {
        if (text != _uiState.value.status) AppLog.info(logTag, "状态: $text")
        _uiState.value = _uiState.value.copy(status = text)
    }

    private suspend fun notifyResult(ret: ScanRet) {
        AppLog.info(logTag, "扫描结果: $ret")
        setStatus(
            when (ret) {
                ScanRet.SUCCESS -> "登录成功"
                ScanRet.FAILURE_1 -> "扫码失败"
                ScanRet.FAILURE_2 -> "确认登录失败"
                ScanRet.LIVE_STOP -> "直播流结束"
                ScanRet.STREAM_ERROR -> "直播流错误"
                else -> "未知结果"
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        engine.stop()
        runCatching { projection?.stop() }
        projection = null
    }
}