# 裕语言 (v5.0) - 安卓 UI 组件库

这是一个基于 Android 的开源 UI 组件库，旨在为开发者提供轻量、易用且美观的界面组件。本项目包含了丰富的 UI 控件和实用的工具类。

## ✨ 特性亮点

本项目目前包含 **6大类**，共计 **27个组件**，并且还在持续更新中：

*   🎨 **基础样式**：统一的颜色管理、状态栏设置、按钮、进度条、编辑框、选择框。
*   ✒️ **文本样式**：支持渐变文字、描边文字、跑马灯效果以及打字机特效。
*   💬 **交互反馈**：Toast 提示、各种样式的弹窗 (Dialog)、Snackbar、确认对话框以及加载中提示。
*   🪟 **弹出层**：底部弹窗 (BottomSheet)、悬浮窗、ActionSheet 以及遮罩浮层。
*   🧭 **导航组件**：小程序风格导航栏、步骤条。
*   📥 **渲染组件**：支持控件下载渲染。
*   🎛️ **选择器组件**：日期选择器 (DatePicker)、时间选择器 (TimePicker)、性别选择器 (GenderPicker)。

## 📦 依赖项

本库提供了一系列强大的 `Util` 工具类（如 `ActionSheetUtil`, `DialogUtil`, `ToastUtil`, `DatePickerUtil` 等），方便开发者快速调用。

## 🚀 如何引入

**第一步：在项目的 `settings.gradle.kts` 中添加 JitPack 仓库**

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

dependencies {
	        implementation("com.github.youhuaixu37-boop:Mouren-UI:Tag")
	}