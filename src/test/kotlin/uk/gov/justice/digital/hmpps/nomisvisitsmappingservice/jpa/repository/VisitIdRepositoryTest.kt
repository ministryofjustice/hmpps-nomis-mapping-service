package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.TestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.MappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.VisitId

@DataR2dbcTest
@ActiveProfiles("test")
@WithMockUser
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
