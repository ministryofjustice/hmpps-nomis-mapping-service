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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AdjudicationMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AdjudicationMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AdjudicationMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.AdjudicationMappingRepository

@Service
@Transactional(readOnly = true)
class AdjudicationMappingService(
  private val adjudicationMappingRepository: AdjudicationMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  suspend fun createMapping(createMappingRequest: AdjudicationMappingDto) =
    with(createMappingRequest) {
      log.debug("creating adjudication {}", createMappingRequest)

      adjudicationMappingRepository.findById(chargeNumber)
        ?.let {
          throw DuplicateMappingException(
            existing = chargeNumber,
            duplicate = chargeNumber,
            messageIn = "Adjudication mapping with id $chargeNumber already exists",
          )
        }
        ?: adjudicationMappingRepository.save(
          AdjudicationMapping(
            adjudicationNumber = adjudicationNumber,
            chargeSequence = chargeSequence,
            chargeNumber = chargeNumber,
            label = label,
            mappingType = AdjudicationMappingType.valueOf(mappingType ?: "ADJUDICATION_CREATED"),
          ),
        )

      telemetryClient.trackEvent(
        "adjudication-mapping-created",
        mapOf(
          "adjudicationNumber" to adjudicationNumber.toString(),
        ),
        null,
      )
      log.debug("Mapping created with adjudicationNumber = $adjudicationNumber")
    }

  suspend fun getMappingByDpsId(chargeNumber: String): AdjudicationMappingDto =
    adjudicationMappingRepository.findById(chargeNumber)
      ?.let { AdjudicationMappingDto(it) }
      ?: throw NotFoundException("chargeNumber=$chargeNumber")

  suspend fun getMappingByNomisId(adjudicationNumber: Long, chargeSequence: Int): AdjudicationMappingDto =
    adjudicationMappingRepository.findByAdjudicationNumberAndChargeSequence(adjudicationNumber, chargeSequence)
      ?.let { AdjudicationMappingDto(it) }
      ?: throw NotFoundException("adjudicationNumber=$adjudicationNumber, chargeSequence=$chargeSequence")

  @Transactional
  suspend fun deleteMapping(chargeNumber: String) = adjudicationMappingRepository.deleteById(chargeNumber)

  suspend fun getAdjudicationMappingsByMigrationId(
    pageRequest: Pageable,
    migrationId: String,
  ): Page<AdjudicationMappingDto> =
    coroutineScope {
      val adjudicationMapping = async {
        adjudicationMappingRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
          label = migrationId,
          AdjudicationMappingType.MIGRATED,
          pageRequest,
        )
      }

      val count = async {
        adjudicationMappingRepository.countAllByLabelAndMappingType(
          migrationId,
          mappingType = AdjudicationMappingType.MIGRATED,
        )
      }

      PageImpl(
        adjudicationMapping.await().toList().map { AdjudicationMappingDto(it) },
        pageRequest,
        count.await(),
      )
    }

  suspend fun getAdjudicationMappingForLatestMigrated(): AdjudicationMappingDto =
    adjudicationMappingRepository.findFirstByMappingTypeOrderByWhenCreatedDesc(AdjudicationMappingType.MIGRATED)
      ?.let { AdjudicationMappingDto(it) }
      ?: throw NotFoundException("No migrated mapping found")

  suspend fun getAllMappings(): List<AdjudicationMappingDto> =
    adjudicationMappingRepository.findAll().toList().map { AdjudicationMappingDto(it) }
}
