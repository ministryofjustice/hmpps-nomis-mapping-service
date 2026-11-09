package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.aliasidentifier

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CorePersonMigrationAliasIdentifierService(
  private val corePersonMigrationAliasIdentifierMappingRepository: CorePersonMigrationAliasIdentifierMappingRepository,
  private val offenderAliasMappingRepository: OffenderAliasMappingRepository,
  private val offenderIdentifierMappingRepository: OffenderIdentifierMappingRepository,
) {

  @Transactional
  suspend fun createMappings(mappings: CorePersonAliasIdentifierMappingsDto) {
    with(mappings) {
      corePersonMigrationAliasIdentifierMappingRepository.save(toCorePersonMapping())
      offenderAliasMappingRepository.deleteAllByNomisPrisonNumber(personMapping.nomisPrisonNumber)
      offenderIdentifierMappingRepository.deleteAllByNomisPrisonNumber(personMapping.nomisPrisonNumber)
      aliases.forEach { offenderAliasMappingRepository.save(it.toMapping()) }
      identifiers.forEach { offenderIdentifierMappingRepository.save(it.toMapping()) }
    }
  }

  suspend fun getCorePersonMappingsByMigrationId(
    pageRequest: Pageable,
    migrationId: String,
  ): Page<CorePersonMappingDto> = coroutineScope {
    val mappings = async {
      corePersonMigrationAliasIdentifierMappingRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
        label = migrationId,
        mappingType = CorePersonMappingType.MIGRATED,
        pageRequest = pageRequest,
      )
    }

    val count = async {
      corePersonMigrationAliasIdentifierMappingRepository.countAllByLabelAndMappingType(
        migrationId = migrationId,
        mappingType = CorePersonMappingType.MIGRATED,
      )
    }

    PageImpl(
      mappings.await().toList().map { it.toDto() },
      pageRequest,
      count.await(),
    )
  }

  suspend fun getCorePersonMappingByNomisPrisonNumber(nomisPrisonNumber: String) = corePersonMigrationAliasIdentifierMappingRepository.findOneByNomisPrisonNumber(nomisPrisonNumber = nomisPrisonNumber)
    ?.toDto()
    ?: throw NotFoundException("No core person mapping found for nomisPrisonNumber=$nomisPrisonNumber")

  suspend fun getCorePersonMappingByCprIdOrNull(cprId: String) = corePersonMigrationAliasIdentifierMappingRepository.findOneByCprId(cprId = cprId)
    ?.toDto()

  @Transactional
  suspend fun deleteAllMappings() {
    corePersonMigrationAliasIdentifierMappingRepository.deleteAll()
    offenderAliasMappingRepository.deleteAll()
    offenderIdentifierMappingRepository.deleteAll()
  }
}

private fun CorePersonAliasIdentifierMappingsDto.toCorePersonMapping() = CorePersonMigrationAliasIdentifier(
  cprId = personMapping.cprId,
  nomisPrisonNumber = personMapping.nomisPrisonNumber,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private fun CorePersonMigrationAliasIdentifier.toDto() = CorePersonMappingDto(
  cprId = cprId,
  nomisPrisonNumber = nomisPrisonNumber,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
