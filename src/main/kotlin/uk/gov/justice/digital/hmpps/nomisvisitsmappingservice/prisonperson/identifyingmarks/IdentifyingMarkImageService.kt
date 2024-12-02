package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.IdentifyingMarkImageMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.toDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException
import java.util.UUID

@Service
class IdentifyingMarkImageService(
  private val repository: IdentifyingMarkImageMappingRepository,
) {
  suspend fun getIdentifyingMarkImageMapping(nomisOffenderImageId: Long): IdentifyingMarkImageMappingDto =
    repository.findById(nomisOffenderImageId)
      ?.toDto()
      ?: throw NotFoundException("Identifying mark image mapping not found for NOMIS offender image id $nomisOffenderImageId")

  suspend fun getIdentifyingMarkImageMapping(dpsImageId: UUID): IdentifyingMarkImageMappingDto =
    repository.findByDpsId(dpsImageId)
      ?.toDto()
      ?: throw NotFoundException("Identifying mark image mapping not found for DPS image id $dpsImageId")

  suspend fun createIdentifyingMarkImageMapping(mapping: IdentifyingMarkImageMapping) {
    repository.save(mapping)
  }
}
