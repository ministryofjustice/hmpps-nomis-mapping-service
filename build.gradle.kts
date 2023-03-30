plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.1.3-beta-2"
  kotlin("plugin.spring") version "1.8.10"
  kotlin("plugin.jpa") version "1.8.10"
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

// Temporarily kept at 0.9.2 as get class java.lang.Long cannot be cast to class java.lang.Integer when upgrading to 1.0.0.RELEASE
val r2dbcPostgresVersion by extra("0.9.2.RELEASE")

repositories {
  maven { url = uri("https://repo.spring.io/milestone") }
  mavenCentral()
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
  runtimeOnly("org.postgresql:r2dbc-postgresql:$r2dbcPostgresVersion")
  runtimeOnly("org.springframework.boot:spring-boot-starter-jdbc")
  runtimeOnly("org.postgresql:postgresql:42.6.0")
  implementation("io.opentelemetry:opentelemetry-api:1.24.0")

  implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.0.4")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

  developmentOnly("org.springframework.boot:spring-boot-devtools")

  testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.11.5")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.11.5")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("javax.xml.bind:jaxb-api:2.3.1")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.13")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.testcontainers:postgresql:1.17.6")
  testImplementation("io.projectreactor:reactor-test")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(19))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "19"
    }
  }
}
