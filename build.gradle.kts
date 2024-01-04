plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.13.0"
  kotlin("plugin.spring") version "1.9.22"
  kotlin("plugin.jpa") version "1.9.22"
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

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")

  implementation("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:r2dbc-postgresql:1.0.3.RELEASE")
  runtimeOnly("org.springframework.boot:spring-boot-starter-jdbc")
  runtimeOnly("org.postgresql:postgresql:42.7.1")

  implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.3.0")

  developmentOnly("org.springframework.boot:spring-boot-devtools")

  testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.3")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.3")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.19") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("io.swagger.core.v3:swagger-core-jakarta:2.2.20")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.testcontainers:postgresql:1.19.3")
  testImplementation("io.projectreactor:reactor-test")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
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
