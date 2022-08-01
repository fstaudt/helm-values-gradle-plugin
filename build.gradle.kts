import org.gradle.api.JavaVersion.VERSION_1_8

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "0.21.0"
    `maven-publish`
}

repositories {
    mavenCentral()
}
val pluginVersion = "$version"
val pluginName = "helm-values"
gradlePlugin {
    plugins {
        register(pluginName) {
            id = "io.github.fstaudt.$pluginName"
            implementationClass = "$group.HelmValuesPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/fstaudt/helm-values-gradle-plugin"
    vcsUrl = "https://github.com/fstaudt/helm-values-gradle-plugin"
    description = "Generate JSON schemas to validate values of complex Helm charts with Gradle!"
    (plugins) {
        pluginName {
            displayName = "Helm values assistant"
            tags = listOf("helm", "chart", "kubernetes", "json", "schema")
            version = pluginVersion
        }
    }
}

dependencies {
    compileOnly(gradleKotlinDsl())
    api(kotlin("gradle-plugin"))
    api("org.apache.httpcomponents.client5:httpclient5:5.1.3")
    api("org.apache.commons:commons-compress:1.21")
    api("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.3")
    api("com.github.java-json-tools:json-patch:1.13")
    testImplementation(gradleTestKit())
    testImplementation("org.assertj:assertj-core:3.23.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.33.2")
    testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.35.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.test {
    useJUnitPlatform()
}

java {
    targetCompatibility = VERSION_1_8
}