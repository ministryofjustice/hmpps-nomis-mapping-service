package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CSIPAttendeeMappingService(
  private val csipAttendeeMappingRepository: CSIPAttendeeMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  suspend fun createCSIPAttendeeMapping(createMappingRequest: CSIPAttendeeMappingDto) =
    with(createMappingRequest) {
      log.debug("creating csip attendee {}", createMappingRequest)

      csipAttendeeMappingRepository.save(
        CSIPAttendeeMapping(
          dpsCSIPAttendeeId = dpsCSIPAttendeeId,
          nomisCSIPAttendeeId = nomisCSIPAttendeeId,
          label = label,
          mappingType = mappingType,
        ),
      )
      telemetryClient.trackEvent(
        "csip-attendee-mapping-created",
        mapOf(
          "dpsCSIPAttendeeId" to dpsCSIPAttendeeId,
          "nomisCSIPAttendeeId" to nomisCSIPAttendeeId.toString(),
          "batchId" to label,
        ),
        null,
      )
      log.debug("Mapping created with dpsCSIPAttendeeId = $dpsCSIPAttendeeId, nomisCSIPAttendeeId=$nomisCSIPAttendeeId")
    }

  suspend fun getMappingByNomisId(nomisCSIPAttendeeId: Long): CSIPAttendeeMappingDto =
    csipAttendeeMappingRepository.findOneByNomisCSIPAttendeeId(
      nomisCSIPAttendeeId = nomisCSIPAttendeeId,
    )
      ?.toCSIPAttendeeDto()
      ?: throw NotFoundException("No CSIP Attendee mapping for  nomisCSIPAttendeeId=$nomisCSIPAttendeeId")

  suspend fun getMappingByDpsId(dpsCSIPAttendeeId: String): CSIPAttendeeMappingDto =
    csipAttendeeMappingRepository.findById(dpsCSIPAttendeeId)
      ?.toCSIPAttendeeDto()
      ?: throw NotFoundException("No CSIP attendee mapping found for dpsCSIPAttendeeId=$dpsCSIPAttendeeId")

  @Transactional
  suspend fun deleteMappingByDpsId(dpsCSIPAttendeeId: String) =
    csipAttendeeMappingRepository.deleteById(dpsCSIPAttendeeId)

  fun alreadyExistsMessage(
    duplicateMapping: CSIPAttendeeMappingDto,
    existingMapping: CSIPAttendeeMappingDto,
  ) =
    """CSIPAttendee Attendee mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
    """.trimMargin()
}

fun CSIPAttendeeMapping.toCSIPAttendeeDto() = CSIPAttendeeMappingDto(
  nomisCSIPAttendeeId = nomisCSIPAttendeeId,
  dpsCSIPAttendeeId = dpsCSIPAttendeeId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
