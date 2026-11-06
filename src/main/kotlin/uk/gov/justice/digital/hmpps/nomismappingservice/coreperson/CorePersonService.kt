package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson

import jakarta.validation.ValidationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonAddressMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonAddressMappingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonAddressUsageMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonAddressUsageMappingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonContactMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.CorePersonContactMappingRepository
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.toDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact.toMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CorePersonService(
  private val corePersonMigrationAddressContactMappingRepository: CorePersonMigrationAddressContactMappingRepository,
  private val corePersonAddressMappingRepository: CorePersonAddressMappingRepository,
  private val corePersonAddressUsageMappingRepository: CorePersonAddressUsageMappingRepository,
  private val corePersonContactMappingRepository: CorePersonContactMappingRepository,
) {
  @Transactional
  suspend fun merge(mappings: CorePersonMergeMappingsDto) {
    validate(mappings)
    corePersonAddressMappingRepository.deleteAllByNomisPrisonNumber(mappings.fromNomisPrisonNumber)
    corePersonContactMappingRepository.deleteAllByNomisPrisonNumber(mappings.fromNomisPrisonNumber)
    corePersonAddressUsageMappingRepository.deleteAllByNomisPrisonNumber(mappings.fromNomisPrisonNumber)
    replaceAddressesAndContactsMappings(
      prisonNumber = mappings.toNomisPrisonNumber,
      contacts = mappings.contacts,
      addresses = mappings.addresses,
      addressUsages = mappings.addressUsages,
    )
  }

  private fun validate(mappings: CorePersonMergeMappingsDto) {
    if (mappings.toNomisPrisonNumber == mappings.fromNomisPrisonNumber) {
      throw ValidationException("to nomis prison number: ${mappings.toNomisPrisonNumber} and from nomis prison number: ${mappings.toNomisPrisonNumber} cannot be the same")
    }
    if (mappings.addresses.any { it.nomisPrisonNumber != mappings.toNomisPrisonNumber } ||
      mappings.contacts.any { it.nomisPrisonNumber != mappings.toNomisPrisonNumber } ||
      mappings.addressUsages.any { it.nomisPrisonNumber != mappings.toNomisPrisonNumber }
    ) {
      throw ValidationException("Child mappings must have the same nomis prison number as the to nomis prison number: ${mappings.toNomisPrisonNumber}")
    }
  }

  @Transactional
  suspend fun createMappings(mappings: CorePersonAddressContactMappingsDto) {
    with(mappings) {
      corePersonMigrationAddressContactMappingRepository.save(toCorePersonMapping())
      replaceMappings(this)
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
    corePersonContactMappingRepository.deleteAll()
    corePersonAddressMappingRepository.deleteAll()
    corePersonAddressUsageMappingRepository.deleteAll()
    corePersonMigrationAddressContactMappingRepository.deleteAll()
  }

  @Transactional
  suspend fun deleteCorePersonMigration() {
    corePersonMigrationAddressContactMappingRepository.deleteAll()
  }

  @Transactional
  internal suspend fun replaceAddressesAndContactsMappings(
    prisonNumber: String,
    contacts: List<CorePersonContactMappingDto>,
    addresses: List<CorePersonAddressMappingDto>,
    addressUsages: List<CorePersonAddressUsageMappingDto>,
  ) {
    corePersonAddressMappingRepository.deleteAllByNomisPrisonNumber(prisonNumber)
    corePersonContactMappingRepository.deleteAllByNomisPrisonNumber(prisonNumber)
    corePersonAddressUsageMappingRepository.deleteAllByNomisPrisonNumber(prisonNumber)
    // In the event of a merge, it's possible that we need to delete dangling identifiers.
    addresses.forEach {
      corePersonAddressMappingRepository.deleteByNomisId(it.nomisId)
      corePersonAddressUsageMappingRepository.deleteById(it.cprId)
    }
    contacts.forEach {
      corePersonContactMappingRepository.deleteByNomisIdAndNomisContactType(it.nomisId, it.nomisContactType)
      corePersonContactMappingRepository.deleteById(it.cprId)
    }
    addressUsages.forEach {
      corePersonAddressUsageMappingRepository.deleteByNomisIdAndAddressUsageCode(it.nomisId, it.addressUsageCode)
      corePersonAddressUsageMappingRepository.deleteById(it.cprId)
    }
    // Save the new mappings
    addresses.forEach {
      corePersonAddressMappingRepository.save(it.toMapping())
    }
    contacts.forEach {
      corePersonContactMappingRepository.save(it.toMapping())
    }
    addressUsages.forEach {
      corePersonAddressUsageMappingRepository.save(it.toMapping())
    }
  }

  @Transactional
  suspend fun replaceMappings(mappings: CorePersonAddressContactMappingsDto) = replaceAddressesAndContactsMappings(mappings.personMapping.nomisPrisonNumber, mappings.contacts, mappings.addresses, mappings.addressUsages)

  @Transactional(readOnly = true)
  suspend fun getMappingResultFromNomisIds(mappingRequest: CorePersonMappingsByNomisIdsRequest): CorePersonMappingsResult {
    val matchingAddresses = corePersonAddressMappingRepository.findAllByNomisIdIn(mappingRequest.addresses).map { it.toDto() }
    val requestedContacts = mappingRequest.contacts
    val matchingContacts = corePersonContactMappingRepository.findAllByNomisIdIn(mappingRequest.contacts.map { it.nomisId })
      .filter { mapping -> requestedContacts.any { it.nomisId == mapping.nomisId && it.nomisContactType == mapping.nomisContactType } }
      .map { it.toDto() }
    val requestedAddressUsages = mappingRequest.addressUsages
    val matchingAddressUsages = corePersonAddressUsageMappingRepository.findAllByNomisIdIn(requestedAddressUsages.map { it.nomisId })
      .filter { mapping -> requestedAddressUsages.any { it.nomisId == mapping.nomisId && it.addressUsageCode == mapping.addressUsageCode } }
      .map { it.toDto() }
    return CorePersonMappingsResult(
      addresses = matchingAddresses,
      contacts = matchingContacts,
      addressUsages = matchingAddressUsages,
    )
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
