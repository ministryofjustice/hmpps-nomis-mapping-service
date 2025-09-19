package uk.gov.justice.digital.hmpps.nomismappingservice.jpa.repository

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.SentencingAdjustmentMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.SentencingMappingType.MIGRATED
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser

@DataR2dbcTest
@ActiveProfiles("test")
@WithMockAuthUser
class SentencingAdjustmentMappingRepositoryTest : TestBase() {
  @Qualifier("sentenceAdjustmentMappingRepository")
  @Autowired
  lateinit var repository: SentenceAdjustmentMappingRepository

  @Test
  fun saveSentenceAdjustmentMapping(): Unit = runBlocking {
    repository.save(
      SentencingAdjustmentMapping(
        adjustmentId = "123",
        nomisAdjustmentId = 456,
        nomisAdjustmentCategory = "SENTENCE",
        label = "TIMESTAMP",
        mappingType = MIGRATED,
      ),
    )

    val persistedSentenceAdjustmentMappingBySentenceAdjustmentId = repository.findById("123") ?: throw RuntimeException("123L not found")
    with(persistedSentenceAdjustmentMappingBySentenceAdjustmentId) {
      assertThat(adjustmentId).isEqualTo("123")
      assertThat(nomisAdjustmentId).isEqualTo(456L)
      assertThat(nomisAdjustmentCategory).isEqualTo("SENTENCE")
      assertThat(label).isEqualTo("TIMESTAMP")
      assertThat(mappingType).isEqualTo(MIGRATED)
    }

    val persistedSentenceAdjustmentMappingByNomisId = repository.findOneByNomisAdjustmentIdAndNomisAdjustmentCategory(456L, "SENTENCE") ?: throw RuntimeException("456L not found")
    with(persistedSentenceAdjustmentMappingByNomisId) {
      assertThat(adjustmentId).isEqualTo("123")
      assertThat(nomisAdjustmentId).isEqualTo(456L)
      assertThat(nomisAdjustmentCategory).isEqualTo("SENTENCE")
      assertThat(label).isEqualTo("TIMESTAMP")
      assertThat(mappingType).isEqualTo(MIGRATED)
    }
  }
}
