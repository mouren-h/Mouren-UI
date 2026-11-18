// 根目录的 build.gradle.kts
plugins {
    // 声明 Android Library 插件和版本号，apply false 表示这里不直接运行
    id("com.android.library") version "8.1.2" apply false
}

android {
    // 你的包名（从代码里找到的那个）
    namespace = "mr.x.zjx" 
    
    // 如果你之前有写 compileSdk 数字，请改成你原本的数字（比如 33 或 34）
    compileSdk = 34 

    defaultConfig {
        // 你的最低支持安卓版本（如果你原本有写 minSdk，保留你原本的数字）
        minSdk = 24 
        // 开发者专门强调过：库文件绝对不能有 versionName 和 versionCode，这里千万不要写。
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    
    // 开发者要求的发布配置
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
    
    // 编译使用的 Java 版本（通常安卓库用 17 或 11）
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                // ⚠️ 根据 JitPack 规则，你的用户名带中划线 '-', 必须改成点 '.'
                groupId = "com.github.youhuaixu37.boops" 
                
                // 你的 GitHub 仓库名
                artifactId = "Mouren-UI"                 
                
                // 版本号
                version = "3.0.0"                        
                
                // 绑定 release 组件
                from(components["release"])
            }
        }
    }
}