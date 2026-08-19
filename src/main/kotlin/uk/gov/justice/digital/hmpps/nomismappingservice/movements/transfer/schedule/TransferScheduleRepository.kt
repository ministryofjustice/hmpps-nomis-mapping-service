package uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.schedule

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TransferScheduleRepository : CoroutineCrudRepository<TransferScheduleMapping, UUID> {
  suspend fun findByNomisEventId(nomisEventId: Long): TransferScheduleMapping?
  suspend fun deleteByNomisEventId(nomisEventId: Long)
  suspend fun deleteByOffenderNo(offenderNo: String)
}
