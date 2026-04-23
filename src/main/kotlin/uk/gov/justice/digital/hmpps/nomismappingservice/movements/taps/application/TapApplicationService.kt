package uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException
import java.util.UUID

@Service
class TapApplicationService(
  private val applicationRepository: TapApplicationRepository,
) {

  @Transactional
  suspend fun createApplicationMapping(mappingDto: TapApplicationMappingDto) {
    applicationRepository.save(mappingDto.toMapping())
  }

  suspend fun getApplicationMappingByNomisId(nomisApplicationId: Long) = applicationRepository.findByNomisApplicationId(nomisApplicationId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for NOMIS application id $nomisApplicationId not found")

  suspend fun getApplicationMappingByDpsId(dpsAuthorisationId: UUID) = applicationRepository.findById(dpsAuthorisationId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for DPS application id $dpsAuthorisationId not found")

  @Transactional
  suspend fun deleteApplicationMappingByNomisId(nomisApplicationId: Long) = applicationRepository.deleteByNomisApplicationId(nomisApplicationId)
}

fun TapApplicationMappingDto.toMapping(): TapApplicationMapping = TapApplicationMapping(
  dpsAuthorisationId,
  nomisApplicationId,
  prisonerNumber,
  bookingId,
  mappingType = mappingType,
)

fun TapApplicationMapping.toMappingDto(): TapApplicationMappingDto = TapApplicationMappingDto(
  offenderNo,
  bookingId,
  nomisApplicationId,
  dpsAuthorisationId,
  mappingType = mappingType,
)
