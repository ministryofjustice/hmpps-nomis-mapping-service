package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
class AlertMappingService(val repository: AlertsMappingRepository) {
  suspend fun getMappingByNomisId(bookingId: Long, alertSequence: Long) =
    repository.findOneByNomisBookingIdAndNomisAlertSequence(bookingId = bookingId, alertSequence = alertSequence)
      ?.toDto()
      ?: throw NotFoundException("No alert mapping found for bookingId=$bookingId,alertSequence=$alertSequence")

  suspend fun getMappingByDpsId(alertId: String) =
    repository.findOneByDpsAlertId(dpsAlertId = alertId)
      ?.toDto()
      ?: throw NotFoundException("No alert mapping found for dpsAlertId=$alertId")

  suspend fun deleteMappingByDpsId(alertId: String) =
    repository.deleteById(alertId)

  suspend fun createMapping(mapping: AlertMappingDto) {
    repository.save(mapping.fromDto())
  }

  suspend fun deleteAllMappings() {
    repository.deleteAll()
  }
}

fun AlertMapping.toDto() = AlertMappingDto(
  nomisBookingId = nomisBookingId,
  nomisAlertSequence = nomisAlertSequence,
  dpsAlertId = dpsAlertId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

fun AlertMappingDto.fromDto() = AlertMapping(
  dpsAlertId = dpsAlertId,
  nomisBookingId = nomisBookingId,
  nomisAlertSequence = nomisAlertSequence,
  label = label,
  mappingType = mappingType,
)
