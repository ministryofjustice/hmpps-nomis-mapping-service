package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AllocationMigrationMapping

@Repository
interface AllocationMigrationMappingRepository : CoroutineCrudRepository<AllocationMigrationMapping, Long> {
  suspend fun findOneByActivityAllocationId(activityAllocationId: Long): AllocationMigrationMapping?

  suspend fun findFirstByOrderByWhenCreatedDesc(): AllocationMigrationMapping?

  fun findAllByLabelOrderByNomisAllocationIdAsc(label: String, pageable: Pageable): Flow<AllocationMigrationMapping>

  suspend fun countAllByLabel(label: String): Long
}
