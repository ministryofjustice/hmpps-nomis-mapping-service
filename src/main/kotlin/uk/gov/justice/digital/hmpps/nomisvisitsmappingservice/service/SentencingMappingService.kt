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

  fun alreadyExistsMessage(nomisId: Long, id: Long) =
    "Sentence adjustment mapping nomisSentenceAdjustmentId = $nomisId and sentenceAdjustmentId = $id already exists"

  @Transactional
  suspend fun createSentenceAdjustmentMapping(createMappingRequest: SentenceAdjustmentMappingDto) =
    with(createMappingRequest) {
      log.debug("creating sentence adjustment $createMappingRequest")
      sentenceAdjustmentRepository.findById(sentenceAdjustmentId)?.run {
        if (this@run.nomisSentenceAdjustmentId == this@with.nomisSentenceAdjustmentId) {
          log.debug(alreadyExistsMessage(nomisSentenceAdjustmentId, sentenceAdjustmentId) + "so not creating. All OK")
          return
        }

        throw ValidationException(alreadyExistsMessage(nomisSentenceAdjustmentId, sentenceAdjustmentId))
      }

      sentenceAdjustmentRepository.findOneByNomisSentenceAdjustmentId(
        nomisSentenceAdjustmentId = nomisSentenceAdjustmentId
      )?.run {
        throw ValidationException(alreadyExistsMessage(nomisSentenceAdjustmentId, sentenceAdjustmentId))
      }

      sentenceAdjustmentRepository.save(
        SentenceAdjustmentMapping(
          sentenceAdjustmentId = sentenceAdjustmentId,
          nomisSentenceAdjustmentId = nomisSentenceAdjustmentId,
          label = label,
          mappingType = SentencingMappingType.valueOf(mappingType)
        )
      )
      telemetryClient.trackEvent(
        "sentence-adjustment-mapping-created",
        mapOf(
          "sentenceAdjustmentId" to sentenceAdjustmentId.toString(),
          "nomisSentenceAdjustmentId" to nomisSentenceAdjustmentId.toString(),
          "batchId" to label,
        ),
        null
      )
      log.debug("Mapping created with nomisSentenceAdjustmentId=$nomisSentenceAdjustmentId and sentenceAdjustmentId = $sentenceAdjustmentId ")
    }

  suspend fun getSentenceAdjustmentMappingByNomisId(nomisSentenceAdjustmentId: Long): SentenceAdjustmentMappingDto =
    sentenceAdjustmentRepository.findOneByNomisSentenceAdjustmentId(
      nomisSentenceAdjustmentId = nomisSentenceAdjustmentId
    )
      ?.let { SentenceAdjustmentMappingDto(it) }
      ?: throw NotFoundException("Sentence adjustment with nomisSentenceAdjustmentId = $nomisSentenceAdjustmentId not found")

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

  suspend fun getSentenceAdjustmentMappingsByMigrationId(pageRequest: Pageable, migrationId: String): Page<SentenceAdjustmentMappingDto> =
    coroutineScope {
      val incentiveMapping = async {
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
        incentiveMapping.await().toList().map { SentenceAdjustmentMappingDto(it) },
        pageRequest, count.await()
      )
    }

  suspend fun getSentenceAdjustmentMappingForLatestMigrated(): SentenceAdjustmentMappingDto =
    sentenceAdjustmentRepository.findFirstByMappingTypeOrderByWhenCreatedDesc(MIGRATED)
      ?.let { SentenceAdjustmentMappingDto(it) }
      ?: throw NotFoundException("No migrated mapping found")

  suspend fun deleteSentenceAdjustmentMapping(sentenceAdjustmentId: Long) = sentenceAdjustmentRepository.deleteById(sentenceAdjustmentId)
}
