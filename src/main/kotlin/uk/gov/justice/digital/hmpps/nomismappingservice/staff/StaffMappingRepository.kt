package uk.gov.justice.digital.hmpps.nomismappingservice.staff

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface StaffMappingRepository : CoroutineCrudRepository<StaffMapping, String> {
  suspend fun findOneByNomisId(nomisStaffId: Long): StaffMapping?
  suspend fun findOneByDpsId(dpsId: String): StaffMapping?

  suspend fun countAllByLabel(migrationId: String): Long
  suspend fun findAllByLabelOrderByLabelDesc(label: String, pageRequest: Pageable): Flow<StaffMapping>
}
