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
import uk.gov.justice.digital.hmpps.nomismappingservice.data.AppointmentMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.AppointmentMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.AppointmentMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.repository.AppointmentMappingRepository

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
  ) = """Appointment mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
  """.trimMargin()

  @Transactional
  suspend fun createMapping(createMappingRequest: AppointmentMappingDto) = with(createMappingRequest) {
    log.debug("creating appointment {}", createMappingRequest)
    appointmentMappingRepository.findById(appointmentInstanceId)?.run {
      if (this@run.nomisEventId == this@with.nomisEventId) {
        log.debug(
          "Not creating. All OK: {}",
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

  suspend fun getMappingById(id: Long): AppointmentMappingDto = appointmentMappingRepository.findById(id)
    ?.let { AppointmentMappingDto(it) }
    ?: throw NotFoundException("appointmentInstanceId=$id")

  suspend fun getMappingByEventId(eventId: Long): AppointmentMappingDto = appointmentMappingRepository.findOneByNomisEventId(eventId)
    ?.let { AppointmentMappingDto(it) }
    ?: throw NotFoundException("eventId=$eventId")

  @Transactional
  suspend fun deleteMapping(id: Long) = appointmentMappingRepository.deleteById(id)

  suspend fun getAppointmentMappingsByMigrationId(
    pageRequest: Pageable,
    migrationId: String,
  ): Page<AppointmentMappingDto> = coroutineScope {
    val appointmentMapping = async {
      appointmentMappingRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
        label = migrationId,
        AppointmentMappingType.MIGRATED,
        pageRequest,
      )
    }

    val count = async {
      appointmentMappingRepository.countAllByLabelAndMappingType(migrationId, mappingType = AppointmentMappingType.MIGRATED)
    }

    PageImpl(
      appointmentMapping.await().toList().map { AppointmentMappingDto(it) },
      pageRequest,
      count.await(),
    )
  }

  suspend fun getAppointmentMappingForLatestMigrated(): AppointmentMappingDto = appointmentMappingRepository.findFirstByMappingTypeOrderByWhenCreatedDesc(AppointmentMappingType.MIGRATED)
    ?.let { AppointmentMappingDto(it) }
    ?: throw NotFoundException("No migrated mapping found")

  @Transactional
  suspend fun deleteAppointmentMappingsByMigrationId(migrationId: String) {
    appointmentMappingRepository.deleteByLabel(migrationId)
  }
}
