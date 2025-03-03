package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.visitorders

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PrisonerVisitOrderMappingRepository : CoroutineCrudRepository<PrisonerVisitOrderMapping, String> {
  suspend fun findOneByNomisPrisonNumber(nomisPrisonNumber: String): PrisonerVisitOrderMapping?
  suspend fun findOneByDpsId(dpsId: String): PrisonerVisitOrderMapping?
  suspend fun findAllBy(pageRequest: Pageable): Flow<PrisonerVisitOrderMapping>
  suspend fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: PrisonerVisitOrderMappingType, pageRequest: Pageable): Flow<PrisonerVisitOrderMapping>
  suspend fun countAllByLabelAndMappingType(migrationId: String, mappingType: PrisonerVisitOrderMappingType): Long
}
