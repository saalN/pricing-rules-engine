plugins {
    `java-library`
}

description = "Catalog bounded context: product and category management (UC-01 to UC-04)."

dependencies {
    api(project(":shared-kernel"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.security:spring-security-core")
}
