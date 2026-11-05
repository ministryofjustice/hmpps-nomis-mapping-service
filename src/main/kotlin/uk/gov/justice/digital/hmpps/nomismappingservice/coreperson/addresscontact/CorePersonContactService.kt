package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.addresscontact

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.CorePersonContactMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CorePersonContactService(
  private val corePersonContactMappingRepository: CorePersonContactMappingRepository,
) {
  suspend fun getContactMappingByNomisId(nomisId: Long, nomisContactType: NomisContactType) = corePersonContactMappingRepository
    .findOneByNomisIdAndNomisContactType(nomisId = nomisId, nomisContactType = nomisContactType)
    ?.toDto()
    ?: throw NotFoundException("No core person contact mapping found for nomisId=$nomisId and nomisContactType=$nomisContactType")

  suspend fun getContactMappingByCprId(cprId: String) = corePersonContactMappingRepository.findOneByCprId(cprId = cprId)
    ?.toDto()
    ?: throw NotFoundException("No core person contact mapping found for cprId=$cprId")

  suspend fun getContactMappingByCprIdOrNull(cprId: String) = corePersonContactMappingRepository.findOneByCprId(cprId = cprId)
    ?.toDto()

  @Transactional
  suspend fun deleteContactMappingByNomisId(nomisId: Long, nomisContactType: NomisContactType) = corePersonContactMappingRepository
    .deleteByNomisIdAndNomisContactType(nomisId = nomisId, nomisContactType = nomisContactType)

  @Transactional
  suspend fun createContactMapping(mapping: CorePersonContactMappingDto) {
    corePersonContactMappingRepository.save(mapping.toMapping())
  }
}

internal fun CorePersonContactMapping.toDto() = CorePersonContactMappingDto(
  nomisId = nomisId,
  nomisContactType = nomisContactType,
  cprId = cprId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
  nomisPrisonNumber = nomisPrisonNumber,
)

internal fun CorePersonContactMappingDto.toMapping() = CorePersonContactMapping(
  nomisId = nomisId,
  nomisContactType = nomisContactType,
  cprId = cprId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
  nomisPrisonNumber = nomisPrisonNumber,
)
