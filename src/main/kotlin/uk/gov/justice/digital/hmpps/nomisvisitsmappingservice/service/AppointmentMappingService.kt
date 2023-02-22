package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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

  @Transactional
  suspend fun createMapping(createMappingRequest: AppointmentMappingDto) =
    with(createMappingRequest) {
      log.debug("creating appointment $createMappingRequest")
      appointmentMappingRepository.findById(appointmentInstanceId)?.run {
        if (this@run.nomisEventId == this@with.nomisEventId) {
          log.debug("Appointment mapping already exists for nomisBookingId: $nomisEventId : $appointmentInstanceId so not creating. All OK")
          return
        }
        throw ValidationException("Appointment mapping id = $appointmentInstanceId already exists")
      }

      appointmentMappingRepository.findOneByNomisEventId(
        nomisEventId = nomisEventId,
      )?.run {
        throw ValidationException("Appointment with Nomis id=$nomisEventId already exists")
      }

      appointmentMappingRepository.save(
        AppointmentMapping(
          appointmentInstanceId = appointmentInstanceId,
          nomisEventId = nomisEventId,
          mappingType = AppointmentMappingType.valueOf(mappingType)
        )
      )
      telemetryClient.trackEvent(
        "appointment-mapping-created",
        mapOf(
          "nomisEventId" to nomisEventId.toString(),
          "appointmentInstanceId" to appointmentInstanceId.toString(),
        ),
        null
      )
      log.debug("Mapping created with appointmentInstanceId = $appointmentInstanceId, nomisEventId = $nomisEventId")
    }

  suspend fun getMappingById(id: Long): AppointmentMappingDto =
    appointmentMappingRepository.findById(id)
      ?.let { AppointmentMappingDto(it) }
      ?: throw NotFoundException("appointmentInstanceId=$id")

  @Transactional
  suspend fun deleteMapping(id: Long) = appointmentMappingRepository.deleteById(id)
}
