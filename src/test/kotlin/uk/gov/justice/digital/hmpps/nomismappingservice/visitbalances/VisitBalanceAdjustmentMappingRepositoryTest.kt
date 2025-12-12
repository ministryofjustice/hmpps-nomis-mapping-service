package uk.gov.justice.digital.hmpps.nomismappingservice.visitbalances

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomismappingservice.helper.TestBase
import uk.gov.justice.digital.hmpps.nomismappingservice.visitbalances.VisitBalanceAdjustmentMappingType.NOMIS_CREATED
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.time.LocalDateTime

@DataR2dbcTest
@ActiveProfiles("test")
@WithMockAuthUser
class VisitBalanceAdjustmentMappingRepositoryTest : TestBase() {
  @Autowired
  private lateinit var repository: VisitBalanceAdjustmentMappingRepository

  @AfterEach
  fun tearDown() = runTest {
    repository.deleteAll()
  }

  @Test
  fun saveMappingByNomisId(): Unit = runBlocking {
    repository.save(
      VisitBalanceAdjustmentMapping(
        dpsId = "123",
        nomisId = 456,
        label = "TIMESTAMP",
        mappingType = NOMIS_CREATED,
      ),
    )

    val persistedMappingByNomisId = repository.findOneByNomisId(456L) ?: throw RuntimeException("456L not found")
    with(persistedMappingByNomisId) {
      assertThat(dpsId).isEqualTo("123")
      assertThat(nomisId).isEqualTo(456L)
      assertThat(label).isEqualTo("TIMESTAMP")
      assertThat(mappingType).isEqualTo(NOMIS_CREATED)
    }
  }

  @Test
  fun deleteExpiredMappings(): Unit = runBlocking {
    repository.saveAll(
      listOf(
        VisitBalanceAdjustmentMapping(
          dpsId = "123",
          nomisId = 111,
          label = "TIMESTAMP1",
          mappingType = NOMIS_CREATED,
          whenCreated = LocalDateTime.parse("2025-05-12T11:30:00"),
        ),
        VisitBalanceAdjustmentMapping(
          dpsId = "123",
          nomisId = 222,
          label = "TIMESTAMP2",
          mappingType = NOMIS_CREATED,
          whenCreated = LocalDateTime.parse("2025-06-18T19:30:00"),
        ),
        VisitBalanceAdjustmentMapping(
          dpsId = "123",
          nomisId = 333,
          label = "TIMESTAMP3",
          mappingType = NOMIS_CREATED,
          whenCreated = LocalDateTime.parse("2025-06-17T12:30:00"),
        ),
        VisitBalanceAdjustmentMapping(
          dpsId = "124",
          nomisId = 444,
          label = "TIMESTAMP4",
          mappingType = NOMIS_CREATED,
          whenCreated = LocalDateTime.parse("2025-06-12T23:30:00"),
        ),
      ),
    ).collect()
    assertThat(repository.count()).isEqualTo(4)

    repository.deleteByWhenCreatedBefore(LocalDateTime.parse("2025-06-17T00:00:00"))
    assertThat(repository.count()).isEqualTo(2)
    assertThat(repository.findOneByNomisId(222)?.label).isEqualTo("TIMESTAMP2")
    assertThat(repository.findOneByNomisId(333)?.label).isEqualTo("TIMESTAMP3")
    assertThat(repository.count()).isEqualTo(2)
  }
}
