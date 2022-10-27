package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.IncentiveMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.IncentiveMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.IncentiveMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.MappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.IncentiveMappingRepository
import javax.validation.ValidationException

@Service
@Transactional(readOnly = true)
class IncentiveMappingService(
  private val incentiveMappingRepository: IncentiveMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  suspend fun createIncentiveMapping(createMappingRequest: IncentiveMappingDto) =
    with(createMappingRequest) {
      log.debug("creating incentive $createMappingRequest")
      incentiveMappingRepository.findById(incentiveId)?.run {
        if (this@run.nomisBookingId == this@with.nomisBookingId && this@run.nomisIncentiveSequence == this@with.nomisIncentiveSequence) {
          log.debug("Incentive mapping already exists for nomisBookingId: $nomisBookingId and nomisIncentiveSequence: $nomisIncentiveSequence: $incentiveId so not creating. All OK")
          return
        }
        throw ValidationException("Incentive mapping id = $incentiveId already exists")
      }

      incentiveMappingRepository.findOneByNomisBookingIdAndNomisIncentiveSequence(
        bookingId = nomisBookingId,
        incentiveSequence = nomisIncentiveSequence
      )?.run {
        throw ValidationException("Incentive with bookingId=$nomisBookingId and incentiveSequence=$nomisIncentiveSequence already exists")
      }

      incentiveMappingRepository.save(
        IncentiveMapping(
          incentiveId = incentiveId,
          nomisBookingId = nomisBookingId,
          nomisIncentiveSequence = nomisIncentiveSequence,
          label = label,
          mappingType = IncentiveMappingType.valueOf(mappingType)
        )
      )
      telemetryClient.trackEvent(
        "incentive-mapping-created",
        mapOf(
          "nomisBookingId" to nomisBookingId.toString(),
          "nomisIncentiveSequence" to nomisIncentiveSequence.toString(),
          "incentiveId" to incentiveId.toString(),
          "batchId" to label,
        ),
        null
      )
      log.debug("Mapping created with Incentive id = $incentiveId, bookingId=$nomisBookingId and incentiveSequence=$nomisIncentiveSequence")
    }

  suspend fun getIncentiveMappingByNomisId(nomisBookingId: Long, nomisIncentiveSequence: Long): IncentiveMappingDto =
    incentiveMappingRepository.findOneByNomisBookingIdAndNomisIncentiveSequence(
      bookingId = nomisBookingId,
      incentiveSequence = nomisIncentiveSequence
    )
      ?.let { IncentiveMappingDto(it) }
      ?: throw NotFoundException("Incentive with bookingId=$nomisBookingId and incentiveSequence=$nomisIncentiveSequence not found")

  suspend fun getIncentiveMappingByIncentiveId(incentiveId: Long): IncentiveMappingDto =
    incentiveMappingRepository.findById(incentiveId)
      ?.let { IncentiveMappingDto(it) }
      ?: throw NotFoundException("Incentive id=$incentiveId")

  @Transactional
  suspend fun deleteIncentiveMappings(onlyMigrated: Boolean) =
    onlyMigrated.takeIf { it }?.apply {
      incentiveMappingRepository.deleteByMappingTypeEquals(MappingType.MIGRATED)
    } ?: run {
      incentiveMappingRepository.deleteAll()
    }

  suspend fun getIncentiveMappingsByMigrationId(pageRequest: Pageable, migrationId: String): Page<IncentiveMappingDto> =
    coroutineScope {
      val incentiveMapping = async {
        incentiveMappingRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
          label = migrationId,
          MappingType.MIGRATED,
          pageRequest
        )
      }

      val count = async {
        incentiveMappingRepository.countAllByLabelAndMappingType(migrationId, mappingType = MappingType.MIGRATED)
      }

      PageImpl(
        incentiveMapping.await().toList().map { IncentiveMappingDto(it) },
        pageRequest, count.await()
      )
    }

  suspend fun getIncentiveMappingForLatestMigrated(): IncentiveMappingDto =
    incentiveMappingRepository.findFirstByMappingTypeOrderByWhenCreatedDesc(MappingType.MIGRATED)
      ?.let { IncentiveMappingDto(it) }
      ?: throw NotFoundException("No migrated mapping found")

  suspend fun deleteIncentiveMapping(incentiveId: Long) = incentiveMappingRepository.deleteById(incentiveId)
}
