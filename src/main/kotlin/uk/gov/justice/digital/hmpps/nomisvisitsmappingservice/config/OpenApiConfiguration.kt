package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val version: String = buildProperties.version

  @Bean
  fun customOpenAPI(): OpenAPI = OpenAPI()
    .servers(
      listOf(
        Server().url("https://nomis-sync-prisoner-mapping.hmpps.service.justice.gov.uk").description("Prod"),
        Server().url("https://nomis-sync-prisoner-mapping-preprod.hmpps.service.justice.gov.uk").description("PreProd"),
        Server().url("https://nomis-sync-prisoner-mapping-dev.hmpps.service.justice.gov.uk").description("Development"),
        Server().url("http://localhost:8080").description("Local"),
      ),
    ).tags(
      listOf(
        Tag().name("NOMIS / DPS Mapping lookup")
          .description("APIs designed to be used by DPS services that need to map NOMIS ids stored in a DPS service to a sibling DPS domain service"),
      ),
    )
    .info(
      Info().title("NOMIS Mapping Service")
        .version(version)
        .description(
          // language=html
          """
          <div>
          <h2>Manages mapping information between DPS services and NOMIS</h2>
          <p>APIs tagged by <b>NOMIS to DPS Mapping lookup</b> are designed to be used by other DPS services that need to transition data held in that service that was originally sourced from NOMIS by now needs to sourced from other DPS services.</p>
          <p>Each domain requires their own role to access these APIs for instance <b>NOMIS_DPS_MAPPING__LOCATIONS__R</b> for location mapping lookups</p>
          <p>All other APIs should be considered private to the NOMIS Migration and Synchronisation suite of services.</p>
          <div>
          """.trimIndent(),
        )
        .contact(Contact().name("HMPPS Digital Studio").email("feedback@digital.justice.gov.uk")),
    )
    .components(
      Components().addSecuritySchemes(
        "bearer-jwt",
        SecurityScheme()
          .type(SecurityScheme.Type.HTTP)
          .scheme("bearer")
          .bearerFormat("JWT")
          .`in`(SecurityScheme.In.HEADER)
          .name("Authorization"),
      ),
    )
    .addSecurityItem(SecurityRequirement().addList("bearer-jwt", listOf("read", "write")))

  @Bean
  fun openAPICustomiser(): OpenApiCustomizer = OpenApiCustomizer {
    it.components.schemas.forEach { (_, schema: Schema<*>) ->
      val properties = schema.properties ?: mutableMapOf()
      for (propertyName in properties.keys) {
        val propertySchema = properties[propertyName]!!
        if (propertySchema is DateTimeSchema) {
          properties.replace(
            propertyName,
            StringSchema()
              .example("2021-07-05T10:35:17")
              .pattern("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$")
              .description(propertySchema.description)
              .required(propertySchema.required),
          )
        }
      }
    }
  }
}
