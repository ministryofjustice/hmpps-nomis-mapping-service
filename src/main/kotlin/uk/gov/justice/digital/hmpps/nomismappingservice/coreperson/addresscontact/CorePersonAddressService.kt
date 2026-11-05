package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact

import kotlinx.coroutines.flow.collect
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CorePersonAddressService(
  private val corePersonAddressMappingRepository: CorePersonAddressMappingRepository,
  private val corePersonAddressUsageMappingRepository: CorePersonAddressUsageMappingRepository,
) {
  suspend fun getAddressMappingByNomisId(nomisId: Long) = corePersonAddressMappingRepository.findOneByNomisId(nomisId = nomisId)
    ?.toDto()
    ?: throw NotFoundException("No core person address mapping found for nomisId=$nomisId")

  suspend fun getAddressMappingByCprId(cprId: String) = corePersonAddressMappingRepository.findOneByCprId(cprId = cprId)
    ?.toDto()
    ?: throw NotFoundException("No core person address mapping found for cprId=$cprId")

  suspend fun getAddressMappingByCprIdOrNull(cprId: String) = corePersonAddressMappingRepository.findOneByCprId(cprId = cprId)
    ?.toDto()

  @Transactional
  suspend fun deleteAddressMappingByNomisId(nomisId: Long) {
    corePersonAddressUsageMappingRepository.deleteAllByNomisId(nomisId)
    corePersonAddressMappingRepository.deleteByNomisId(nomisId)
  }

  @Transactional
  suspend fun createAddressMapping(mapping: CorePersonAddressMappingDto) {
    corePersonAddressMappingRepository.save(mapping.toMapping())
    mapping.addressUsageMappings?.takeIf { it.isNotEmpty() }?.let { mappings ->
      corePersonAddressUsageMappingRepository.saveAll(mappings.map { it.toMapping() }).collect()
    }
  }

  suspend fun getAddressUsageMappingByNomisId(nomisId: Long, addressUsageCode: String) = corePersonAddressUsageMappingRepository
    .findOneByNomisIdAndAddressUsageCode(nomisId, addressUsageCode)
    ?.toDto()
    ?: throw NotFoundException("No core person address usage mapping found for nomisId=$nomisId and addressUsageCode=$addressUsageCode")

  suspend fun getAddressUsageMappingByCprId(cprId: String) = corePersonAddressUsageMappingRepository.findOneByCprId(cprId)
    ?.toDto()
    ?: throw NotFoundException("No core person address usage mapping found for cprId=$cprId")

  suspend fun getAddressUsageMappingByCprIdOrNull(cprId: String) = corePersonAddressUsageMappingRepository.findOneByCprId(cprId)
    ?.toDto()

  @Transactional
  suspend fun deleteAddressUsageMappingByNomisId(nomisId: Long, addressUsageCode: String) = corePersonAddressUsageMappingRepository.deleteByNomisIdAndAddressUsageCode(nomisId, addressUsageCode)

  @Transactional
  suspend fun createAddressUsageMapping(mapping: CorePersonAddressUsageMappingDto) {
    corePersonAddressUsageMappingRepository.save(mapping.toMapping())
  }
}

internal fun CorePersonAddressMapping.toDto() = CorePersonAddressMappingDto(
  cprId = cprId,
  nomisId = nomisId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
  nomisPrisonNumber = nomisPrisonNumber,
)

internal fun CorePersonAddressMappingDto.toMapping() = CorePersonAddressMapping(
  cprId = cprId,
  nomisId = nomisId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
  nomisPrisonNumber = nomisPrisonNumber,
)

internal fun CorePersonAddressUsageMapping.toDto() = CorePersonAddressUsageMappingDto(
  cprId = cprId,
  nomisId = nomisId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
  addressUsageCode = addressUsageCode,
  nomisPrisonNumber = nomisPrisonNumber,
)

internal fun CorePersonAddressUsageMappingDto.toMapping() = CorePersonAddressUsageMapping(
  cprId = cprId,
  nomisId = nomisId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
  addressUsageCode = addressUsageCode,
  nomisPrisonNumber = nomisPrisonNumber,
)
