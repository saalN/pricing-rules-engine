plugins {
    `java-library`
}

description = "Pricing bounded context: the rule engine and price calculation core (UC-05 to UC-09)."

dependencies {
    api(project(":shared-kernel"))
    // Depends only on the ports Catalog publishes (GetProductUseCase,
    // FindCheapestActiveBasePriceUseCase), never its domain/infrastructure internals.
    implementation(project(":catalog"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.security:spring-security-core")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
