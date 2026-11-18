plugins {
    id("com.android.library")
    id("maven-publish") 
}

android {
    namespace = "mr.x.zjx"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.11.0")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("com.github.zcweng:switch-button:0.0.3")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "com.github.youhuaixu37.boops" 
                artifactId = "Mouren-UI"                 
                version = "3.1.0"
                from(components["release"])
            }
        }
    }
}