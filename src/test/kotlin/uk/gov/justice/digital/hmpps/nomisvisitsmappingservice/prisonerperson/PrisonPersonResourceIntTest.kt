package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonerperson

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.PrisonPersonMigrationMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.PrisonPersonMigrationService
import java.time.LocalDate

class PrisonPersonResourceIntTest : IntegrationTestBase() {

  // TODO SDIT-1825 replace service with calls to the resource when it's been created
  @Autowired
  private lateinit var service: PrisonPersonMigrationService

  @Autowired
  private lateinit var repository: PrisonPersonMigrationMappingRepository

  @Nested
  inner class CreateMigrationMapping {
    @Test
    fun `should create migration mapping`() = runTest {
      service.create("A1234AA", "label")

      val mapping = repository.findById("A1234AA")
      assertThat(mapping?.nomisPrisonerNumber).isEqualTo("A1234AA")
      assertThat(mapping?.label).isEqualTo("label")
      assertThat(mapping?.whenCreated?.toLocalDate()).isEqualTo(LocalDate.now())
    }
  }
}
