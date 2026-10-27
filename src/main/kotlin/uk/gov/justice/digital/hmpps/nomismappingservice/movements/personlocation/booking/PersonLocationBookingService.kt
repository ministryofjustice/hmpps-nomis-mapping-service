package uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.booking

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException
import java.util.UUID

@Service
class PersonLocationBookingService(
  private val bookingRepository: PersonLocationBookingRepository,
) {

  @Transactional
  suspend fun createBookingMapping(mappingDto: PersonLocationBookingIdMappingDto) {
    bookingRepository.save(mappingDto.toMapping())
  }

  suspend fun getBookingMappingByNomisId(nomisBookingId: Long) = bookingRepository.findByNomisBookingId(nomisBookingId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for NOMIS booking id $nomisBookingId not found")

  @Transactional
  suspend fun deleteBookingMappingByNomisId(nomisBookingId: Long) = bookingRepository.deleteByNomisBookingId(nomisBookingId)

  suspend fun getBookingMappingByDpsId(dpsCustodialSeriesId: UUID) = bookingRepository.findById(dpsCustodialSeriesId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for DPS custodial series id $dpsCustodialSeriesId not found")

  suspend fun getExistingBookingMappingSimilarTo(mapping: PersonLocationBookingIdMappingDto) = runCatching {
    getBookingMappingByNomisId(mapping.nomisBookingId)
  }.getOrElse {
    getBookingMappingByDpsId(mapping.dpsCustodialSeriesId)
  }
}

fun PersonLocationBookingIdMappingDto.toMapping() = PersonLocationBookingMapping(
  dpsCustodialSeriesId = dpsCustodialSeriesId,
  nomisBookingId = nomisBookingId,
  mappingType = mappingType,
)

fun PersonLocationBookingMapping.toMappingDto() = PersonLocationBookingIdMappingDto(
  nomisBookingId = nomisBookingId,
  dpsCustodialSeriesId = dpsCustodialSeriesId,
  mappingType = mappingType,
)
