allprojects {
    group = "com.scypter.smaintenance"
    version = property("version") as String
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.extendedclip.com/releases/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
}
