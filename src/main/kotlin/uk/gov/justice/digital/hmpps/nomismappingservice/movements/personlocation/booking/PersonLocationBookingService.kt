package uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.booking

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException

@Service
class PersonLocationBookingService(
  private val bookingRepository: PersonLocationBookingRepository,
) {

  suspend fun getBookingMappingByNomisId(nomisBookingId: Long) = bookingRepository.findByNomisBookingId(nomisBookingId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for NOMIS booking id $nomisBookingId not found")
}

fun PersonLocationBookingMapping.toMappingDto() = PersonLocationBookingIdMappingDto(
  nomisBookingId = nomisBookingId,
  dpsCustodialSeriesId = dpsCustodialSeriesId,
  mappingType = mappingType,
)
