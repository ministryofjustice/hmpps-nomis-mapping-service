package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CorePersonMigrationAddressContactMappingRepository : CoroutineCrudRepository<CorePersonMigrationAddressContact, String> {
  suspend fun findOneByNomisPrisonNumber(nomisPrisonNumber: String): CorePersonMigrationAddressContact?
  suspend fun findOneByCprId(cprId: String): CorePersonMigrationAddressContact?
  suspend fun findAllBy(pageRequest: Pageable): Flow<CorePersonMigrationAddressContact>
  suspend fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: CorePersonMappingType, pageRequest: Pageable): Flow<CorePersonMigrationAddressContact>
  suspend fun countAllByLabelAndMappingType(migrationId: String, mappingType: CorePersonMappingType): Long
}
