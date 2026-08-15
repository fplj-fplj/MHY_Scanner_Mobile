package com.fplj.mhyscanner.engine

import android.content.Context
import com.fplj.mhyscanner.core.GameType
import com.fplj.mhyscanner.core.MhyApi
import com.fplj.mhyscanner.core.QrGameEntry
import com.fplj.mhyscanner.core.QrGameMap
import com.fplj.mhyscanner.core.ScanRet
import com.fplj.mhyscanner.core.ServerType
import com.fplj.mhyscanner.live.ExoFrameSource
import com.fplj.mhyscanner.scanner.Frame
import com.fplj.mhyscanner.scanner.FrameSource
import com.fplj.mhyscanner.scanner.QrScanner
import com.fplj.mhyscanner.screen.ScreenFrameSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 抢码引擎:从直播流 / 屏幕持续取帧识别二维码,识别到后自动"扫码"并(可选)自动"确认登录"。
 * 复刻原项目 QRCodeForStream / QRCodeForScreen 的完整流程。
 */
class ScanEngine(private val context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val scanMutex = Mutex()

    @Volatile
    private var running = false
    private var lastTicket = ""
    private var currentJob: Job? = null
    private var currentSource: FrameSource? = null
    private var currentTarget: ScanTarget? = null
    private var currentAutoLogin = false
    private var pendingConfirm: PendingConfirm? = null

    private val _events = kotlinx.coroutines.flow.MutableSharedFlow<ScanEvent>(extraBufferCapacity = 64)
    val events = _events

    sealed interface ScanEvent {
        data class Status(val text: String) : ScanEvent
        data class QrDetected(val game: String) : ScanEvent
        data class ConfirmRequest(val game: String) : ScanEvent
        data class Result(val ret: ScanRet) : ScanEvent
        data class Error(val text: String) : ScanEvent
    }

    data class ScanTarget(
        val serverType: ServerType,
        val uid: String,
        val stoken: String = "",
        val mid: String = "",
        val uname: String = ""
    )

    private data class PendingConfirm(
        val entry: QrGameEntry?,
        val ticket: String,
        val target: ScanTarget,
        val bh3: Boolean,
        val passportUrl: String = ""
    )

    val isRunning: Boolean get() = running

    // ---------- 启动 ----------

    fun startStreamScan(url: String, target: ScanTarget, autoLogin: Boolean) {
        stop()
        running = true
        lastTicket = ""
        currentTarget = target
        currentAutoLogin = autoLogin
        currentJob = scope.launch {
            _events.emit(ScanEvent.Status("连接直播流..."))
            val source = ExoFrameSource(appContext, url)
            currentSource = source
            if (!source.open(::onFrame, { msg ->
                if (running) _events.tryEmit(ScanEvent.Error(msg))
            })) {
                running = false
                return@launch
            }
            _events.emit(ScanEvent.Status("扫描直播流中..."))
            while (running) {
                delay(300)
            }
        }
    }

    fun startScreenScan(
        projection: android.media.projection.MediaProjection,
        target: ScanTarget,
        autoLogin: Boolean
    ) {
        stop()
        running = true
        lastTicket = ""
        currentTarget = target
        currentAutoLogin = autoLogin
        currentJob = scope.launch {
            _events.emit(ScanEvent.Status("开启屏幕捕获..."))
            val source = ScreenFrameSource(appContext, projection)
            currentSource = source
            if (!source.open(::onFrame, { msg ->
                if (running) _events.tryEmit(ScanEvent.Error(msg))
            })) {
                running = false
                return@launch
            }
            _events.emit(ScanEvent.Status("扫描屏幕中..."))
            while (running) {
                delay(300)
            }
        }
    }

    // ---------- 帧处理 ----------

    private fun onFrame(frame: Frame) {
        if (!running) return
        val text = when {
            frame.hasLuma -> QrScanner.decodeLuma(
                frame.luma!!, frame.dataWidth, frame.dataHeight,
                frame.left, frame.top, frame.width, frame.height
            )
            frame.hasRgb -> QrScanner.decodeRgb(frame.rgb!!, frame.rgbWidth, frame.rgbHeight)
            else -> null
        }
        if (text != null && text.length >= 85) {
            scope.launch { process(text) }
        }
    }

    private suspend fun process(text: String) {
        if (!scanMutex.tryLock()) return
        try {
            if (!running) return
            val target = currentTarget ?: return
            val tag = if (text.length >= 82) text.substring(79, 82) else ""
            val ticket = text.substring(text.length - 24)
            if (ticket == lastTicket) return

            when (target.serverType) {
                ServerType.OFFICIAL -> {
                    val entry = QrGameMap.match(tag) ?: return
                    val passportUrl = MhyApi.pandaScanQrLogin(entry.scanUrl, ticket, entry.gameType)
                    if (passportUrl.isEmpty()) {
                        _events.emit(ScanEvent.Result(ScanRet.FAILURE_1))
                        stopInternal()
                        return
                    }
                    lastTicket = ticket
                    _events.emit(ScanEvent.QrDetected(entry.gameType.name))
                    if (currentAutoLogin) {
                        val ok = MhyApi.confirmOfficialQrLogin(
                            entry.scanUrl, ticket, entry.gameType, target.stoken, target.mid
                        )
                        _events.emit(ScanEvent.Result(if (ok) ScanRet.SUCCESS else ScanRet.FAILURE_2))
                    } else {
                        pendingConfirm = PendingConfirm(entry, ticket, target, bh3 = false, passportUrl = passportUrl)
                        _events.emit(ScanEvent.ConfirmRequest(entry.gameType.name))
                    }
                    stopInternal()
                }

                ServerType.BH3_BILI -> {
                    if (tag != "8F3") return
                    if (MhyApi.scanCheck(ticket) != ScanRet.SUCCESS) {
                        _events.emit(ScanEvent.Result(ScanRet.FAILURE_1))
                        stopInternal()
                        return
                    }
                    lastTicket = ticket
                    _events.emit(ScanEvent.QrDetected(GameType.HONKAI3_BILI.name))
                    if (currentAutoLogin) {
                        val ret = MhyApi.scanConfirm(ticket, target.uid, target.stoken, target.uname)
                        _events.emit(ScanEvent.Result(ret))
                    } else {
                        pendingConfirm = PendingConfirm(null, ticket, target, bh3 = true)
                        _events.emit(ScanEvent.ConfirmRequest("崩坏3B服"))
                    }
                    stopInternal()
                }

                else -> {}
            }
        } finally {
            scanMutex.unlock()
        }
    }

    // ---------- 手动确认 / 停止 ----------

    fun confirmLogin(confirm: Boolean) {
        val p = pendingConfirm ?: run { stop(); return }
        pendingConfirm = null
        if (!confirm) {
            stop()
            return
        }
        scope.launch {
            val ret = if (p.bh3) {
                MhyApi.scanConfirm(p.ticket, p.target.uid, p.target.stoken, p.target.uname)
            } else if (p.entry != null) {
                if (MhyApi.confirmPassportQrLogin(p.passportUrl, p.target.stoken, p.target.mid)) {
                    ScanRet.SUCCESS
                } else {
                    ScanRet.FAILURE_2
                }
            } else {
                ScanRet.FAILURE_2
            }
            _events.emit(ScanEvent.Result(ret))
        }
    }

    fun stop() {
        stopInternal()
    }

    private fun stopInternal() {
        running = false
        currentJob?.cancel()
        currentJob = null
        runCatching { currentSource?.close() }
        currentSource = null
    }
}