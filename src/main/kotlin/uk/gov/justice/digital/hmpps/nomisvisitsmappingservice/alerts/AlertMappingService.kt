package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
class AlertMappingService(val repository: AlertsMappingRepository) {
  suspend fun getMappingByNomisId(bookingId: Long, alertSequence: Long): AlertMappingDto {
    return repository.findOneByNomisBookingIdAndNomisAlertSequence(bookingId = bookingId, alertSequence = alertSequence)
      ?.let {
        AlertMappingDto(
          nomisBookingId = it.nomisBookingId,
          nomisAlertSequence = it.nomisAlertSequence,
          dpsAlertId = it.dpsAlertId,
          label = it.label,
          mappingType = it.mappingType,
          whenCreated = it.whenCreated,
        )
      } ?: throw NotFoundException("No alert mapping found for bookingId=$bookingId,alertSequence=$alertSequence")
  }
}
