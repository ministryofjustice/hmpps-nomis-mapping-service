package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service

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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.SentenceAdjustmentMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.SentenceAdjustmentMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.SentencingMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.SentencingMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.SentenceAdjustmentMappingRepository

@Service
@Transactional(readOnly = true)
class SentencingMappingService(
  private val sentenceAdjustmentRepository: SentenceAdjustmentMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun alreadyExistsMessage(nomisId: Long, nomisType: String, id: Long) =
    "Sentence adjustment mapping nomisAdjustmentId = $nomisId with nomisAdjustmentType = $nomisType and sentenceAdjustmentId = $id already exists"

  @Transactional
  suspend fun createSentenceAdjustmentMapping(createMappingRequest: SentenceAdjustmentMappingDto) =
    with(createMappingRequest) {
      log.debug("creating sentence adjustment $createMappingRequest")
      sentenceAdjustmentRepository.findById(sentenceAdjustmentId)?.run {
        if (this@run.nomisAdjustmentId == this@with.nomisAdjustmentId &&
          this@run.nomisAdjustmentType == this@with.nomisAdjustmentType
        ) {
          log.debug(
            alreadyExistsMessage(
              nomisAdjustmentId,
              nomisAdjustmentType,
              sentenceAdjustmentId
            ) + "so not creating. All OK"
          )
          return
        }

        throw ValidationException(alreadyExistsMessage(nomisAdjustmentId, nomisAdjustmentType, sentenceAdjustmentId))
      }

      sentenceAdjustmentRepository.findOneByNomisAdjustmentIdAndNomisAdjustmentType(
        nomisAdjustmentId = nomisAdjustmentId,
        nomisAdjustmentType = nomisAdjustmentType
      )?.run {
        throw ValidationException(
          alreadyExistsMessage(
            this@with.nomisAdjustmentId,
            this@with.nomisAdjustmentType,
            sentenceAdjustmentId
          )
        )
      }

      sentenceAdjustmentRepository.save(
        SentenceAdjustmentMapping(
          sentenceAdjustmentId = sentenceAdjustmentId,
          nomisAdjustmentId = nomisAdjustmentId,
          nomisAdjustmentType = nomisAdjustmentType,
          label = label,
          mappingType = SentencingMappingType.valueOf(mappingType)
        )
      )
      telemetryClient.trackEvent(
        "sentence-adjustment-mapping-created",
        mapOf(
          "sentenceAdjustmentId" to sentenceAdjustmentId.toString(),
          "nomisAdjustmentId" to nomisAdjustmentId.toString(),
          "nomisAdjustmentType" to nomisAdjustmentType,
          "batchId" to label,
        ),
        null
      )
    }

  suspend fun getSentenceAdjustmentMappingByNomisId(
    nomisAdjustmentId: Long,
    nomisAdjustmentType: String
  ): SentenceAdjustmentMappingDto =
    sentenceAdjustmentRepository.findOneByNomisAdjustmentIdAndNomisAdjustmentType(
      nomisAdjustmentId = nomisAdjustmentId,
      nomisAdjustmentType = nomisAdjustmentType,
    )
      ?.let { SentenceAdjustmentMappingDto(it) }
      ?: throw NotFoundException("Sentence adjustment with nomisAdjustmentId = $nomisAdjustmentId  nomisAdjustmentType $nomisAdjustmentType not found")

  suspend fun getSentenceAdjustmentMappingBySentencingId(sentenceAdjustmentId: Long): SentenceAdjustmentMappingDto =
    sentenceAdjustmentRepository.findById(sentenceAdjustmentId)
      ?.let { SentenceAdjustmentMappingDto(it) }
      ?: throw NotFoundException("Sentencing sentenceAdjustmentId id=$sentenceAdjustmentId")

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
  ): Page<SentenceAdjustmentMappingDto> =
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
        sentenceAdjustmentMapping.await().toList().map { SentenceAdjustmentMappingDto(it) },
        pageRequest, count.await()
      )
    }

  suspend fun getSentenceAdjustmentMappingForLatestMigrated(): SentenceAdjustmentMappingDto =
    sentenceAdjustmentRepository.findFirstByMappingTypeOrderByWhenCreatedDesc(MIGRATED)
      ?.let { SentenceAdjustmentMappingDto(it) }
      ?: throw NotFoundException("No migrated mapping found")

  @Transactional
  suspend fun deleteSentenceAdjustmentMapping(sentenceAdjustmentId: Long) =
    sentenceAdjustmentRepository.deleteById(sentenceAdjustmentId)
}
