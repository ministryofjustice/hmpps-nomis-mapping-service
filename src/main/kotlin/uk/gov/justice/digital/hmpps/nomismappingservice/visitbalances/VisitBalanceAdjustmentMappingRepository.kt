package uk.gov.justice.digital.hmpps.nomismappingservice.visitbalances

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface VisitBalanceAdjustmentMappingRepository : CoroutineCrudRepository<VisitBalanceAdjustmentMapping, String> {
  suspend fun findOneByNomisId(nomisVisitBalanceAdjustmentId: Long): VisitBalanceAdjustmentMapping?
  suspend fun findAllByDpsIdOrderByNomisIdDesc(dpsId: String): List<VisitBalanceAdjustmentMapping>
  suspend fun deleteByWhenCreatedBefore(toDate: LocalDateTime)
}
