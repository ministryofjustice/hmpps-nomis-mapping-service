package uk.gov.justice.digital.hmpps.nomismappingservice.officialvisits

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface OfficialVisitMappingRepository : CoroutineCrudRepository<OfficialVisitMapping, String> {
  suspend fun findOneByNomisId(
    nomisId: Long,
  ): OfficialVisitMapping?

  suspend fun findOneByDpsId(
    dpsId: String,
  ): OfficialVisitMapping?

  suspend fun countAllByLabel(migrationId: String): Long
  suspend fun findAllByLabelOrderByLabelDesc(label: String, pageRequest: Pageable): Flow<OfficialVisitMapping>
}
