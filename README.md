# GPS 速度计

实时显示当前位置的行进速度，并集成高德地图导航 SDK，支持在导航过程中查看前方红绿灯倒计时。

## 功能

- 实时速度显示（大字号，深色背景）
- km/h 与 MPH 单位一键切换
- 显示 GPS 精度与坐标
- GPS / 网络双定位，按可用信号自动切换
- 🚦 红绿灯导航：输入目的地后进入高德导航，途中实时显示前方剩余红绿灯数量（导航视图内置红绿灯灯态与倒计时读秒）

## 技术

- Kotlin + Material 3
- 原生 LocationManager 实现速度显示
- 高德导航 SDK（`com.amap.api:navi-3dmap`）实现红绿灯导航
- minSdk 26，targetSdk / compileSdk 35
- 支持 armeabi-v7a、arm64-v8a

---

## ⚠️ 重要：需要配置高德 Key 与授权

**红绿灯导航功能依赖高德地图开放平台的授权，必须先申请 Key 才能正常使用。**

### 一、申请高德开发者 Key

1. 访问 [高德开放平台](https://lbs.amap.com/)，注册并登录账号。
2. 完成**个人开发者认证**（导航 SDK 需要实名认证）。
3. 进入控制台 → 「应用管理」→「我的应用」→「创建新应用」。
4. 在应用下「添加 Key」：
   - 服务平台选择 **「Android 平台」**
   - 安全码 SHA1 填写你 APK 签名证书的 SHA1（Android Studio 的 `Signing Report` 可查看）
   - 发布版安全码填签名用的 keystore 对应的 SHA1，调试版填 debug keystore 的 SHA1
5. 提交后会生成一串 Key（形如 `xxxxx...`），即为高德 Key。

### 二、把 Key 配置进项目

把申请到的 Key 写到仓库根目录的 `local.properties`（不会提交到 GitHub）：

```
AMAP_KEY=你的高德Key
```

如果没有配置，`app/build.gradle.kts` 会使用占位符 `YOUR_AMAP_KEY`，此时导航会提示需要配置 Key。

### 三、关于导航 SDK 的商务授权

高德的**导航 SDK**（不同于地图/定位 SDK）属于商业授权产品，免费支撑期有限，正式商用需提交商务合作工单。个人测试可在申请 Key 后按官方指引获取试用权限。红绿灯倒计时的**精确读秒**能力也属于需要开通的增值能力；基础的红绿灯图标与灯态会随导航地图显示。

> 若在导航时提示「授权失败 / key 无效」，请确认：Key 已配置、SHA1 正确、已完成开发者认证，并联系高德开放平台开通导航授权。

---

## 构建

仓库配置了 GitHub Actions，推送到 `main` 分支后自动构建 APK。

release 版本使用 debug 签名，可直接安装测试。
