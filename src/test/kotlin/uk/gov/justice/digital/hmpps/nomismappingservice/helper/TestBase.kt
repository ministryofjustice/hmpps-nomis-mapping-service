package uk.gov.justice.digital.hmpps.nomismappingservice.helper

import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import uk.gov.justice.digital.hmpps.nomismappingservice.config.PostgresContainer

@ActiveProfiles("test")
abstract class TestBase {

  companion object {
    private val pgContainer = PostgresContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      pgContainer?.run {
        registry.add("spring.datasource.url", pgContainer::getJdbcUrl)
        registry.add("spring.datasource.username", pgContainer::getUsername)
        registry.add("spring.datasource.password", pgContainer::getPassword)
        registry.add("spring.r2dbc.url") { pgContainer.jdbcUrl.replace("jdbc:", "r2dbc:") }
        registry.add("spring.r2dbc.username", pgContainer::getUsername)
        registry.add("spring.r2dbc.password", pgContainer::getPassword)
      }
    }
  }
}
