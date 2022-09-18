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
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.spring") version "1.6.10"
    kotlin("plugin.jpa") version "1.6.10"
}

group = "com.setvect.bokslstock2"
version = "0.0.1"
java.sourceCompatibility = JavaVersion.VERSION_11

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

extra["snippetsDir"] = file("build/generated-snippets")

dependencies {
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

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
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
            from("$buildDir/libs/BokslStock2-0.0.1.jar")
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
                Pair(File("$buildDir", "/dist/lib/BokslStock2-0.0.1.jar"), project.properties["remoteDir"].toString() + "/lib"),
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