package uk.gov.justice.digital.hmpps.nomismappingservice.officialvisits

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.DayOfWeek

@Repository
interface VisitTimeSlotMappingRepository : CoroutineCrudRepository<VisitTimeSlotMapping, String> {
  suspend fun findOneByNomisPrisonIdAndNomisDayOfWeekAndNomisSlotSequence(
    nomisPrisonId: String,
    nomisDayOfWeek: DayOfWeek,
    nomisSlotSequence: Int,
  ): VisitTimeSlotMapping?

  suspend fun findOneByDpsId(
    dpsId: String,
  ): VisitTimeSlotMapping?

  @Suppress("unused")
  suspend fun countAllByLabel(migrationId: String): Long
}
