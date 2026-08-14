package com.fplj.mhyscanner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreKitTest {

    @Test
    fun md5_is_hex_32() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", CryptoKit.md5(""))
        assertEquals("900150983cd24fb0d6963f7d28e17f72", CryptoKit.md5("abc"))
    }

    @Test
    fun hmacSha256_matches_known_vector() {
        val result = CryptoKit.hmacSha256("key", "")
        assertEquals("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843", result)
    }

    @Test
    fun rsa_encrypt_produces_base64() {
        val encrypted = CryptoKit.rsaEncrypt("+86")
        assertTrue(encrypted.isNotBlank())
        // Base64 可解码
        java.util.Base64.getDecoder().decode(encrypted)
    }

    @Test
    fun ds_gen1_format() {
        val ds = DS.gen1("test_salt")
        val parts = ds.split(",")
        assertEquals(3, parts.size)
        assertTrue(parts[0].toLongOrNull() != null)
        assertTrue(parts[1].toLongOrNull() != null)
        assertEquals(32, parts[2].length)
        assertTrue(parts[2].matches(Regex("[0-9a-f]{32}")))
    }

    @Test
    fun ds_gen2_matches_manual_calculation() {
        val salt = "test_salt"
        val body = "{\"a\":1}"
        val query = "uid=123"
        val ds = DS.gen2(body, query, salt)
        val parts = ds.split(",")
        val t = parts[0]
        val r = parts[1]
        val expected = CryptoKit.md5("salt=$salt&t=$t&r=$r&b=$body&q=$query")
        assertEquals(expected, parts[2])
    }

    @Test
    fun cookie_parser_handles_spaces_and_quotes() {
        val cookie = "stuid=12345; bbs mustard=1.2.3; account_id=67890; "
        val cp = CookieParser(cookie)
        assertEquals("12345", cp["stuid"])
        assertEquals("1.2.3", cp["bbs mustard"])
        assertEquals("67890", cp["account_id"])
    }

    @Test
    fun server_type_round_trip() {
        assertEquals(ServerType.OFFICIAL, ServerType.fromTypeName("官服"))
        assertEquals(ServerType.BH3_BILI, ServerType.fromTypeName("崩坏3B服"))
        assertEquals(ServerType.UNKNOWN, ServerType.fromTypeName("不存在"))
    }
}