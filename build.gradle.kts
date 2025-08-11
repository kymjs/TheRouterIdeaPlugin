plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.intellij") version "1.15.0"
}

group = "cn.therouter"
version = "1.3.5"

repositories {
    maven("https://maven.therouter.cn:8443/repository/maven-public/")
    mavenCentral()
}

intellij {
    version.set("2024.1.4")
    type.set("IC") // Target IDE Platform
    plugins.set(listOf("java"))
}
dependencies {
    implementation("com.github.adamko-dev:json5-kotlin:2.1.0")
    implementation("com.squareup.okhttp3:okhttp:3.14.2")
}
tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("231.*")
        untilBuild.set("264.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
