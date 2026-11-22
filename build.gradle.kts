plugins {
    id("java")
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
}
group = "org.iHarwood"
version = "1.0-SNAPSHOT"
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}
repositories {
    mavenCentral()
}
dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:2.1.0")
    }
}
dependencies {
    // Spring Boot 4 preferred MVC starter (spring-boot-starter-web is deprecated)
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // MCP server (Streamable HTTP at /mcp) so AI agents can query the astronomical data
    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
springBoot {
    mainClass.set("org.iHarwood.MoonPhaseApplication")
}
tasks.jar {
    enabled = false
}
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:deprecation")
    options.compilerArgs.add("-parameters")
}
tasks.test {
    useJUnitPlatform()
}
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs = listOf(
        "--enable-native-access=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "-XX:+EnableDynamicAgentLoading"
    )
}
