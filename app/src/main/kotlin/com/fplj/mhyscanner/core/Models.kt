package com.fplj.mhyscanner.core

enum class ServerType {
    UNKNOWN,
    OFFICIAL,
    BH3_BILI;

    fun toTypeName(): String = when (this) {
        OFFICIAL -> "官服"
        BH3_BILI -> "崩坏3B服"
        UNKNOWN -> ""
    }

    companion object {
        fun fromTypeName(name: String): ServerType = when (name) {
            "官服" -> OFFICIAL
            "崩坏3B服" -> BH3_BILI
            else -> UNKNOWN
        }
    }
}

enum class GameType(val value: Int) {
    UNKNOWN(0),
    HONKAI3(1),
    TEARS_OF_THEMIS(2),
    GENSHIN(4),
    PLATFORM_APP(5),
    HONKAI2(7),
    HONKAI_STAR_RAIL(8),
    CLOUD_GAME(9),
    _3NNN(10),
    PJSH(11),
    ZENLESS_ZONE_ZERO(12),
    HYG(13),
    HONKAI3_BILI(10000)
}

enum class ScanRet {
    UNKNOWN,
    SUCCESS,
    FAILURE_1,
    FAILURE_2,
    LIVE_STOP,
    STREAM_ERROR
}

enum class LoginQRCodeState {
    INIT,
    SCANNED,
    CONFIRMED,
    EXPIRED
}

data class GeetestData(
    var actionType: String = "",
    var sessionId: String = "",
    var mmtType: Int = 0,
    var gt: String = "",
    var challenge: String = "",
    var message: String = "",
    var geetestType: ServerType = ServerType.UNKNOWN
)

data class QrGameEntry(
    val tag: String,
    val gameType: GameType,
    val scanUrl: String,
    val confirmUrl: String
)

object QrGameMap {
    private val entries = listOf(
        QrGameEntry("8F3", GameType.HONKAI3, ApiDefs.Bh3.QRCODE_SCAN, ApiDefs.Bh3.QRCODE_CONFIRM),
        QrGameEntry("9E&", GameType.GENSHIN, ApiDefs.Hk4e.QRCODE_SCAN, ApiDefs.Hk4e.QRCODE_CONFIRM),
        QrGameEntry("8F%", GameType.HONKAI_STAR_RAIL, ApiDefs.Hkrpg.QRCODE_SCAN, ApiDefs.Hkrpg.QRCODE_CONFIRM),
        QrGameEntry("%BA", GameType.ZENLESS_ZONE_ZERO, ApiDefs.Nap.QRCODE_SCAN, ApiDefs.Nap.QRCODE_CONFIRM)
    )

    fun match(tag: String): QrGameEntry? = entries.firstOrNull { it.tag == tag }
}
