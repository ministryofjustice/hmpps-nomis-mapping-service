package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact

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
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMigrationAddressContact
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMigrationAddressContactMappingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CorePersonMigrationAddressContactService(
  private val corePersonAddressContactService: CorePersonAddressContactService,
  private val corePersonMigrationAddressContactMappingRepository: CorePersonMigrationAddressContactMappingRepository,
) {

  @Transactional
  suspend fun createMappings(mappings: CorePersonAddressContactMappingsDto) {
    with(mappings) {
      corePersonMigrationAddressContactMappingRepository.save(toCorePersonMapping())
      corePersonAddressContactService.replaceMappings(this)
    }
  }

  suspend fun getCorePersonMappingsByMigrationId(
    pageRequest: Pageable,
    migrationId: String,
  ): Page<CorePersonMappingDto> = coroutineScope {
    val mappings = async {
      corePersonMigrationAddressContactMappingRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
        label = migrationId,
        mappingType = CorePersonMappingType.MIGRATED,
        pageRequest = pageRequest,
      )
    }

    val count = async {
      corePersonMigrationAddressContactMappingRepository.countAllByLabelAndMappingType(
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

  suspend fun getCorePersonMappingByNomisPrisonNumber(nomisPrisonNumber: String) = corePersonMigrationAddressContactMappingRepository.findOneByNomisPrisonNumber(nomisPrisonNumber = nomisPrisonNumber)
    ?.toDto()
    ?: throw NotFoundException("No core person mapping found for nomisPrisonNumber=$nomisPrisonNumber")

  suspend fun getCorePersonMappingByCprIdOrNull(cprId: String) = corePersonMigrationAddressContactMappingRepository.findOneByCprId(cprId = cprId)
    ?.toDto()

  @Transactional
  suspend fun deleteAllMappings() {
    corePersonMigrationAddressContactMappingRepository.deleteAll()
    corePersonAddressContactService.deleteAll()
  }
}

private fun CorePersonAddressContactMappingsDto.toCorePersonMapping() = CorePersonMigrationAddressContact(
  cprId = personMapping.cprId,
  nomisPrisonNumber = personMapping.nomisPrisonNumber,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private fun CorePersonMigrationAddressContact.toDto() = CorePersonMappingDto(
  cprId = cprId,
  nomisPrisonNumber = nomisPrisonNumber,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
