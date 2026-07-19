plugins {
    `java-library`
}

description = "Shared kernel: Money, DomainEvent, typed identifiers and the EventPublisher port. " +
    "Deliberately minimal and framework-agnostic — no Spring dependency."

dependencies {
    // Annotation-only, no runtime/Spring dependency — keeps Money's isZero()/isNegative() query
    // methods from being serialized as extra JSON properties (they aren't record components).
    implementation("com.fasterxml.jackson.core:jackson-annotations")
}
