package uk.gov.justice.digital.hmpps.nomismappingservice.officialvisits

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface VisitTimeSlotMappingRepository : CoroutineCrudRepository<VisitTimeSlotMapping, String> {
  suspend fun findOneByNomisPrisonIdAndNomisDayOfWeekAndNomisSlotSequence(
    nomisPrisonId: String,
    nomisDayOfWeek: String,
    nomisSlotSequence: Int,
  ): VisitTimeSlotMapping?

  suspend fun findOneByDpsId(
    dpsId: String,
  ): VisitTimeSlotMapping?

  suspend fun countAllByLabel(migrationId: String): Long
  suspend fun findAllByLabelOrderByLabelDesc(label: String, pageRequest: Pageable): Flow<VisitTimeSlotMapping>
}
