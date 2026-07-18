plugins {
    java
    id("org.springframework.boot")
}

description = "Startup module: main class, configuration and wiring for all bounded contexts."

dependencies {
    implementation(project(":shared-kernel"))
    implementation(project(":catalog"))
    implementation(project(":pricing"))
    implementation(project(":promotions"))
    implementation(project(":audit"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-json")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}
