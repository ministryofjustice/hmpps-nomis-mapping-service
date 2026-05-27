package uk.gov.justice.digital.hmpps.nomismappingservice.users

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserMappingRepository : CoroutineCrudRepository<UserMapping, String> {
  suspend fun findOneByNomisId(nomisId: Long): UserMapping?
  suspend fun findOneByDpsId(dpsId: String): UserMapping?

  suspend fun countAllByLabel(migrationId: String): Long
  suspend fun findAllByLabelOrderByLabelDesc(label: String, pageRequest: Pageable): Flow<UserMapping>
}
