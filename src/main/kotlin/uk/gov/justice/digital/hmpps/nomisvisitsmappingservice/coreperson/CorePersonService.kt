package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.coreperson

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CorePersonService(
  private val corePersonMappingRepository: CorePersonMappingRepository,
) {
  @Transactional
  suspend fun createMappings(mappings: CorePersonMappingsDto) {
    with(mappings) {
      corePersonMappingRepository.save(toCorePersonMapping())
    }
  }

  suspend fun getCorePersonMappingByPrisonNumber(prisonNumber: String) =
    corePersonMappingRepository.findOneByPrisonNumber(prisonNumber = prisonNumber)
      ?.toDto()
      ?: throw NotFoundException("No person mapping found for prisonNumber=$prisonNumber")

  suspend fun getCorePersonMappingByCprIdOrNull(cprId: String) =
    corePersonMappingRepository.findOneByCprId(cprId = cprId)
      ?.toDto()
}

private fun CorePersonMappingsDto.toCorePersonMapping() = CorePersonMapping(
  cprId = personMapping.cprId,
  prisonNumber = personMapping.prisonNumber,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private fun CorePersonMapping.toDto() = CorePersonMappingDto(
  cprId = cprId,
  prisonNumber = prisonNumber,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
