package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks

import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.IdentifyingMarkMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.toDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.toEntity
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException
import java.util.UUID

@Service
class IdentifyingMarkService(
  private val repository: IdentifyingMarkMappingRepository,
) {
  suspend fun getIdentifyingMarkMapping(bookingId: Long, markSequence: Long): IdentifyingMarkMappingDto =
    repository.findByNomisBookingIdAndNomisMarksSequence(bookingId, markSequence)
      ?.toDto()
      ?: throw NotFoundException("Identifying mark mapping not found for booking id $bookingId and sequence $markSequence")

  suspend fun getIdentifyingMarkMappings(dpsId: UUID): List<IdentifyingMarkMappingDto> =
    repository.findByDpsId(dpsId)
      .map { it.toDto() }

  suspend fun createIdentifyingMarkMapping(mapping: IdentifyingMarkMappingDto) {
    try {
      repository.save(mapping.toEntity())
    } catch (e: DuplicateKeyException) {
      throw DuplicateMappingException(
        messageIn = "Identifying mark mapping already exists",
        duplicate = mapping,
        existing = getExistingMappingSimilarTo(mapping),
        cause = e,
      )
    }
  }

  private suspend fun getExistingMappingSimilarTo(mapping: IdentifyingMarkMappingDto) =
    getIdentifyingMarkMapping(mapping.nomisBookingId, mapping.nomisMarksSequence)
}
