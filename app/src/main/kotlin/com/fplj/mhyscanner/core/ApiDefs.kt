package com.fplj.mhyscanner.core

object ApiDefs {
    const val MIHOYOBBS_VERSION = "2.95.1"
    const val MIHOYOBBS_SALT = "oqrJbPCoFhWhFBNDvVRuldbrbiVxyWsP"
    const val MIHOYOBBS_SALT_WEB = "zZDfHqEcwTqvvKDmqRcHyqqurxGgLfBV"
    const val MIHOYOBBS_SALT_X4 = "xV8v4Qu54lUKrEYFZkJhB8cuOh9Asafs"
    const val MIHOYOBBS_SALT_X6 = "t0qEgfub6cvueAPgR5m9aQWWVciEer7v"
    const val MIHOYOBBS_SALT_PROD = "JwYDpKvLj6MrMqqYU6jTKF17KNO2PXoS"
    const val MIHOYOBBS_SALT_K2 = "sfYPEgpxkOe1I3XVMLdwp1Lyt9ORgZsq"
    const val MIHOYOBBS_SALT_LK2 = "sidQFEglajEz7FA0Aj7HQPV88zpf17SO"

    const val API_SDK = "https://api-sdk.mihoyo.com"

    object Bh3 {
        const val BASE = "$API_SDK/bh3_cn"
        const val V2_LOGIN = "$BASE/combo/granter/login/v2/login"
        const val QRCODE_SCAN = "$BASE/combo/panda/qrcode/scan"
        const val QRCODE_CONFIRM = "$BASE/combo/panda/qrcode/confirm"
    }

    object Hk4e {
        const val BASE = "$API_SDK/hk4e_cn"
        const val QRCODE_SCAN = "$BASE/combo/panda/qrcode/scan"
        const val QRCODE_CONFIRM = "$BASE/combo/panda/qrcode/confirm"
    }

    object Hkrpg {
        const val BASE = "$API_SDK/hkrpg_cn"
        const val QRCODE_SCAN = "$BASE/combo/panda/qrcode/scan"
        const val QRCODE_CONFIRM = "$BASE/combo/panda/qrcode/confirm"
    }

    object Nap {
        const val BASE = "$API_SDK/nap_cn"
        const val QRCODE_SCAN = "$BASE/combo/panda/qrcode/scan"
        const val QRCODE_CONFIRM = "$BASE/combo/panda/qrcode/confirm"
    }

    object Takumi {
        const val BASE = "https://api-takumi.mihoyo.com"
        const val MULTI_TOKEN = "$BASE/auth/api/getMultiTokenByLoginTicket"
    }

    object Passport {
        const val BASE = "https://passport-api.mihoyo.com"
        const val CREATE_CAPTCHA = "$BASE/account/ma-cn-verifier/verifier/createLoginCaptcha"
        const val LOGIN_BY_MOBILE_CAPTCHA = "$BASE/account/ma-cn-passport/app/loginByMobileCaptcha"
        const val CREATE_QR_LOGIN = "$BASE/account/ma-cn-passport/app/createQRLogin"
        const val QUERY_QR_LOGIN_STATUS = "$BASE/account/ma-cn-passport/app/queryQRLoginStatus"
        const val SCAN_QR_LOGIN = "$BASE/account/ma-cn-passport/app/scanQRLogin"
        const val CONFIRM_QR_LOGIN = "$BASE/account/ma-cn-passport/app/confirmQRLogin"
    }

    object Mys {
        const val BASE = "https://bbs-api.miyoushe.com"
        const val USERINFO = "$BASE/user/api/getUserFullInfo"
    }

    object GameBili {
        const val BASE = "https://line1-sdk-center-login-sh.biligame.net"
        const val USERINFO = "$BASE/api/client/user.info"
        const val START_CAPTCHA = "$BASE/api/client/start_captcha"
        const val LOGIN = "$BASE/api/client/login"
        const val RSA = "$BASE/api/client/rsa"
    }

    object LiveBili {
        const val BASE = "https://api.live.bilibili.com"
        const val ROOM_INIT = "$BASE/room/v1/Room/room_init"
        const val V2_PLAY_INFO = "$BASE/xlive/web-room/v2/index/getRoomPlayInfo"
    }

    object LiveDouyin {
        const val BASE = "https://live.douyin.com"
        const val ROOM_ENTER = "$BASE/webcast/room/web/enter/?"
    }

    const val BH3_OA_API = "https://api.v6qbb.cloud/get_bh3_bilibili_oa"
}
