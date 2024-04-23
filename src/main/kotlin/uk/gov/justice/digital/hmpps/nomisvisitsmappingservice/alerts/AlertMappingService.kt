package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.alerts.AlertMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class AlertMappingService(
  val repository: AlertsMappingRepository,
  val prisonerMappingRepository: AlertPrisonerMappingRepository,
) {
  suspend fun getMappingByNomisId(bookingId: Long, alertSequence: Long) =
    repository.findOneByNomisBookingIdAndNomisAlertSequence(bookingId = bookingId, alertSequence = alertSequence)
      ?.toDto()
      ?: throw NotFoundException("No alert mapping found for bookingId=$bookingId,alertSequence=$alertSequence")

  suspend fun getMappingByDpsId(alertId: String) =
    repository.findOneByDpsAlertId(dpsAlertId = alertId)
      ?.toDto()
      ?: throw NotFoundException("No alert mapping found for dpsAlertId=$alertId")

  @Transactional
  suspend fun deleteMappingByDpsId(alertId: String) =
    repository.deleteById(alertId)

  @Transactional
  suspend fun createMapping(mapping: AlertMappingDto) {
    repository.save(mapping.fromDto())
  }

  @Transactional
  suspend fun createMappings(offenderNo: String, prisonerMapping: PrisonerAlertMappingsDto) {
    // since we are replacing all alerts remove old mappings so they they can all be recreated
    repository.deleteAllByOffenderNo(offenderNo)
    repository.saveAll(
      prisonerMapping.mappings.map {
        AlertMapping(
          dpsAlertId = it.dpsAlertId,
          nomisBookingId = it.nomisBookingId,
          nomisAlertSequence = it.nomisAlertSequence,
          offenderNo = offenderNo,
          label = prisonerMapping.label,
          mappingType = prisonerMapping.mappingType,
        )
      },
    ).collect()
    prisonerMappingRepository.save(
      AlertPrisonerMapping(
        offenderNo = offenderNo,
        count = prisonerMapping.mappings.size,
        mappingType = prisonerMapping.mappingType,
        label = prisonerMapping.label,
      ),
    )
  }

  @Transactional
  suspend fun deleteAllMappings() {
    repository.deleteAll()
    prisonerMappingRepository.deleteAll()
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

  suspend fun getByMigrationIdGroupedByPrisoner(
    pageRequest: Pageable,
    migrationId: String,
  ): Page<PrisonerAlertMappingsSummaryDto> = coroutineScope {
    val mappings = async {
      prisonerMappingRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
        label = migrationId,
        mappingType = MIGRATED,
        pageRequest = pageRequest,
      )
    }

    val count = async {
      prisonerMappingRepository.countAllByLabelAndMappingType(
        migrationId = migrationId,
        mappingType = MIGRATED,
      )
    }

    PageImpl(
      mappings.await().toList()
        .map {
          PrisonerAlertMappingsSummaryDto(
            offenderNo = it.offenderNo,
            mappingsCount = it.count,
            whenCreated = it.whenCreated,
          )
        },
      pageRequest,
      count.await(),
    )
  }

  @Transactional
  suspend fun updateMappingByNomisId(previousBookingId: Long, alertSequence: Long, newBookingId: Long) =
    repository.findOneByNomisBookingIdAndNomisAlertSequence(
      bookingId = previousBookingId,
      alertSequence = alertSequence,
    )
      ?.let {
        repository.save(it.copy(nomisBookingId = newBookingId)).toDto()
      }
      ?: throw NotFoundException("No alert mapping found for bookingId=$previousBookingId,alertSequence=$alertSequence")
}

fun AlertMapping.toDto() = AlertMappingDto(
  nomisBookingId = nomisBookingId,
  nomisAlertSequence = nomisAlertSequence,
  dpsAlertId = dpsAlertId,
  offenderNo = offenderNo,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

fun AlertMappingDto.fromDto() = AlertMapping(
  dpsAlertId = dpsAlertId,
  nomisBookingId = nomisBookingId,
  nomisAlertSequence = nomisAlertSequence,
  offenderNo = offenderNo,
  label = label,
  mappingType = mappingType,
)
