package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class IdentifyingMarkService(
  private val identifyingMarkMappingRepository: IdentifyingMarkMappingRepository,
) {
  suspend fun getIdentifyingMarkMapping(bookingId: Long, markSequence: Long): IdentifyingMarkMapping? =
    identifyingMarkMappingRepository.findByNomisBookingIdAndNomisMarksSequence(bookingId, markSequence)

  suspend fun getIdentifyingMarkMappings(dpsId: UUID): List<IdentifyingMarkMapping> =
    identifyingMarkMappingRepository.findByDpsId(dpsId)
}
