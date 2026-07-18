plugins {
    `java-library`
}

description = "Audit bounded context: read-side traceability over Catalog, Pricing and " +
    "Promotions domain events (UC-14, UC-15)."

dependencies {
    api(project(":shared-kernel"))
}
