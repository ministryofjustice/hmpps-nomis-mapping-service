package uk.gov.justice.digital.hmpps.nomismappingservice.officialvisits

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface VisitorMappingRepository : CoroutineCrudRepository<VisitorMapping, String> {

  suspend fun findOneByDpsId(
    dpsId: String,
  ): VisitorMapping?

  suspend fun findOneByNomisId(
    nomisId: Long,
  ): VisitorMapping?

  suspend fun deleteByNomisId(nomisId: Long)
}
