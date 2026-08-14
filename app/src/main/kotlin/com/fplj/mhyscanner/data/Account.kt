package com.fplj.mhyscanner.data

import com.fplj.mhyscanner.core.ServerType
import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val accessKey: String = "",
    val uid: String = "",
    val name: String = "",
    val type: String = "",
    val note: String = "",
    val mid: String = ""
) {
    val serverType: ServerType
        get() = ServerType.fromTypeName(type)
}

@Serializable
data class Config(
    val autoExit: Boolean = false,
    val autoLogin: Boolean = false,
    val autoStart: Boolean = false,
    val account: List<Account> = emptyList(),
    val lastAccount: Int = 0,
    val num: Int = 0
)