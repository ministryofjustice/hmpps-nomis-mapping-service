package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class PrisonPersonMigrationService(
  private val repository: PrisonPersonMigrationMappingRepository,
  private val prisonPersonMigrationMappingRepository: PrisonPersonMigrationMappingRepository,
) {

  suspend fun create(mappingRequest: PrisonPersonMigrationMappingRequest) {
    repository.save(
      PrisonPersonMigrationMapping(
        nomisPrisonerNumber = mappingRequest.nomisPrisonerNumber,
        migrationType = mappingRequest.migrationType,
        dpsIds = mappingRequest.dpsIds.toString(),
        label = mappingRequest.label,
      ),
    )
  }

  suspend fun find(nomisPrisonerNumber: String, migrationType: PrisonPersonMigrationType) = repository.findByNomisPrisonerNumberAndMigrationType(nomisPrisonerNumber, migrationType)

  suspend fun getMappings(
    pageRequest: Pageable,
    migrationId: String,
  ): Page<PrisonPersonMigrationMapping> =
    coroutineScope {
      val migrationMappings = async {
        prisonPersonMigrationMappingRepository.findAllByLabelOrderByNomisPrisonerNumberAsc(label = migrationId, pageRequest)
      }

      val count = async {
        prisonPersonMigrationMappingRepository.countAllByLabel(migrationId)
      }

      PageImpl(
        migrationMappings.await().toList(),
        pageRequest,
        count.await(),
      )
    }
}
