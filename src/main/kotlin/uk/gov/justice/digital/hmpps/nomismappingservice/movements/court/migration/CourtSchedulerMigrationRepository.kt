package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.migration

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CourtSchedulerMigrationRepository : CoroutineCrudRepository<CourtSchedulerMigration, String>
