import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.6.2"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("org.asciidoctor.convert") version "1.5.8"
    id("org.hidetake.ssh") version "2.10.1"
    war
    idea
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.spring") version "1.6.10"
    kotlin("plugin.jpa") version "1.6.10"
    kotlin("kapt") version "1.3.61"
}

group = "com.setvect.bokslstock2"
version = "0.0.2"
java.sourceCompatibility = JavaVersion.VERSION_17

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
    runtimeOnly { // runtimeOnly 구성
        isCanBeResolved = true
    }
}

apply(plugin = "kotlin-kapt")

repositories {
    mavenCentral()
}

extra["snippetsDir"] = file("build/generated-snippets")

dependencies {
    val querydslVersion = "5.0.0"

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.apache.httpcomponents:httpclient")
    implementation("com.google.code.gson:gson")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.apache.poi:poi:5.2.2")
    implementation("org.apache.poi:poi-ooxml:5.2.2")
    implementation("org.apache.commons:commons-csv:1.9.0")
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("org.modelmapper:modelmapper:2.4.0")
    implementation("com.querydsl:querydsl-jpa:$querydslVersion")
//    implementation("com.google.api-client:google-api-client:1.20.0")
    implementation("com.google.api-client:google-api-client-jackson2:1.20.0")
//    implementation("com.google.apis:google-api-services-youtube:v3-rev222-1.25.0")
    implementation ("org.apache.commons:commons-compress")

    implementation ("com.google.api-client:google-api-client:1.31.5")
    implementation ("com.google.apis:google-api-services-youtube:v3-rev222-1.25.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")

    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    kapt("com.querydsl:querydsl-apt:$querydslVersion:jpa")
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    compileOnly("org.projectlombok:lombok")
    runtimeOnly("com.h2database:h2")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    providedRuntime("org.springframework.boot:spring-boot-starter-tomcat")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
}

kotlin.sourceSets.main {
    println("buildDir: $buildDir")
    setBuildDir("$buildDir/generated/source/kapt/main")
}

sourceSets {
    create("testDependency") {
        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        runtimeClasspath += configurations.getByName("runtimeOnly")

        resources.srcDir(file("src/testDependency/resources"))
    }
}

task<Test>("testDependency") {
    description = "배포 전 검증 테스트 아님. 환경에 의존된 테스트. 항상 같은 결과를 보장하지 않음. 배포시 테스트로 사용하면 안됨"
    group = "verification"

    testClassesDirs = sourceSets["testDependency"].output.classesDirs
    classpath = sourceSets["testDependency"].runtimeClasspath

    shouldRunAfter("test")
}

val testDependencyImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get(), configurations.testImplementation.get())
}

idea.module {
    testSourceDirs = testSourceDirs + project.sourceSets.getByName("testDependency").allSource.srcDirs
    testResourceDirs = testResourceDirs + project.sourceSets.getByName("testDependency").resources.srcDirs
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val snippetsDir by extra { file("build/generated-snippets") }

tasks.test {
    outputs.dir(snippetsDir)
}

tasks.asciidoctor {
    inputs.dir(snippetsDir)
    dependsOn(tasks.test) // added tasks. prefix
}

tasks.register("makeInstallFile") {
    group = "build"
    dependsOn(tasks.bootJar)

    doLast {
        delete("$buildDir/dist")

        copy {
            from("$buildDir/libs/BokslStock2-0.0.2.jar")
            into("$buildDir/dist/lib")
        }
        copy {
            from("./script")
            include("*")
            into("$buildDir/dist/bin")
        }

        copy {
            from("./src/main/resources/application.yml")
            into("$buildDir/dist/conf")
            rename("application.yml", "BokslStock2.yml")
        }
        copy {
            from("./src/main/resources/logback-spring.xml")
            into("$buildDir/dist/conf")
        }
    }
    println("########################################################################")
    println("인스톨 파일이 있는 디렉토리: $buildDir/dist")
    println("########################################################################")
}


tasks.register("deployRemote") {
    group = "build"
    dependsOn("makeInstallFile")


    doLast {
        ssh.run {
            val jsch = JSch()
            jsch.addIdentity(project.properties["authFile"].toString())

            val session = jsch.getSession(
                project.properties["remoteUser"].toString(),
                project.properties["remoteHost"].toString(),
                project.properties["remotePort"].toString().toInt()
            )
            session.setConfig("StrictHostKeyChecking", "no")
            session.connect()
            val sftp = session.openChannel("sftp") as ChannelSftp
            sftp.connect()

            val uploadFiles = listOf(
                Pair(File("$buildDir", "/dist/lib/BokslStock2-0.0.2.jar"), project.properties["remoteDir"].toString() + "/lib"),
//                Pair(File("$buildDir", "/dist/bin/BokslStock2.sh"), project.properties["remoteDir"].toString() + "/bin"),
            )

            uploadFiles.forEach {
                sftp.put(it.first.absolutePath, it.second)
                println("upload: ${it.second}/${it.first.name}")
            }
            sftp.disconnect()
            val exec = session.openChannel("exec") as ChannelExec
            exec.setCommand(project.properties["restartCommand"].toString())
            exec.outputStream = System.out
            exec.setErrStream(System.err)
            exec.connect()
            while (exec.isConnected) {
                Thread.sleep(1000)
            }

            exec.disconnect()

            session.disconnect()
        }
    }
}
