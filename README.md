# MHY_Scanner_Mobile

米哈游(原神/星穹铁道/绝区零/崩坏3)直播抢码工具的 Android 移植版。

本项目为 [MHY_Scanner](https://github.com/fplj-fplj/MHY_Scanner) 的 Android 重制版,使用 Kotlin + Jetpack Compose 编写,遵循上游 **GPL-3.0** 协议。

## 功能

- 直播抢码:解析 B 站 / 抖音直播间画面中的登录二维码,自动"扫码"并(可选)自动"确认登录"
- 屏幕扫码:通过系统录屏权限实时识别屏幕上的登录二维码
- 多账号管理:保存多个账号并切换默认账号
- 添加账号方式:
  - 手机米游社扫码(生成二维码,用米游社 App 扫描并确认)
  - Cookie 导入(自动用 login_ticket 换 stoken,或直接解析 stoken)
  - 手机号 + 短信验证码(需要时可弹极验滑块)
- 支持平台:原神、星穹铁道、绝区零(官服)以及崩坏3(官服 / B服)

## 架构

| 模块 | 说明 |
| --- | --- |
| `core/` | API 常量、DS 签名、RSA/MD5/HMAC、Cookie 解析、米哈游接口(MhyApi) |
| `scanner/` | ZXing 二维码识别、帧抽象 |
| `live/` | 直播流地址解析(ExoPlayer 拉流) |
| `screen/` | MediaProjection 屏幕抓帧 |
| `engine/` | 抢码引擎(识码 → 扫码 → 确认) |
| `data/` | DataStore 多账号配置存储 |
| `ui/` | Compose 界面(直播/屏幕/账号/设置) |

## 构建

```bash
./gradlew assembleDebug
```

产物位于 `app/build/outputs/apk/debug/`。

GitHub Actions 会在每次 push 到 `main` 时自动编译并上传 debug APK 到构建产物(Artifacts)。

## 免责声明

- 本项目仅供学习与研究,请勿用于任何商业用途。
- 使用本工具登录米游社即代表同意米游社用户协议,因使用本工具导致的账号风险由使用者自行承担。
- 本工具未与米哈游官方有任何合作或授权关系。

## 许可证

[GPL-3.0](LICENSE) — 移植自 [fplj-fplj/MHY_Scanner](https://github.com/fplj-fplj/MHY_Scanner)(GPL-3.0)。
