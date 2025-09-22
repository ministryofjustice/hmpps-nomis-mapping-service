package uk.gov.justice.digital.hmpps.nomismappingservice.timed

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.nomismappingservice.visitbalances.VisitBalanceAdjustmentMappingRepository
import java.time.LocalDate
import java.time.LocalTime

class ClearVisitBalanceAdjustmentServiceTest {
  val repository: VisitBalanceAdjustmentMappingRepository = mock()
  val service = ClearVisitBalanceAdjustmentService(repository)

  @Test
  fun clearVisitBalanceAdjustments() = runBlocking {
    service.clearExpiredEntries(4)
    verify(repository).deleteByWhenCreatedBefore(
      check {
        assertThat(it.toLocalDate()).isEqualTo(LocalDate.now().minusDays(4))
        assertThat(it.toLocalTime()).isEqualTo(LocalTime.MIDNIGHT)
      },
    )
  }
}
