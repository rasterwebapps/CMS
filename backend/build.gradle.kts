plugins {
    java
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
    `maven-publish`
}

group = "com.cms"
// Allow CI to override version via -Pversion=x.y.z or APP_VERSION env var
version = System.getenv("APP_VERSION") ?: "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    // Excel import / export
    implementation("org.apache.poi:poi-ooxml:5.3.0")
    // PDF export
    implementation("com.github.librepdf:openpdf:1.3.35")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val lifecycleTestSourceSet = sourceSets.create("lifecycleTest") {
    java.srcDir("src/lifecycleTest/java")
    resources.srcDir("src/lifecycleTest/resources")
    compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
    runtimeClasspath += output + compileClasspath
}

configurations[lifecycleTestSourceSet.implementationConfigurationName]
    .extendsFrom(configurations["testImplementation"])
configurations[lifecycleTestSourceSet.runtimeOnlyConfigurationName]
    .extendsFrom(configurations["testRuntimeOnly"])

val lifecycleTest by tasks.registering(Test::class) {
    description = "Runs isolated Program/Course lifecycle tests without compiling src/test"
    group = "verification"
    testClassesDirs = lifecycleTestSourceSet.output.classesDirs
    classpath = lifecycleTestSourceSet.runtimeClasspath
    shouldRunAfter(tasks.test)
    useJUnitPlatform()
    jvmArgs("-Duser.timezone=UTC")
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Run tests with JVM in UTC so date/time assertions are timezone-independent
    jvmArgs("-Duser.timezone=UTC")
}

tasks.withType<JavaExec> {
    // Ensure the Spring Boot process itself runs in UTC
    jvmArgs("-Duser.timezone=UTC")
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test, tasks.compileJava, tasks.processResources)
    reports {
        xml.required = true
        html.required = true
    }
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude("com/cms/CmsApplication.class")
                exclude("com/cms/config/DataLoader.class")
                exclude("com/cms/config/LocalDataSeeder.class")
                exclude("com/cms/config/LocalRbacSeeder.class")
                exclude("com/cms/service/StudentImportService*.class")
                exclude("com/cms/service/ExcelTemplateService.class")
                exclude("com/cms/model/**")
                exclude("com/cms/service/*Scholarship*.class")
                exclude("com/cms/controller/*Scholarship*.class")
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                minimum = "0.90".toBigDecimal()
            }
        }
    }
    // Exclude the main application class, seed data loaders, and model entities
    // from coverage. The main class and data seeders only contain Spring Boot
    // entry/startup hooks which cannot be meaningfully unit tested.
    // Model entities are primarily boilerplate getters/setters validated
    // through integration tests.
    // Excel import/template services depend on Apache POI file I/O and are
    // better verified via integration tests; excluding from the unit-test metric.
    // DTO response classes are pure data carriers excluded from unit-test coverage.
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude("com/cms/CmsApplication.class")
                exclude("com/cms/config/DataLoader.class")
                exclude("com/cms/config/LocalDataSeeder.class")
                exclude("com/cms/config/LocalRbacSeeder.class")
                exclude("com/cms/service/StudentImportService*.class")
                exclude("com/cms/service/ExcelTemplateService.class")
                exclude("com/cms/model/**")
                exclude("com/cms/dto/**")
                exclude("com/cms/service/*Scholarship*.class")
                exclude("com/cms/controller/*Scholarship*.class")
            }
        })
    )
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

// ── Nexus Publishing ─────────────────────────────────────────────────────────
// This Gradle project uses the `maven-publish` plugin to publish artifacts to Nexus.
// "maven-publish" refers to the Maven REPOSITORY FORMAT (groupId/artifactId/version layout)
// used by Nexus — NOT the Maven build tool. Gradle is the build tool throughout.
//
// To publish manually:
//   NEXUS_URL=http://<nexus-ip>:8081 NEXUS_USER=cms-deploy NEXUS_PASS=xxx ./gradlew publish
//
// The Jenkinsfile sets NEXUS_URL, NEXUS_USER, NEXUS_PASS as environment variables.
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.named("bootJar"))
            groupId    = "com.cms"
            artifactId = "college-management-system"
            version    = project.version.toString()
        }
    }
    repositories {
        maven {
            name = "nexus"
            val isSnapshot = project.version.toString().endsWith("SNAPSHOT")
            val nexusBase  = System.getenv("NEXUS_URL") ?: "http://localhost:8081"
            url = uri(
                if (isSnapshot) "$nexusBase/repository/cms-snapshots/"
                else            "$nexusBase/repository/cms-releases/"
            )
            credentials {
                username = System.getenv("NEXUS_USER") ?: ""
                password = System.getenv("NEXUS_PASS") ?: ""
            }
        }
    }
}

