package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
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
        Server().url("https://nomis-visits-mapping.hmpps.service.justice.gov.uk").description("Prod"),
        Server().url("https://nomis-visits-mapping-preprod.hmpps.service.justice.gov.uk").description("PreProd"),
        Server().url("https://nomis-visits-mapping-dev.hmpps.service.justice.gov.uk").description("Development"),
        Server().url("http://localhost:8080").description("Local"),
      )
    )
    .info(
      Info().title("NOMIS Visits Mapping Service")
        .version(version)
        .description("Manages mapping information between VSIP and NOMIS")
        .contact(Contact().name("HMPPS Digital Studio").email("feedback@digital.justice.gov.uk"))
    )
}
