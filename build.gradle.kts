plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.5.1-beta"
  kotlin("plugin.spring") version "1.7.10"
  kotlin("plugin.jpa") version "1.7.10"
  idea
}

dependencyCheck {
  suppressionFiles.add("reactive-suppressions.xml")
  // Please remove the below suppressions once it has been suppressed in the DependencyCheck plugin (see this issue: https://github.com/jeremylong/DependencyCheck/issues/4616)
  suppressionFiles.add("postgres-suppressions.xml")
}

configurations {
  implementation { exclude(module = "spring-boot-starter-web") }
  implementation { exclude(module = "spring-boot-starter-tomcat") }
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.6.4")

  implementation("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:r2dbc-postgresql:0.9.2.RELEASE")
  runtimeOnly("org.springframework.boot:spring-boot-starter-jdbc")
  runtimeOnly("org.postgresql:postgresql:42.5.0")
  implementation("io.opentelemetry:opentelemetry-api:1.18.0")

  implementation("org.springdoc:springdoc-openapi-webflux-ui:1.6.11")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.6.11")
  implementation("org.springdoc:springdoc-openapi-security:1.6.11")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.4")

  developmentOnly("org.springframework.boot:spring-boot-devtools")

  testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("org.mockito:mockito-inline:4.8.0")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.2")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.testcontainers:postgresql:1.17.3")
  testImplementation("io.projectreactor:reactor-test")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(18))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "18"
    }
  }
}
