plugins {
    `java-library`
}

description = "Promotions bounded context: campaigns, coupons and eligibility (UC-10 to UC-13)."

dependencies {
    api(project(":shared-kernel"))
    // Depends only on the ports published by Pricing (e.g. the conflict-resolution extension
    // point) and Catalog (the cheapest-active-base-price query), never on their
    // domain/infrastructure internals.
    implementation(project(":pricing"))
    implementation(project(":catalog"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.security:spring-security-core")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.flywaydb:flyway-core")
    testImplementation("org.flywaydb:flyway-database-postgresql")
    testRuntimeOnly("org.postgresql:postgresql")
}
