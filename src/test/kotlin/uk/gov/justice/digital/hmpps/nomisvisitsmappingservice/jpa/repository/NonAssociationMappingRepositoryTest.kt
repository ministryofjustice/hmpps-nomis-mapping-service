package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.TestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.NonAssociationMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.NonAssociationMappingType.MIGRATED
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser

@DataR2dbcTest
@ActiveProfiles("test")
@WithMockAuthUser
class NonAssociationMappingRepositoryTest : TestBase() {
  @Autowired
  @Qualifier("nonAssociationMappingRepository")
  lateinit var repository: NonAssociationMappingRepository

  @Test
  fun saveNonAssociationMapping(): Unit = runBlocking {
    repository.save(
      NonAssociationMapping(
        nonAssociationId = 123,
        firstOffenderNo = "A1234BC",
        secondOffenderNo = "D5678EF",
        nomisTypeSequence = 2,
        label = "TIMESTAMP",
        mappingType = MIGRATED,
      ),
    )

    val persistedNonAssociationMappingById = repository.findById(123L) ?: throw RuntimeException("123L not found")
    with(persistedNonAssociationMappingById) {
      assertThat(nonAssociationId).isEqualTo(123)
      assertThat(firstOffenderNo).isEqualTo("A1234BC")
      assertThat(secondOffenderNo).isEqualTo("D5678EF")
      assertThat(nomisTypeSequence).isEqualTo(2)
      assertThat(label).isEqualTo("TIMESTAMP")
      assertThat(mappingType).isEqualTo(MIGRATED)
    }

    val persistedNonAssociationMappingByNomisNonAssociationIds = repository.findOneByFirstOffenderNoAndSecondOffenderNoAndNomisTypeSequence(
      "A1234BC",
      "D5678EF",
      2,
    ) ?: throw RuntimeException("Mapping A1234BC-D5678EF not found")
    with(persistedNonAssociationMappingByNomisNonAssociationIds) {
      assertThat(nonAssociationId).isEqualTo(123)
      assertThat(firstOffenderNo).isEqualTo("A1234BC")
      assertThat(secondOffenderNo).isEqualTo("D5678EF")
      assertThat(nomisTypeSequence).isEqualTo(2)
      assertThat(label).isEqualTo("TIMESTAMP")
      assertThat(mappingType).isEqualTo(MIGRATED)
    }
  }
}
