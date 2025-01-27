package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.IdentifyingMarkImageMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.toDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.toEntity
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException
import java.util.UUID

@Service
@Transactional(readOnly = true)
class IdentifyingMarkImageService(
  private val repository: IdentifyingMarkImageMappingRepository,
) {
  suspend fun getIdentifyingMarkImageMapping(nomisOffenderImageId: Long): IdentifyingMarkImageMappingDto = repository.findById(nomisOffenderImageId)
    ?.toDto()
    ?: throw NotFoundException("Identifying mark image mapping not found for NOMIS offender image id $nomisOffenderImageId")

  suspend fun getIdentifyingMarkImageMapping(dpsImageId: UUID): IdentifyingMarkImageMappingDto = repository.findByDpsId(dpsImageId)
    ?.toDto()
    ?: throw NotFoundException("Identifying mark image mapping not found for DPS image id $dpsImageId")

  @Transactional
  suspend fun createIdentifyingMarkImageMapping(mapping: IdentifyingMarkImageMappingDto) {
    repository.save(mapping.toEntity())
  }
}
