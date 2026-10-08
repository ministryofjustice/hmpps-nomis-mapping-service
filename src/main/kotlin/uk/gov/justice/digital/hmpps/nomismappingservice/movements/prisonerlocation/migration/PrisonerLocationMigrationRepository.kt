package uk.gov.justice.digital.hmpps.nomismappingservice.movements.prisonerlocation.migration

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PrisonerLocationMigrationRepository : CoroutineCrudRepository<PrisonerLocationMigration, String>
