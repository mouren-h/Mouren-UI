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
    implementation("androidx.core:core:1.19.1") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    }
    implementation("androidx.annotation:annotation:1.11.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "com.github.youhuaixu37.boops" 
                artifactId = "Mouren-UI"                 
                version = "3.3.0"
                from(components["release"])
            }
        }
    }
}