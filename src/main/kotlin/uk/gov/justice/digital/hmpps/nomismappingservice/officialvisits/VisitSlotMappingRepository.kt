package uk.gov.justice.digital.hmpps.nomismappingservice.officialvisits

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface VisitSlotMappingRepository : CoroutineCrudRepository<VisitSlotMapping, String> {

  suspend fun findOneByDpsId(
    dpsId: String,
  ): VisitSlotMapping?

  suspend fun findOneByNomisId(
    nomisId: Long,
  ): VisitSlotMapping?

  suspend fun deleteByNomisId(
    nomisId: Long,
  )
}
