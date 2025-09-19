package uk.gov.justice.digital.hmpps.nomismappingservice.jpa.repository

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.MappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.VisitId
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser

@DataR2dbcTest
@ActiveProfiles("test")
@WithMockAuthUser
class VisitIdRepositoryTest : TestBase() {
  @Autowired
  lateinit var repository: VisitIdRepository

  @BeforeEach
  fun removeVisitId(): Unit = runBlocking {
    repository.deleteById(123)
  }

  @Test
  fun saveVisitId(): Unit = runBlocking {
    repository.save(VisitId(123, "123", "TIMESTAMP", MappingType.MIGRATED))

    val persistedVisitId = repository.findById(123L) ?: throw RuntimeException("123L not found")
    with(persistedVisitId) {
      assertThat(nomisId).isEqualTo(123)
      assertThat(vsipId).isEqualTo("123")
      assertThat(label).isEqualTo("TIMESTAMP")
      assertThat(mappingType).isEqualTo(MappingType.MIGRATED)
    }
  }
}
