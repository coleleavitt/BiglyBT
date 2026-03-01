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
    compileOnly("org.eclipse.swt:$swtArtifact:$swtVersion")
}

group = "com.biglybt"
version = "3.4.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

val addOpensArgs = listOf(
    "--add-opens", "java.base/java.net=ALL-UNNAMED",
    "--add-opens", "java.base/sun.net.www.protocol.http=ALL-UNNAMED",
    "--add-opens", "java.base/sun.net.www.protocol.https=ALL-UNNAMED"
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
    options.encoding = "8859_1"
    options.compilerArgs.addAll(listOf("-Xlint:-options", "-Xlint:-deprecation"))
}

tasks.withType<Javadoc> {
    options.encoding = "8859_1"
}

sourceSets {
    main {
        java {
            srcDirs("src")
        }
        resources {
            srcDirs("resources")
        }
    }
    test {
        java {
            srcDirs("test")
        }
    }
}
