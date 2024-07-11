package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson

import org.springframework.stereotype.Service

@Service
class PrisonPersonMigrationService(
  private val repository: PrisonPersonMigrationMappingRepository,
) {

  suspend fun create(mappingRequest: PrisonPersonMigrationMappingRequest) {
    repository.save(
      PrisonPersonMigrationMapping(
        nomisPrisonerNumber = mappingRequest.nomisPrisonerNumber,
        migrationType = mappingRequest.migrationType,
        label = mappingRequest.label,
      ),
    )
  }

  suspend fun find(nomisPrisonerNumber: String, migrationType: PrisonPersonMigrationType) = repository.findByNomisPrisonerNumberAndMigrationType(nomisPrisonerNumber, migrationType)
}
