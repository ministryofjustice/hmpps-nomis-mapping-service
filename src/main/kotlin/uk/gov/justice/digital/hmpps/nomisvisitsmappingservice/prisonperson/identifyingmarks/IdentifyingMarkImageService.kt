package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class IdentifyingMarkImageService(
  private val repository: IdentifyingMarkImageMappingRepository,
) {
  suspend fun getIdentifyingMarkImageMapping(nomisOffenderImageId: Long): IdentifyingMarkImageMapping? =
    repository.findById(nomisOffenderImageId)

  suspend fun getIdentifyingMarkImageMapping(dpsImageId: UUID): IdentifyingMarkImageMapping? =
    repository.findByDpsId(dpsImageId)

  suspend fun createIdentifyingMarkImageMapping(mapping: IdentifyingMarkImageMapping) {
    repository.save(mapping)
  }
}
