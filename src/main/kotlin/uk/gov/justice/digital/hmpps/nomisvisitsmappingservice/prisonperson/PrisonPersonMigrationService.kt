package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.core.insert
import org.springframework.data.r2dbc.core.update
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.data.relational.core.query.Query.query
import org.springframework.data.relational.core.query.Update.update
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PrisonPersonMigrationService(
  private val repository: PrisonPersonMigrationMappingRepository,
  private val prisonPersonMigrationMappingRepository: PrisonPersonMigrationMappingRepository,
  private val template: R2dbcEntityTemplate,
) {
  @Transactional
  suspend fun upsert(mappingRequest: PrisonPersonMigrationMappingRequest): PrisonPersonMigrationMapping =
    mappingRequest.find()
      ?.let { mappingRequest.update() }
      ?: let { mappingRequest.insert() }

  private suspend fun PrisonPersonMigrationMappingRequest.find() =
    repository.findByNomisPrisonerNumberAndMigrationTypeAndLabel(nomisPrisonerNumber, migrationType, label)

  private suspend fun PrisonPersonMigrationMappingRequest.update(): PrisonPersonMigrationMapping =
    template.update<PrisonPersonMigrationMapping>()
      .inTable("prison_person_migration_mapping")
      .matching(
        query(
          where("nomis_prisoner_number").`is`(nomisPrisonerNumber)
            .and(where("migration_type").`is`(migrationType))
            .and(where("label").`is`(label)),
        ),
      )
      .apply(update("dps_ids", dpsIds.toString()))
      .awaitSingle()
      .let { find()!! }

  private suspend fun PrisonPersonMigrationMappingRequest.insert(): PrisonPersonMigrationMapping =
    template.insert<PrisonPersonMigrationMapping>()
      .using(
        PrisonPersonMigrationMapping(
          nomisPrisonerNumber = nomisPrisonerNumber,
          migrationType = migrationType,
          dpsIds = dpsIds.toString(),
          label = label,
        ),
      )
      .awaitSingle()

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
