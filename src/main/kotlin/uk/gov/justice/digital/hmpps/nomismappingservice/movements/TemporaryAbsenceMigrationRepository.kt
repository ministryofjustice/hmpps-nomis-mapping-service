package uk.gov.justice.digital.hmpps.nomismappingservice.movements

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TemporaryAbsenceMigrationRepository : CoroutineCrudRepository<TemporaryAbsenceMigration, String>
