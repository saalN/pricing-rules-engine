import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension

plugins {
    java
    id("org.springframework.boot") version "3.5.3" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.priceforge"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile> {
        // Spring MVC resolves @PathVariable/@RequestParam names via reflection when no explicit
        // name is given on the annotation; without -parameters that lookup fails at request time.
        options.compilerArgs.add("-parameters")
    }

    configure<DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.3")
            mavenBom("org.testcontainers:testcontainers-bom:1.21.3")
        }
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testImplementation"("org.assertj:assertj-core")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        // Testcontainers 1.21.x falls back to Docker API version 1.32 when it cannot negotiate
        // one, which recent Docker Engine releases (minimum supported API 1.40) reject outright.
        // Pin a modern-but-conservative version so Testcontainers-based tests work regardless of
        // the negotiation fallback bug.
        systemProperty("api.version", "1.44")
    }
}
