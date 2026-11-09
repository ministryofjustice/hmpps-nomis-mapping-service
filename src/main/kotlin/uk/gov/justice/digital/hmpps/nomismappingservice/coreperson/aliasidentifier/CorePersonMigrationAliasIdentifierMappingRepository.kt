package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.aliasidentifier

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingType

@Repository
interface CorePersonMigrationAliasIdentifierMappingRepository : CoroutineCrudRepository<CorePersonMigrationAliasIdentifier, String> {
  suspend fun findOneByNomisPrisonNumber(nomisPrisonNumber: String): CorePersonMigrationAliasIdentifier?
  suspend fun findOneByCprId(cprId: String): CorePersonMigrationAliasIdentifier?
  suspend fun findAllBy(pageRequest: Pageable): Flow<CorePersonMigrationAliasIdentifier>
  suspend fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: CorePersonMappingType, pageRequest: Pageable): Flow<CorePersonMigrationAliasIdentifier>
  suspend fun countAllByLabelAndMappingType(migrationId: String, mappingType: CorePersonMappingType): Long
}
