package uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.migration

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PersonLocationMigrationRepository : CoroutineCrudRepository<PersonLocationMigration, String>
