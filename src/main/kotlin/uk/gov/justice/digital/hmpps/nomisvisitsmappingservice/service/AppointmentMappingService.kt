package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.AppointmentMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AppointmentMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AppointmentMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.AppointmentMappingRepository

@Service
@Transactional(readOnly = true)
class AppointmentMappingService(
  private val appointmentMappingRepository: AppointmentMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun alreadyExistsMessage(
    duplicateMapping: AppointmentMappingDto,
    existingMapping: AppointmentMappingDto,
  ) =
    "Appointment mapping already exists. \nExisting mapping: $existingMapping\nDuplicate mapping: $duplicateMapping"

  @Transactional
  suspend fun createMapping(createMappingRequest: AppointmentMappingDto) =
    with(createMappingRequest) {
      log.debug("creating appointment {}", createMappingRequest)
      appointmentMappingRepository.findById(appointmentInstanceId)?.run {
        if (this@run.nomisEventId == this@with.nomisEventId) {
          log.debug(
            "Not creating. All OK: " +
              alreadyExistsMessage(
                duplicateMapping = createMappingRequest,
                existingMapping = AppointmentMappingDto(this@run),
              ),
          )
          return
        }
        throw DuplicateMappingException(
          messageIn = alreadyExistsMessage(
            duplicateMapping = createMappingRequest,
            existingMapping = AppointmentMappingDto(this@run),
          ),
          duplicate = createMappingRequest,
          existing = AppointmentMappingDto(this@run),
        )
      }

      appointmentMappingRepository.findOneByNomisEventId(nomisEventId)?.run {
        throw DuplicateMappingException(
          messageIn = alreadyExistsMessage(
            duplicateMapping = createMappingRequest,
            existingMapping = AppointmentMappingDto(this@run),
          ),
          duplicate = createMappingRequest,
          existing = AppointmentMappingDto(this),
        )
      }

      appointmentMappingRepository.save(
        AppointmentMapping(
          appointmentInstanceId = appointmentInstanceId,
          nomisEventId = nomisEventId,
          label = label,
          mappingType = AppointmentMappingType.valueOf(mappingType ?: "APPOINTMENT_CREATED"),
        ),
      )
      telemetryClient.trackEvent(
        "appointment-mapping-created",
        mapOf(
          "nomisEventId" to nomisEventId.toString(),
          "appointmentInstanceId" to appointmentInstanceId.toString(),
        ),
        null,
      )
      log.debug("Mapping created with appointmentInstanceId = $appointmentInstanceId, nomisEventId = $nomisEventId")
    }

  suspend fun getMappingById(id: Long): AppointmentMappingDto =
    appointmentMappingRepository.findById(id)
      ?.let { AppointmentMappingDto(it) }
      ?: throw NotFoundException("appointmentInstanceId=$id")

  suspend fun getMappingByEventId(eventId: Long): AppointmentMappingDto =
    appointmentMappingRepository.findOneByNomisEventId(eventId)
      ?.let { AppointmentMappingDto(it) }
      ?: throw NotFoundException("eventId=$eventId")

  @Transactional
  suspend fun deleteMapping(id: Long) = appointmentMappingRepository.deleteById(id)

  suspend fun getAllMappings(): List<AppointmentMappingDto> =
    appointmentMappingRepository.findAll().toList().map { AppointmentMappingDto(it) }
}
