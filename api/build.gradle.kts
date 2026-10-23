

// Build script for the Runway REST API (IIE, 2026).
// Build script for the Runway REST API (PROG7314 POE Part 2, SCRUM-78).
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(ktorLibs.plugins.ktor)
}

// Must match the version of the ktorLibs catalogue in settings.gradle.kts.
val ktorVersion = "3.5.2"

group = "com.example"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

ktor {
    openApi {
        enabled = true
    }
}

dependencies {
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)

    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Turns thrown exceptions into a single JSON error shape (Ktor, 2026).
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    // Reads the Authorization: Bearer header on protected routes (Ktor, 2026).
    implementation("io.ktor:ktor-server-auth:$ktorVersion")

    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")

    // Serves interactive API documentation from the OpenAPI file (Ktor, 2026).
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")

    implementation(libs.firebase.admin)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Ktor, 2026. Ktor server documentation. [online] Available at: <https://ktor.io/docs/server-create-and-configure.html> [Accessed 11 September 2026].
*/
