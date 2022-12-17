plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {


    implementation("io.vertx:vertx-core:4.3.5")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

allprojects{
    repositories {
        mavenCentral()
    }
    apply {
        plugin("java")
    }
    dependencies{
        implementation("io.vertx:vertx-core:4.3.5")
        implementation("io.vertx:vertx-hazelcast:4.3.5")
        implementation("com.fasterxml.jackson.core:jackson-core:2.14.1")
        compileOnly("org.projectlombok:lombok:1.18.24")
        annotationProcessor("org.projectlombok:lombok:1.18.24")
        implementation("org.jetbrains:annotations:23.0.0")
        implementation("com.fasterxml.jackson.core:jackson-databind:2.14.1")
        implementation("commons-io:commons-io:2.11.0")

    }
}
tasks.getByName<Test>("test") {
    useJUnitPlatform()
}