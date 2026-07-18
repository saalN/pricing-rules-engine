plugins {
    `java-library`
}

description = "Promotions bounded context: campaigns, coupons and eligibility (UC-10 to UC-13)."

dependencies {
    api(project(":shared-kernel"))
    // Depends only on the ports published by Pricing (e.g. the conflict-resolution extension
    // point), never on its domain/infrastructure internals.
    implementation(project(":pricing"))
}
