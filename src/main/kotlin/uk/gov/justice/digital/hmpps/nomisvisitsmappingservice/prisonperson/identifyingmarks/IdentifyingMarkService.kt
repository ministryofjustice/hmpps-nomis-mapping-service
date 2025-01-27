package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.IdentifyingMarkMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.toDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks.api.toEntity
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException
import java.util.UUID

@Service
@Transactional(readOnly = true)
class IdentifyingMarkService(
  private val repository: IdentifyingMarkMappingRepository,
) {
  suspend fun getIdentifyingMarkMapping(bookingId: Long, markSequence: Long): IdentifyingMarkMappingDto = repository.findByNomisBookingIdAndNomisMarksSequence(bookingId, markSequence)
    ?.toDto()
    ?: throw NotFoundException("Identifying mark mapping not found for booking id $bookingId and sequence $markSequence")

  suspend fun getIdentifyingMarkMappings(dpsId: UUID): List<IdentifyingMarkMappingDto> = repository.findByDpsId(dpsId)
    .map { it.toDto() }

  @Transactional
  suspend fun createIdentifyingMarkMapping(mapping: IdentifyingMarkMappingDto) {
    repository.save(mapping.toEntity())
  }
}
