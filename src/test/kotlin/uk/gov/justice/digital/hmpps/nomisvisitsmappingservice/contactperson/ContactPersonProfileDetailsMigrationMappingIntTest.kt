package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson.profiledetails.ContactPersonProfileDetailMigrationMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson.profiledetails.ContactPersonProfileDetailMigrationService
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ContactPersonProfileDetailsMigrationMappingIntTest(
  @Autowired private val service: ContactPersonProfileDetailMigrationService,
  @Autowired private val repository: ContactPersonProfileDetailMigrationMappingRepository,
) : IntegrationTestBase() {

  @Nested
  inner class CreateMigrationMapping {
    @BeforeEach
    fun tearDown() = runTest {
      repository.deleteAll()
    }

    @Test
    fun `should insert mapping`() = runTest {
      val migrationId = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString()
      service.insert(
        nomisPrisonerNumber = "A1234BC",
        label = migrationId,
        domesticStatusDpsIds = "1,2,3",
        numberOfChildrenDpsIds = "4,5,6",
      )

      with(service.find("A1234BC", migrationId)!!) {
        assertThat(domesticStatusDpsIds).isEqualTo("1,2,3")
        assertThat(numberOfChildrenDpsIds).isEqualTo("4,5,6")
        assertThat("$whenCreated").startsWith("${LocalDate.now()}")
      }
    }

    @Test
    fun `should update mapping`() = runTest {
      val migrationId = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString()
      service.insert(
        nomisPrisonerNumber = "A1234BC",
        label = migrationId,
        domesticStatusDpsIds = "1,2,3",
        numberOfChildrenDpsIds = "4,5,6",
      )
      service.update(
        nomisPrisonerNumber = "A1234BC",
        label = migrationId,
        domesticStatusDpsIds = "7,8,9",
        numberOfChildrenDpsIds = "10,11,12",
      )

      with(service.find("A1234BC", migrationId)!!) {
        assertThat(domesticStatusDpsIds).isEqualTo("7,8,9")
        assertThat(numberOfChildrenDpsIds).isEqualTo("10,11,12")
        assertThat("$whenCreated").startsWith("${LocalDate.now()}")
      }
    }

    @Test
    fun `should upsert mapping`() = runTest {
      val migrationId = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString()
      service.upsert(
        nomisPrisonerNumber = "A1234BC",
        label = migrationId,
        domesticStatusDpsIds = "1,2,3",
        numberOfChildrenDpsIds = "4,5,6",
      )
      with(service.find("A1234BC", migrationId)!!) {
        assertThat(domesticStatusDpsIds).isEqualTo("1,2,3")
        assertThat(numberOfChildrenDpsIds).isEqualTo("4,5,6")
        assertThat("$whenCreated").startsWith("${LocalDate.now()}")
      }

      service.upsert(
        nomisPrisonerNumber = "A1234BC",
        label = migrationId,
        domesticStatusDpsIds = "7,8,9",
        numberOfChildrenDpsIds = "10,11,12",
      )
      with(service.find("A1234BC", migrationId)!!) {
        assertThat(domesticStatusDpsIds).isEqualTo("7,8,9")
        assertThat(numberOfChildrenDpsIds).isEqualTo("10,11,12")
        assertThat("$whenCreated").startsWith("${LocalDate.now()}")
      }
    }

    @Test
    fun `should allow multiple mappings for same prisoner number`() = runTest {
      val firstMigrationId = LocalDateTime.now().minusDays(2).truncatedTo(ChronoUnit.SECONDS).toString()
      service.upsert(
        nomisPrisonerNumber = "A1234BC",
        label = firstMigrationId,
        domesticStatusDpsIds = "1,2,3",
        numberOfChildrenDpsIds = "4,5,6",
      )
      val secondMigrationId = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.SECONDS).toString()
      service.upsert(
        nomisPrisonerNumber = "A1234BC",
        label = secondMigrationId,
        domesticStatusDpsIds = "7,8,9",
        numberOfChildrenDpsIds = "10,11,12",
      )

      with(service.find("A1234BC", firstMigrationId)!!) {
        assertThat(domesticStatusDpsIds).isEqualTo("1,2,3")
        assertThat(numberOfChildrenDpsIds).isEqualTo("4,5,6")
        assertThat("$whenCreated").startsWith("${LocalDate.now()}")
      }
      with(service.find("A1234BC", secondMigrationId)!!) {
        assertThat(domesticStatusDpsIds).isEqualTo("7,8,9")
        assertThat(numberOfChildrenDpsIds).isEqualTo("10,11,12")
        assertThat("$whenCreated").startsWith("${LocalDate.now()}")
      }
    }
  }
}