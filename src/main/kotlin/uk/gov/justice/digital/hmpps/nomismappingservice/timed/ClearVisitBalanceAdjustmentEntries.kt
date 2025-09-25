package uk.gov.justice.digital.hmpps.nomismappingservice.timed
import com.microsoft.applicationinsights.TelemetryClient
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.visitbalances.VisitBalanceAdjustmentMappingRepository
import java.time.LocalDate
import kotlin.jvm.java
import kotlin.jvm.javaClass

@Component
class ClearVisitBalanceAdjustmentEntries(
  @Value("\${visit-balance-adjustment.expiry.days:28}") val expiryDays: Long,
  private val service: ClearVisitBalanceAdjustmentService,
  private val telemetryClient: TelemetryClient,
) {
  @SchedulerLock(
    name = "Clear expired visit adjustment mappings",
    lockAtLeastFor = "PT1M",
    lockAtMostFor = "PT5M",
  )
  @Scheduled(cron = "\${visit-balance-adjustment.expiry.schedule.cron}")
  suspend fun removeOldVisitBalanceAdjustment() {
    try {
      service.clearExpiredEntries(expiryDays)
      telemetryClient.trackEvent("Cleared visit balance adjustment entries for last $expiryDays days")
    } catch (e: Exception) {
      // have to catch the exception here otherwise scheduling will stop
      log.error("Caught exception {} during removal of old visit balance adjustment entries", e.javaClass.simpleName, e)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

@Service
class ClearVisitBalanceAdjustmentService(private val repository: VisitBalanceAdjustmentMappingRepository) {
  @Transactional
  suspend fun clearExpiredEntries(expiryDays: Long) {
    val expiry = LocalDate.now().atStartOfDay().minusDays(expiryDays)
    repository.deleteByWhenCreatedBefore(expiry)
  }
}
