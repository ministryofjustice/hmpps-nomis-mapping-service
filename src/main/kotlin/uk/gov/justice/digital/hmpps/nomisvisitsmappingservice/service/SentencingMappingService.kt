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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.SentencingAdjustmentMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.SentencingAdjustmentMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.SentencingMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.SentencingMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.SentenceAdjustmentMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.resource.DuplicateAdjustmentException

@Service
@Transactional(readOnly = true)
class SentencingMappingService(
  private val sentenceAdjustmentRepository: SentenceAdjustmentMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun alreadyExistsMessage(
    duplicateMapping: SentencingAdjustmentMappingDto,
    existingMapping: SentencingAdjustmentMappingDto
  ) =
    "Sentence adjustment mapping already exists. \nExisting mapping: $existingMapping\nDuplicate mapping: $duplicateMapping"

  @Transactional
  suspend fun createSentenceAdjustmentMapping(createMappingRequest: SentencingAdjustmentMappingDto) =
    with(createMappingRequest) {
      log.debug("creating sentence adjustment $createMappingRequest")
      sentenceAdjustmentRepository.findById(adjustmentId)?.run {
        if (this@run.nomisAdjustmentId == this@with.nomisAdjustmentId &&
          this@run.nomisAdjustmentCategory == this@with.nomisAdjustmentCategory
        ) {
          log.debug(
            "Not creating. All OK: " +
              alreadyExistsMessage(
                duplicateMapping = createMappingRequest, existingMapping = SentencingAdjustmentMappingDto(this@run)
              )
          )
          return
        }
        throw DuplicateAdjustmentException(
          messageIn = alreadyExistsMessage(
            duplicateMapping = createMappingRequest, existingMapping = SentencingAdjustmentMappingDto(this@run)
          ),
          duplicateMapping = createMappingRequest, existingMapping = SentencingAdjustmentMappingDto(this@run),
        )
      }

      sentenceAdjustmentRepository.findOneByNomisAdjustmentIdAndNomisAdjustmentCategory(
        nomisAdjustmentId = nomisAdjustmentId,
        nomisAdjustmentCategory = nomisAdjustmentCategory
      )?.run {
        throw DuplicateAdjustmentException(
          messageIn = alreadyExistsMessage(
            duplicateMapping = createMappingRequest, existingMapping = SentencingAdjustmentMappingDto(this@run)
          ),
          duplicateMapping = createMappingRequest,
          existingMapping = SentencingAdjustmentMappingDto(this)
        )
      }

      sentenceAdjustmentRepository.save(
        SentencingAdjustmentMapping(
          adjustmentId = adjustmentId,
          nomisAdjustmentId = nomisAdjustmentId,
          nomisAdjustmentCategory = nomisAdjustmentCategory,
          label = label,
          mappingType = SentencingMappingType.valueOf(mappingType)
        )
      )
      telemetryClient.trackEvent(
        "sentence-adjustment-mapping-created",
        mapOf(
          "sentenceAdjustmentId" to adjustmentId,
          "nomisAdjustmentId" to nomisAdjustmentId.toString(),
          "nomisAdjustmentCategory" to nomisAdjustmentCategory,
          "batchId" to label,
        ),
        null
      )
    }

  suspend fun getSentenceAdjustmentMappingByNomisId(
    nomisAdjustmentId: Long,
    nomisAdjustmentCategory: String
  ): SentencingAdjustmentMappingDto =
    sentenceAdjustmentRepository.findOneByNomisAdjustmentIdAndNomisAdjustmentCategory(
      nomisAdjustmentId = nomisAdjustmentId,
      nomisAdjustmentCategory = nomisAdjustmentCategory,
    )
      ?.let { SentencingAdjustmentMappingDto(it) }
      ?: throw NotFoundException("Sentence adjustment with nomisAdjustmentId = $nomisAdjustmentId  nomisAdjustmentCategory $nomisAdjustmentCategory not found")

  suspend fun getSentencingAdjustmentMappingByAdjustmentId(adjustmentId: String): SentencingAdjustmentMappingDto =
    sentenceAdjustmentRepository.findById(adjustmentId)
      ?.let { SentencingAdjustmentMappingDto(it) }
      ?: throw NotFoundException("Sentencing adjustmentId id=$adjustmentId")

  @Transactional
  suspend fun deleteSentenceAdjustmentMappings(onlyMigrated: Boolean) =
    onlyMigrated.takeIf { it }?.apply {
      sentenceAdjustmentRepository.deleteByMappingTypeEquals(MIGRATED)
    } ?: run {
      sentenceAdjustmentRepository.deleteAll()
    }

  suspend fun getSentenceAdjustmentMappingsByMigrationId(
    pageRequest: Pageable,
    migrationId: String
  ): Page<SentencingAdjustmentMappingDto> =
    coroutineScope {
      val sentenceAdjustmentMapping = async {
        sentenceAdjustmentRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
          label = migrationId,
          MIGRATED,
          pageRequest
        )
      }

      val count = async {
        sentenceAdjustmentRepository.countAllByLabelAndMappingType(migrationId, mappingType = MIGRATED)
      }

      PageImpl(
        sentenceAdjustmentMapping.await().toList().map { SentencingAdjustmentMappingDto(it) },
        pageRequest, count.await()
      )
    }

  suspend fun getSentencingAdjustmentMappingForLatestMigrated(): SentencingAdjustmentMappingDto =
    sentenceAdjustmentRepository.findFirstByMappingTypeOrderByWhenCreatedDesc(MIGRATED)
      ?.let { SentencingAdjustmentMappingDto(it) }
      ?: throw NotFoundException("No migrated mapping found")

  @Transactional
  suspend fun deleteSentencingAdjustmentMapping(adjustmentId: String) =
    sentenceAdjustmentRepository.deleteById(adjustmentId)
}
