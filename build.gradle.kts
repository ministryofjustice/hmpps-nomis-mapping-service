plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.0.3"
  kotlin("plugin.spring") version "1.6.10"
  kotlin("plugin.jpa") version "1.6.10"
  idea
}

dependencyCheck {
  suppressionFiles.add("reactive-suppressions.xml")
}

configurations {
  implementation { exclude(module = "applicationinsights-spring-boot-starter") }
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

  implementation("com.microsoft.azure:applicationinsights-core:2.6.4")

  implementation("org.flywaydb:flyway-core:8.4.4")
  implementation("com.vladmihalcea:hibernate-types-52:2.14.0")
  runtimeOnly("io.r2dbc:r2dbc-postgresql")
  runtimeOnly("org.springframework.boot:spring-boot-starter-jdbc")
  runtimeOnly("org.postgresql:postgresql:42.3.2")

  implementation("org.springdoc:springdoc-openapi-webflux-ui:1.6.6")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.6.6")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")

  implementation("com.zaxxer:HikariCP:5.0.1")

  developmentOnly("org.springframework.boot:spring-boot-devtools")

  testImplementation("org.awaitility:awaitility-kotlin:4.1.1")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("org.mockito:mockito-inline:4.3.1")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.0.30")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.testcontainers:postgresql:1.16.3")
  testImplementation("io.projectreactor:reactor-test")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "17"
    }
  }
}
