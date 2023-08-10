package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AllocationMigrationMapping

@Repository
interface AllocationMigrationMappingRepository : CoroutineCrudRepository<AllocationMigrationMapping, Long> {
  suspend fun findOneByActivityAllocationId(activityAllocationId: Long): AllocationMigrationMapping?

  suspend fun findFirstByOrderByWhenCreatedDesc(): AllocationMigrationMapping?
}
