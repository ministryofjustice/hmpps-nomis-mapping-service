package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson

import org.springframework.stereotype.Service

@Service
class PrisonPersonMigrationService(
  private val repository: PrisonPersonMigrationMappingRepository,
) {

  suspend fun create(nomisPrisonerNumber: String, label: String) {
    repository.save(PrisonPersonMigrationMapping(nomisPrisonerNumber = nomisPrisonerNumber, label = label))
  }
}
