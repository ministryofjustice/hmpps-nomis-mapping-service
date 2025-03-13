package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson.profiledetails

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ContactPersonProfileDetailMigrationMappingRepository : CoroutineCrudRepository<ContactPersonProfileDetailMigrationMapping, String> {
  suspend fun findByNomisPrisonerNumberAndLabel(nomisPrisonerNumber: String, label: String): ContactPersonProfileDetailMigrationMapping?
  fun findAllByLabelOrderByNomisPrisonerNumberAsc(label: String, pageable: Pageable): Flow<ContactPersonProfileDetailMigrationMapping>
  suspend fun countAllByLabel(label: String): Long
}
