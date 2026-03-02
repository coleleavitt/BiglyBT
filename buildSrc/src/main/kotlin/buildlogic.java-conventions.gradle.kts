plugins {
    `java-library`
    `maven-publish`
}

val swtVersion = "4.9"

repositories {
    mavenLocal()
    maven {
        url = uri("https://lislei.github.io/maven-eclipse.github.io/maven")
    }
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

val os = System.getProperty("os.name").lowercase()
val swtArtifact = when {
    os.contains("mac") -> "org.eclipse.swt.cocoa.macosx.x86_64"
    os.contains("win") -> "org.eclipse.swt.win32.win32.x86_64"
    else -> "org.eclipse.swt.gtk.linux.x86_64"
}

dependencies {
    implementation("org.eclipse.swt:$swtArtifact:$swtVersion")
}

group = "com.biglybt"
version = "3.4.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

val addOpensArgs = listOf(
    "--add-opens", "java.base/java.net=ALL-UNNAMED",
    "--add-opens", "java.base/sun.net.www.protocol.http=ALL-UNNAMED",
    "--add-opens", "java.base/sun.net.www.protocol.https=ALL-UNNAMED",
    "--add-opens", "java.base/sun.net.www=ALL-UNNAMED",
    "--add-exports", "java.base/sun.net.www=ALL-UNNAMED"
)

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs(addOpensArgs)
}

tasks.withType<JavaExec> {
    jvmArgs(addOpensArgs)
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:-options", "-Xlint:-deprecation"))
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

sourceSets {
    main {
        java {
            srcDirs("src")
        }
        resources {
            srcDirs("resources", "src")
            include("**/*.properties", "**/*.png", "**/*.gif", "**/*.jpg", "**/*.html", "**/*.css", "**/*.xml", "**/*.txt", "META-INF/**")
        }
    }
    test {
        java {
            srcDirs("test")
        }
    }
}
