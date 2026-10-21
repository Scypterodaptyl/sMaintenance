plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.2.2"
}

tasks.withType<JavaCompile> {
    options.release.set(17)
    options.encoding = "UTF-8"
}

dependencies {
    implementation(project(":core"))
    implementation("org.bstats:bstats-velocity:3.2.1")
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    compileOnly("io.github.miniplaceholders:miniplaceholders-api:2.2.3")
    compileOnly("net.luckperms:api:5.4")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
}

tasks.shadowJar {
    archiveBaseName.set("sMaintenance-Velocity")
    archiveClassifier.set("")
    dependencies {
        exclude(dependency("net.kyori:.*"))
    }
    relocate("org.bstats", "com.scypter.smaintenance.libs.bstats")
    relocate("com.google.gson", "com.scypter.smaintenance.libs.gson")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
