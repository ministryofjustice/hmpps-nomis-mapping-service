package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts.AlertMappingType.MIGRATED
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

  suspend fun getByMigrationId(pageRequest: Pageable, migrationId: String): Page<AlertMappingDto> = coroutineScope {
    val mappings = async {
      repository.findAllByLabelAndMappingTypeOrderByLabelDesc(
        label = migrationId,
        mappingType = MIGRATED,
        pageRequest = pageRequest,
      )
    }

    val count = async {
      repository.countAllByLabelAndMappingType(
        migrationId = migrationId,
        mappingType = MIGRATED,
      )
    }

    PageImpl(
      mappings.await().toList().map { it.toDto() },
      pageRequest,
      count.await(),
    )
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
