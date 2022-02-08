package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.TestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.MappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.VisitId

@DataR2dbcTest
@ActiveProfiles("test")
@WithMockUser
class VisitIdRepositoryTest : TestBase() {
  @Autowired
  lateinit var repository: VisitIdRepository

  @Test
  fun saveVisitId() {
    repository.save(VisitId(123L, "123", "TIMESTAMP", MappingType.MIGRATED))
      .thenMany(repository.findById(123L))
      .`as`(StepVerifier::create)
      .consumeNextWith {
        with(it) {
          assertThat(nomisId).isEqualTo(123L)
          assertThat(vsipId).isEqualTo("123")
          assertThat(label).isEqualTo("TIMESTAMP")
          assertThat(mappingType).isEqualTo(MappingType.MIGRATED)
          assertThat(new).isFalse()
        }
      }
      .verifyComplete()
  }
}
