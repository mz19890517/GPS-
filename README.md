# GPS 速度计

实时显示当前位置的行进速度。

## 功能

- 实时速度显示（大字号，深色背景）
- km/h 与 MPH 单位一键切换
- 显示 GPS 精度与坐标
- GPS / 网络双定位，按可用信号自动切换

## 技术

- Kotlin + Material 3
- 原生 LocationManager，无第三方依赖
- minSdk 26，targetSdk / compileSdk 35

## 构建

仓库配置了 GitHub Actions，推送到 `main` 分支后自动构建 APK。

release 版本使用 debug 签名，可直接安装测试。
