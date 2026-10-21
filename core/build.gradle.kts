plugins {
    `java-library`
}

tasks.withType<JavaCompile> {
    options.release.set(8)
    options.encoding = "UTF-8"
}

dependencies {
    api("com.google.code.gson:gson:2.11.0")
    compileOnly("net.luckperms:api:5.4")
    api("net.kyori:adventure-api:4.17.0")
    api("net.kyori:adventure-text-minimessage:4.17.0")
    testImplementation("net.kyori:adventure-text-serializer-plain:4.17.0")
    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
