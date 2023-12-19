package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.IncidentMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.IncidentMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.IncidentMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.IncidentMappingRepository

@Service
@Transactional(readOnly = true)
class IncidentMappingService(
  private val incidentMappingRepository: IncidentMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun alreadyExistsMessage(
    duplicateMapping: IncidentMappingDto,
    existingMapping: IncidentMappingDto,
  ) =
    "Incident mapping already exists. \nExisting mapping: $existingMapping\nDuplicate mapping: $duplicateMapping"

  @Transactional
  suspend fun createIncidentMapping(createMappingRequest: IncidentMappingDto) =
    with(createMappingRequest) {
      log.debug("creating incident $createMappingRequest")
      incidentMappingRepository.findById(incidentId)?.run {
        if (this@run.nomisIncidentId == this@with.nomisIncidentId
        ) {
          log.debug(
            "Not creating. All OK: " +
              alreadyExistsMessage(
                duplicateMapping = createMappingRequest,
                existingMapping = IncidentMappingDto(this@run),
              ),
          )
          return
        }
        throw DuplicateMappingException(
          messageIn = alreadyExistsMessage(
            duplicateMapping = createMappingRequest,
            existingMapping = IncidentMappingDto(this@run),
          ),
          duplicate = createMappingRequest,
          existing = IncidentMappingDto(this@run),
        )
      }

      incidentMappingRepository.findOneByNomisIncidentId(
        nomisIncidentId = nomisIncidentId,
      )?.run {
        throw DuplicateMappingException(
          messageIn = alreadyExistsMessage(
            duplicateMapping = createMappingRequest,
            existingMapping = IncidentMappingDto(this@run),
          ),
          duplicate = createMappingRequest,
          existing = IncidentMappingDto(this),
        )
      }

      incidentMappingRepository.save(
        IncidentMapping(
          incidentId = incidentId,
          nomisIncidentId = nomisIncidentId,
          label = label,
          mappingType = IncidentMappingType.valueOf(mappingType),
        ),
      )
      telemetryClient.trackEvent(
        "incident-mapping-created",
        mapOf(
          "incidentId" to incidentId,
          "nomisIncidentId" to nomisIncidentId.toString(),
          "batchId" to label,
        ),
        null,
      )
      log.debug("Mapping created with incidentId = $incidentId, nomisIncidentId=$nomisIncidentId")
    }

  suspend fun getIncidentMappingByNomisId(nomisIncidentId: Long): IncidentMappingDto =
    incidentMappingRepository.findOneByNomisIncidentId(
      nomisIncidentId = nomisIncidentId,
    )
      ?.let { IncidentMappingDto(it) }
      ?: throw NotFoundException("Incident with nomisIncidentId=$nomisIncidentId not found")

  suspend fun getIncidentMappingByIncidentId(incidentId: String): IncidentMappingDto =
    incidentMappingRepository.findById(incidentId)
      ?.let { IncidentMappingDto(it) }
      ?: throw NotFoundException("incidentId=$incidentId")
}
