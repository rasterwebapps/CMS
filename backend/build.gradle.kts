plugins {
    java
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
}

group = "com.cms"
version = "0.0.1-SNAPSHOT"

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

    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude("com/cms/CmsApplication.class")
                exclude("com/cms/config/DataLoader.class")
                exclude("com/cms/model/**")
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                minimum = "0.94".toBigDecimal()
            }
        }
    }
    // Exclude the main application class, seed data loader, and model entities
    // from coverage. The main class and DataLoader only contain Spring Boot
    // entry/startup hooks which cannot be meaningfully unit tested.
    // Model entities are primarily boilerplate getters/setters validated
    // through integration tests.
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude("com/cms/CmsApplication.class")
                exclude("com/cms/config/DataLoader.class")
                exclude("com/cms/model/**")
            }
        })
    )
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
