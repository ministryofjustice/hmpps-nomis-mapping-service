package uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.migration

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TransferSchedulerMigrationRepository : CoroutineCrudRepository<TransferSchedulerMigration, String>
