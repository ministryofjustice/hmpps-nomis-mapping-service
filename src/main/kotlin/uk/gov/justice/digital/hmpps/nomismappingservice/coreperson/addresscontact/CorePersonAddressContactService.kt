package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact

import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonAddressContactMappingsDto
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingsByNomisIdsRequest
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMappingsResult
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonMergeMappingsDto

@Service
@Transactional(readOnly = true)
class CorePersonAddressContactService(
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
  suspend fun replaceMappings(mappings: CorePersonAddressContactMappingsDto) = replaceAddressesAndContactsMappings(
    mappings.personMapping.nomisPrisonNumber,
    mappings.contacts,
    mappings.addresses,
    mappings.addressUsages,
  )

  @Transactional
  suspend fun replaceAddressesAndContactsMappings(
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

  @Transactional
  suspend fun deleteAll() {
    corePersonContactMappingRepository.deleteAll()
    corePersonAddressMappingRepository.deleteAll()
    corePersonAddressUsageMappingRepository.deleteAll()
  }
}
