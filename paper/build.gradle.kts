import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.2.2"
}

tasks.withType<JavaCompile> {
    options.release.set(8)
    options.encoding = "UTF-8"
}

configurations.matching { it.name in listOf("compileClasspath", "testCompileClasspath", "testRuntimeClasspath") }.configureEach {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
}

dependencies {
    implementation(project(":core"))
    implementation("net.kyori:adventure-platform-bukkit:4.3.4")
    implementation("org.bstats:bstats-bukkit:3.0.2")
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("net.luckperms:api:5.4")
    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.shadowJar {
    archiveBaseName.set("sMaintenance-Paper")
    archiveClassifier.set("")
    relocate("net.kyori", "com.scypter.smaintenance.libs.kyori")
    relocate("org.bstats", "com.scypter.smaintenance.libs.bstats")
    relocate("com.google.gson", "com.scypter.smaintenance.libs.gson")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
