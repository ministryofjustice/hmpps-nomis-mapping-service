package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson.profiledetails

import kotlinx.coroutines.reactive.awaitSingle
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
    nomisPrisonerNumber: String,
    label: String,
    domesticStatusDpsIds: String,
    numberOfChildrenDpsIds: String,
  ) = find(nomisPrisonerNumber, label)
    ?.let { update(nomisPrisonerNumber, label, domesticStatusDpsIds, numberOfChildrenDpsIds) }
    ?: insert(nomisPrisonerNumber, label, domesticStatusDpsIds, numberOfChildrenDpsIds)

  suspend fun find(
    nomisPrisonerNumber: String,
    label: String,
  ) = repository.findByNomisPrisonerNumberAndLabel(nomisPrisonerNumber, label)

  suspend fun insert(
    nomisPrisonerNumber: String,
    label: String,
    domesticStatusDpsIds: String,
    numberOfChildrenDpsIds: String,
  ) = template.insert<ContactPersonProfileDetailMigrationMapping>()
    .using(
      ContactPersonProfileDetailMigrationMapping(
        nomisPrisonerNumber = nomisPrisonerNumber,
        label = label,
        domesticStatusDpsIds = domesticStatusDpsIds,
        numberOfChildrenDpsIds = numberOfChildrenDpsIds,
      ),
    )
    .awaitSingle()

  suspend fun update(
    nomisPrisonerNumber: String,
    label: String,
    domesticStatusDpsIds: String,
    numberOfChildrenDpsIds: String,
  ) = template.update<ContactPersonProfileDetailMigrationMapping>()
    .inTable("contact_person_profile_detail_migration_mapping")
    .matching(
      query(
        where("nomis_prisoner_number").`is`(nomisPrisonerNumber)
          .and(where("label").`is`(label)),
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
    .let { find(nomisPrisonerNumber, label) }
}
