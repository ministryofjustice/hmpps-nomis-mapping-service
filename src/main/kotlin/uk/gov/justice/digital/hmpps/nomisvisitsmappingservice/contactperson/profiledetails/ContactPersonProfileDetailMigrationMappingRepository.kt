package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson.profiledetails

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ContactPersonProfileDetailMigrationMappingRepository : CoroutineCrudRepository<ContactPersonProfileDetailMigrationMapping, String> {
  suspend fun findByNomisPrisonerNumberAndLabel(nomisPrisonerNumber: String, label: String): ContactPersonProfileDetailMigrationMapping?
}
