import java.util.Properties

plugins {
    java
    id("xyz.jpenilla.run-paper") version "2.3.0"
}

group = "com.xiaoyiluck.meowtools"
version = "2.2.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("org.jetbrains:annotations:26.0.2-1")
    testImplementation("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    testImplementation("me.clip:placeholderapi:2.11.5")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testImplementation("org.mockito:mockito-core:5.20.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}

// Compile against each published Minecraft release, then run the SAME Java 21
// plugin classes against that API/dependency graph to detect linkage failures.
val paperManifest = Properties().apply {
    rootProject.file("gradle/paper-versions.properties").inputStream().use { load(it) }
}
val paperVersions = paperManifest.stringPropertyNames().sorted().associate {
    it.replace('.', '_') to paperManifest.getProperty(it)
}
val compatibilityChecks = paperVersions.map { (suffix, apiVersion) ->
    val api = configurations.create("paper$suffix") {
        isCanBeConsumed = false
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
    }
    dependencies.add(api.name, "io.papermc.paper:paper-api:$apiVersion")
    dependencies.add(api.name, "me.clip:placeholderapi:2.11.5")
    dependencies.add(api.name, "org.jetbrains:annotations:26.0.2-1")
    val compile = tasks.register<JavaCompile>("compilePaper$suffix") {
        source = sourceSets.main.get().allJava
        classpath = api
        destinationDirectory.set(layout.buildDirectory.dir("compatibility/$suffix/classes"))
        javaCompiler.set(javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(25)) })
        options.encoding = "UTF-8"
        options.release.set(21)
    }
    val runtime = configurations.create("paperTest$suffix") {
        isCanBeConsumed = false
        extendsFrom(configurations.testImplementation.get(), configurations.testRuntimeOnly.get())
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
        resolutionStrategy.force("io.papermc.paper:paper-api:$apiVersion")
    }
    tasks.register<Test>("testPaper$suffix") {
        dependsOn(compile, tasks.testClasses)
        testClassesDirs = sourceSets.test.get().output.classesDirs
        classpath = sourceSets.test.get().output + sourceSets.main.get().output + runtime
        javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
    }
}
tasks.register("checkCompatibility") { dependsOn(compatibilityChecks) }

// Separate instrumentation plugin for isolated real-server acceptance tests.
// Never included in the distributable MeowTools JAR.
val liveTest by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
}
tasks.named<JavaCompile>(liveTest.compileJavaTaskName) {
    options.encoding = "UTF-8"
    options.release.set(21)
}
tasks.register<Jar>("liveProbeJar") {
    dependsOn(liveTest.classesTaskName)
    archiveFileName.set("MeowToolsLiveProbe.jar")
    destinationDirectory.set(layout.buildDirectory.dir("live-tools"))
    from(liveTest.output)
}

tasks.runServer { minecraftVersion("1.21.10") }

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
    javadoc {
        options.encoding = "UTF-8"
    }
    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
