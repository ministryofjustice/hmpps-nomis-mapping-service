plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.15.3"
  kotlin("plugin.spring") version "1.9.22"
  kotlin("plugin.jpa") version "1.9.22"
  idea
}

dependencyCheck {
  // Suppression till can upgrade to 3.2.5
  suppressionFiles.add("spring-suppressions.xml")
}
// Temporarily pin as can't upgrade to latest gradle plugin
ext["netty.version"] = "4.1.108.Final"

configurations {
  implementation { exclude(module = "spring-boot-starter-web") }
  implementation { exclude(module = "spring-boot-starter-tomcat") }
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:0.2.2")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.0")

  implementation("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:r2dbc-postgresql:1.0.4.RELEASE")
  runtimeOnly("org.springframework.boot:spring-boot-starter-jdbc")
  runtimeOnly("org.postgresql:postgresql:42.7.3")

  implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.4.0")

  developmentOnly("org.springframework.boot:spring-boot-devtools")

  testImplementation("org.awaitility:awaitility-kotlin:4.2.1")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.5")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.5")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.21") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("io.swagger.core.v3:swagger-core-jakarta:2.2.20")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.testcontainers:postgresql:1.19.7")
  testImplementation("io.projectreactor:reactor-test")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "21"
    }
  }
}
