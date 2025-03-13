package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson.profiledetails

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.core.insert
import org.springframework.data.r2dbc.core.update
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.data.relational.core.query.Query.query
import org.springframework.data.relational.core.query.Update.from
import org.springframework.data.relational.core.sql.SqlIdentifier
import org.springframework.data.relational.core.sql.SqlIdentifier.unquoted
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ContactPersonProfileDetailMigrationService(
  private val repository: ContactPersonProfileDetailMigrationMappingRepository,
  private val template: R2dbcEntityTemplate,
) {

  @Transactional
  suspend fun upsert(
    mappingRequest: ContactPersonProfileDetailsMigrationMappingRequest,
  ): ContactPersonProfileDetailMigrationMapping = mappingRequest.find()
    ?.let { mappingRequest.update() }
    ?: mappingRequest.insert()

  private suspend fun ContactPersonProfileDetailsMigrationMappingRequest.find() = repository.findByNomisPrisonerNumberAndLabel(prisonerNumber, migrationId)

  private suspend fun ContactPersonProfileDetailsMigrationMappingRequest.insert() = template.insert<ContactPersonProfileDetailMigrationMapping>()
    .using(
      ContactPersonProfileDetailMigrationMapping(
        prisonerNumber,
        migrationId,
        domesticStatusDpsIds,
        numberOfChildrenDpsIds,
      ),
    )
    .awaitSingle()

  private suspend fun ContactPersonProfileDetailsMigrationMappingRequest.update() = template.update<ContactPersonProfileDetailMigrationMapping>()
    .inTable("contact_person_profile_detail_migration_mapping")
    .matching(
      query(
        where("nomis_prisoner_number").`is`(prisonerNumber)
          .and(where("label").`is`(migrationId)),
      ),
    )
    .apply(
      from(
        mapOf<SqlIdentifier, Any>(
          unquoted("domestic_status_dps_ids") to domesticStatusDpsIds,
          unquoted("number_of_children_dps_ids") to numberOfChildrenDpsIds,
        ),
      ),
    )
    .awaitSingle()
    .let { find()!! }

  suspend fun getMappings(
    pageRequest: Pageable,
    migrationId: String,
  ): Page<ContactPersonProfileDetailMigrationMapping> = coroutineScope {
    val migrationMappings = async {
      repository.findAllByLabelOrderByNomisPrisonerNumberAsc(label = migrationId, pageRequest)
    }

    val count = async {
      repository.countAllByLabel(migrationId)
    }

    PageImpl(
      migrationMappings.await().toList(),
      pageRequest,
      count.await(),
    )
  }
}
