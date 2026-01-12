package uk.gov.justice.digital.hmpps.nomismappingservice.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.validation.ValidationException
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
import uk.gov.justice.digital.hmpps.nomismappingservice.data.NonAssociationMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.NonAssociationMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.NonAssociationMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.NonAssociationMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.repository.NonAssociationMappingRepository

@Service
@Transactional(readOnly = true)
class NonAssociationMappingService(
  private val nonAssociationMappingRepository: NonAssociationMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun alreadyExistsMessage(
    duplicateMapping: NonAssociationMappingDto,
    existingMapping: NonAssociationMappingDto,
  ) = """Non-association mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
  """.trimMargin()

  @Transactional
  suspend fun createNonAssociationMapping(createMappingRequest: NonAssociationMappingDto) = with(createMappingRequest) {
    log.debug("creating nonAssociation {}", createMappingRequest)
    nonAssociationMappingRepository.findById(nonAssociationId)?.run {
      if (this@run.firstOffenderNo == this@with.firstOffenderNo &&
        this@run.secondOffenderNo == this@with.secondOffenderNo &&
        this@run.nomisTypeSequence == this@with.nomisTypeSequence
      ) {
        log.debug(
          "Not creating. All OK: {}",
          alreadyExistsMessage(
            duplicateMapping = createMappingRequest,
            existingMapping = NonAssociationMappingDto(this@run),
          ),
        )
        return
      }
      throw DuplicateMappingException(
        messageIn = alreadyExistsMessage(
          duplicateMapping = createMappingRequest,
          existingMapping = NonAssociationMappingDto(this@run),
        ),
        duplicate = createMappingRequest,
        existing = NonAssociationMappingDto(this@run),
      )
    }

    nonAssociationMappingRepository.findOneByFirstOffenderNoAndSecondOffenderNoAndNomisTypeSequence(
      firstOffenderNo = firstOffenderNo,
      secondOffenderNo = secondOffenderNo,
      nomisTypeSequence = nomisTypeSequence,
    )?.run {
      throw DuplicateMappingException(
        messageIn = alreadyExistsMessage(
          duplicateMapping = createMappingRequest,
          existingMapping = NonAssociationMappingDto(this@run),
        ),
        duplicate = createMappingRequest,
        existing = NonAssociationMappingDto(this),
      )
    }

    nonAssociationMappingRepository.save(
      NonAssociationMapping(
        nonAssociationId = nonAssociationId,
        firstOffenderNo = firstOffenderNo,
        secondOffenderNo = secondOffenderNo,
        nomisTypeSequence = nomisTypeSequence,
        label = label,
        mappingType = NonAssociationMappingType.valueOf(mappingType),
      ),
    )
    telemetryClient.trackEvent(
      "nonAssociation-mapping-created",
      mapOf(
        "nonAssociationId" to nonAssociationId.toString(),
        "firstOffenderNo" to firstOffenderNo,
        "secondOffenderNo" to secondOffenderNo,
        "nomisTypeSequence" to nomisTypeSequence.toString(),
        "batchId" to label,
      ),
      null,
    )
    log.debug("Mapping created with nonAssociationId = $nonAssociationId, firstOffenderNo=$firstOffenderNo, secondOffenderNo=$secondOffenderNo, and nomisTypeSequence=$nomisTypeSequence")
  }

  suspend fun getNonAssociationMappingByNomisId(
    firstOffenderNo: String,
    secondOffenderNo: String,
    nomisTypeSequence: Int,
  ): NonAssociationMappingDto = nonAssociationMappingRepository.findOneByFirstOffenderNoAndSecondOffenderNoAndNomisTypeSequence(
    firstOffenderNo = firstOffenderNo,
    secondOffenderNo = secondOffenderNo,
    nomisTypeSequence = nomisTypeSequence,
  )
    ?.let { NonAssociationMappingDto(it) }
    ?: throw NotFoundException("Non-association with firstOffenderNo=$firstOffenderNo, secondOffenderNo=$secondOffenderNo, and nomisTypeSequence=$nomisTypeSequence not found")

  suspend fun getNonAssociationMappingsOfMerge(
    oldOffenderNo: String,
    newOffenderNo: String,
  ): List<String> = nonAssociationMappingRepository
    .findCommonThirdParties(
      oldOffenderNo,
      newOffenderNo,
    ).map { if (it.firstOffenderNo == oldOffenderNo || it.firstOffenderNo == newOffenderNo) it.secondOffenderNo else it.firstOffenderNo }

  suspend fun getNonAssociationMappingByNonAssociationId(nonAssociationId: Long): NonAssociationMappingDto = nonAssociationMappingRepository.findById(nonAssociationId)
    ?.let { NonAssociationMappingDto(it) }
    ?: throw NotFoundException("nonAssociationId=$nonAssociationId")

  suspend fun getNonAssociationMappingsByMigrationId(
    pageRequest: Pageable,
    migrationId: String,
  ): Page<NonAssociationMappingDto> = coroutineScope {
    val nonAssociationMapping = async {
      nonAssociationMappingRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
        label = migrationId,
        MIGRATED,
        pageRequest,
      )
    }

    val count = async {
      nonAssociationMappingRepository.countAllByLabelAndMappingType(migrationId, mappingType = MIGRATED)
    }

    PageImpl(
      nonAssociationMapping.await().toList().map { NonAssociationMappingDto(it) },
      pageRequest,
      count.await(),
    )
  }

  suspend fun getNonAssociationMappingForLatestMigrated(): NonAssociationMappingDto = nonAssociationMappingRepository.findFirstByMappingTypeOrderByWhenCreatedDesc(MIGRATED)
    ?.let { NonAssociationMappingDto(it) }
    ?: throw NotFoundException("No migrated mapping found")

  @Transactional
  suspend fun deleteNonAssociationMapping(nonAssociationId: Long) = nonAssociationMappingRepository.deleteById(nonAssociationId)

  @Transactional
  suspend fun updateMappingsByNomisId(oldOffenderNo: String, newOffenderNo: String) {
    nonAssociationMappingRepository.findByFirstOffenderNoOrSecondOffenderNo(oldOffenderNo, oldOffenderNo)
      .forEach {
        val telemetryMap = mapOf(
          "originalMapping" to it.toString(),
          "oldOffenderNo" to oldOffenderNo,
          "newOffenderNo" to newOffenderNo,
        )
        if (oldOffenderNo == it.firstOffenderNo) {
          if (newOffenderNo == it.secondOffenderNo) {
            throw ValidationException("Found NA clash in $it when updating offender id from $oldOffenderNo to $newOffenderNo")
          }
          nonAssociationMappingRepository.updateFirstOffenderNo(it.nonAssociationId, newOffenderNo)
          telemetryClient.trackEvent("nonAssociation-mapping-merged", telemetryMap, null)
        } else if (oldOffenderNo == it.secondOffenderNo) {
          if (it.firstOffenderNo == newOffenderNo) {
            throw ValidationException("Found NA clash in $it when updating offender id from $oldOffenderNo to $newOffenderNo")
          }
          nonAssociationMappingRepository.updateSecondOffenderNo(it.nonAssociationId, newOffenderNo)
          telemetryClient.trackEvent("nonAssociation-mapping-merged", telemetryMap, null)
        }
      }
  }

  @Transactional
  suspend fun updateMappingsInList(
    oldOffenderNo: String,
    newOffenderNo: String,
    nonAssociations: List<String>,
  ) {
    nonAssociations.forEach {
      if (it == oldOffenderNo) {
        throw ValidationException("Old offenderNo is in the list, when updating offender id from $oldOffenderNo to $newOffenderNo")
      }
      if (it == newOffenderNo) {
        throw ValidationException("New offenderNo is in the list, when updating offender id from $oldOffenderNo to $newOffenderNo")
      }

      val rows1 = nonAssociationMappingRepository.updateFirstOffenderNoByOffenderNos(
        firstOffenderNo = oldOffenderNo,
        secondOffenderNo = it,
        newOffenderNo = newOffenderNo,
      )
      val rows2 = nonAssociationMappingRepository.updateSecondOffenderNoByOffenderNos(
        firstOffenderNo = it,
        secondOffenderNo = oldOffenderNo,
        newOffenderNo = newOffenderNo,
      )

      telemetryClient.trackEvent(
        "nonAssociation-mapping-booking-moved",
        mapOf(
          "listEntry" to it,
          "oldOffenderNo" to oldOffenderNo,
          "newOffenderNo" to newOffenderNo,
          "updatedRows1" to rows1.toString(),
          "updatedRows2" to rows2.toString(),
        ),
        null,
      )
    }
  }
}
