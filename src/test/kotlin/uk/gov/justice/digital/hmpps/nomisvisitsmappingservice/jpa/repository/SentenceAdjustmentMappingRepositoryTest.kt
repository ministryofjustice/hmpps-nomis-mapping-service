package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.TestBase
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.SentenceAdjustmentMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.SentencingMappingType.MIGRATED

@DataR2dbcTest
@ActiveProfiles("test")
@WithMockUser
class SentenceAdjustmentMappingRepositoryTest : TestBase() {
  @Qualifier("sentenceAdjustmentMappingRepository")
  @Autowired
  lateinit var repository: SentenceAdjustmentMappingRepository

  @Test
  fun saveSentenceAdjustmentMapping(): Unit = runBlocking {

    repository.save(
      SentenceAdjustmentMapping(
        sentenceAdjustmentId = 123,
        nomisAdjustmentId = 456,
        nomisAdjustmentCategory = "SENTENCE",
        label = "TIMESTAMP",
        mappingType = MIGRATED
      )
    )

    val persistedSentenceAdjustmentMappingBySentenceAdjustmentId = repository.findById(123L) ?: throw RuntimeException("123L not found")
    with(persistedSentenceAdjustmentMappingBySentenceAdjustmentId) {
      assertThat(sentenceAdjustmentId).isEqualTo(123L)
      assertThat(nomisAdjustmentId).isEqualTo(456L)
      assertThat(nomisAdjustmentCategory).isEqualTo("SENTENCE")
      assertThat(label).isEqualTo("TIMESTAMP")
      assertThat(mappingType).isEqualTo(MIGRATED)
    }

    val persistedSentenceAdjustmentMappingByNomisId = repository.findOneByNomisAdjustmentIdAndNomisAdjustmentCategory(456L, "SENTENCE") ?: throw RuntimeException("456L not found")
    with(persistedSentenceAdjustmentMappingByNomisId) {
      assertThat(sentenceAdjustmentId).isEqualTo(123L)
      assertThat(nomisAdjustmentId).isEqualTo(456L)
      assertThat(nomisAdjustmentCategory).isEqualTo("SENTENCE")
      assertThat(label).isEqualTo("TIMESTAMP")
      assertThat(mappingType).isEqualTo(MIGRATED)
    }
  }
}
