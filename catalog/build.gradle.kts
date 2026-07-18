plugins {
    `java-library`
}

description = "Catalog bounded context: product and category management (UC-01 to UC-04)."

dependencies {
    api(project(":shared-kernel"))
}
