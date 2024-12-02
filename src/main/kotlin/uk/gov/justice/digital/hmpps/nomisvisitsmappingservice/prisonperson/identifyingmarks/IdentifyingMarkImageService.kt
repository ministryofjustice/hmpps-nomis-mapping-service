package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks

import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.IdentifyingMarkImageMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.toDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.toEntity
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

  suspend fun createIdentifyingMarkImageMapping(mapping: IdentifyingMarkImageMappingDto) {
    try {
      repository.save(mapping.toEntity())
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "Identifying mark image mapping already exists",
        duplicate = mapping,
        existing = getExistingMappingSimilarTo(mapping),
        cause = e,
      )
    }
  }

  private suspend fun getExistingMappingSimilarTo(mapping: IdentifyingMarkImageMappingDto) =
    runCatching {
      getIdentifyingMarkImageMapping(mapping.nomisOffenderImageId)
    }.getOrElse {
      getIdentifyingMarkImageMapping(mapping.dpsId)
    }
}
