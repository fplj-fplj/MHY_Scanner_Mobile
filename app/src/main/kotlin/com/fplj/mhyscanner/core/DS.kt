package com.fplj.mhyscanner.core

import kotlin.random.Random

object DS {

    /** 一代 DS 签名: salt + t + r 的 MD5 */
    fun gen1(salt: String = ApiDefs.MIHOYOBBS_SALT_X6): String {
        val t = System.currentTimeMillis() / 1000
        val r = Random.nextInt(100001, 200001)
        val m = "salt=$salt&t=$t&r=$r"
        return "$t,$r,${CryptoKit.md5(m)}"
    }

    /** 二代 DS 签名: salt + t + r + b + q 的 MD5 */
    fun gen2(body: String, query: String, salt: String = ApiDefs.MIHOYOBBS_SALT_X6): String {
        val t = System.currentTimeMillis() / 1000
        val r = Random.nextInt(100001, 200001)
        val m = "salt=$salt&t=$t&r=$r&b=$body&q=$query"
        return "$t,$r,${CryptoKit.md5(m)}"
    }
}
