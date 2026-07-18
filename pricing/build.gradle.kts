plugins {
    `java-library`
}

description = "Pricing bounded context: the rule engine and price calculation core (UC-05 to UC-09)."

dependencies {
    api(project(":shared-kernel"))
}
