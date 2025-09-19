package uk.gov.justice.digital.hmpps.nomismappingservice.service

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
import uk.gov.justice.digital.hmpps.nomismappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomismappingservice.data.IncentiveMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.IncentiveMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.IncentiveMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.IncentiveMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.repository.IncentiveMappingRepository

@Service
@Transactional(readOnly = true)
class IncentiveMappingService(
  private val incentiveMappingRepository: IncentiveMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun alreadyExistsMessage(
    duplicateMapping: IncentiveMappingDto,
    existingMapping: IncentiveMappingDto,
  ) = """Incentive mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
  """.trimMargin()

  @Transactional
  suspend fun createIncentiveMapping(createMappingRequest: IncentiveMappingDto) = with(createMappingRequest) {
    log.debug("creating incentive $createMappingRequest")
    incentiveMappingRepository.findById(incentiveId)?.run {
      if (this@run.nomisBookingId == this@with.nomisBookingId &&
        this@run.nomisIncentiveSequence == this@with.nomisIncentiveSequence
      ) {
        log.debug(
          "Not creating. All OK: {}",
          alreadyExistsMessage(
            duplicateMapping = createMappingRequest,
            existingMapping = IncentiveMappingDto(this@run),
          ),
        )
        return
      }
      throw DuplicateMappingException(
        messageIn = alreadyExistsMessage(
          duplicateMapping = createMappingRequest,
          existingMapping = IncentiveMappingDto(this@run),
        ),
        duplicate = createMappingRequest,
        existing = IncentiveMappingDto(this@run),
      )
    }

    incentiveMappingRepository.findOneByNomisBookingIdAndNomisIncentiveSequence(
      bookingId = nomisBookingId,
      incentiveSequence = nomisIncentiveSequence,
    )?.run {
      throw DuplicateMappingException(
        messageIn = alreadyExistsMessage(
          duplicateMapping = createMappingRequest,
          existingMapping = IncentiveMappingDto(this@run),
        ),
        duplicate = createMappingRequest,
        existing = IncentiveMappingDto(this),
      )
    }

    incentiveMappingRepository.save(
      IncentiveMapping(
        incentiveId = incentiveId,
        nomisBookingId = nomisBookingId,
        nomisIncentiveSequence = nomisIncentiveSequence,
        label = label,
        mappingType = IncentiveMappingType.valueOf(mappingType),
      ),
    )
    telemetryClient.trackEvent(
      "incentive-mapping-created",
      mapOf(
        "nomisBookingId" to nomisBookingId.toString(),
        "nomisIncentiveSequence" to nomisIncentiveSequence.toString(),
        "incentiveId" to incentiveId.toString(),
        "batchId" to label,
      ),
      null,
    )
    log.debug("Mapping created with Incentive id = $incentiveId, bookingId=$nomisBookingId and incentiveSequence=$nomisIncentiveSequence")
  }

  suspend fun getIncentiveMappingByNomisId(nomisBookingId: Long, nomisIncentiveSequence: Long): IncentiveMappingDto = incentiveMappingRepository.findOneByNomisBookingIdAndNomisIncentiveSequence(
    bookingId = nomisBookingId,
    incentiveSequence = nomisIncentiveSequence,
  )
    ?.let { IncentiveMappingDto(it) }
    ?: throw NotFoundException("Incentive with bookingId=$nomisBookingId and incentiveSequence=$nomisIncentiveSequence not found")

  suspend fun getIncentiveMappingByIncentiveId(incentiveId: Long): IncentiveMappingDto = incentiveMappingRepository.findById(incentiveId)
    ?.let { IncentiveMappingDto(it) }
    ?: throw NotFoundException("Incentive id=$incentiveId")

  @Transactional
  suspend fun deleteIncentiveMappings(onlyMigrated: Boolean) = onlyMigrated.takeIf { it }?.apply {
    incentiveMappingRepository.deleteByMappingTypeEquals(MIGRATED)
  } ?: run {
    incentiveMappingRepository.deleteAll()
  }

  suspend fun getIncentiveMappingsByMigrationId(pageRequest: Pageable, migrationId: String): Page<IncentiveMappingDto> = coroutineScope {
    val incentiveMapping = async {
      incentiveMappingRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
        label = migrationId,
        MIGRATED,
        pageRequest,
      )
    }

    val count = async {
      incentiveMappingRepository.countAllByLabelAndMappingType(migrationId, mappingType = MIGRATED)
    }

    PageImpl(
      incentiveMapping.await().toList().map { IncentiveMappingDto(it) },
      pageRequest,
      count.await(),
    )
  }

  suspend fun getIncentiveMappingForLatestMigrated(): IncentiveMappingDto = incentiveMappingRepository.findFirstByMappingTypeOrderByWhenCreatedDesc(MIGRATED)
    ?.let { IncentiveMappingDto(it) }
    ?: throw NotFoundException("No migrated mapping found")

  @Transactional
  suspend fun deleteIncentiveMapping(incentiveId: Long) = incentiveMappingRepository.deleteById(incentiveId)
}
