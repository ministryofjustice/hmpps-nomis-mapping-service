package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.MappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.RoomMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.MappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.VisitId
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.RoomIdRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.VisitIdRepository
import javax.validation.ValidationException

@Service
@Transactional(readOnly = true)
class MappingService(
  private val visitIdRepository: VisitIdRepository,
  private val telemetryClient: TelemetryClient,
  private val roomIdRepository: RoomIdRepository,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun createVisitMapping(createMappingRequest: MappingDto) =
    with(createMappingRequest) {
      visitIdRepository.findByIdOrNull(nomisId)?.run {
        throw ValidationException("Nomis visit id = $nomisId already exists")
      }

      visitIdRepository.findOneByVsipId(vsipId)?.run {
        throw ValidationException("VSIP visit id=$vsipId already exists")
      }

      visitIdRepository.save(VisitId(nomisId, vsipId, label, MappingType.valueOf(mappingType)))
      telemetryClient.trackEvent(
        "visit-created",
        mapOf(
          "nomisVisitId" to nomisId.toString(),
          "vsipVisitId" to vsipId,
          "batchId" to label,
        ),
        null
      )
      log.debug("Mapping created with VSIP visit id = $vsipId, Nomis visit id = $nomisId")
    }

  fun getVisitMappingGivenNomisId(nomisId: Long): MappingDto =
    visitIdRepository.findByIdOrNull(nomisId)
      ?.let { MappingDto(it) }
      ?: throw NotFoundException("NOMIS visit id=$nomisId")

  fun getVisitMappingGivenVsipId(vsipId: String): MappingDto =
    visitIdRepository.findOneByVsipId(vsipId)
      ?.let { MappingDto(it) }
      ?: throw NotFoundException("VSIP visit id=$vsipId")

  fun getRoomMapping(prisonId: String, nomisRoomDescription: String): RoomMappingDto =
    roomIdRepository.findOneByPrisonIdAndNomisRoomDescription(prisonId, nomisRoomDescription)
      ?.let { RoomMappingDto(it.vsipId, it.nomisRoomDescription, it.prisonId, it.isOpen) }
      ?: throw NotFoundException("prison id=$prisonId, nomis room id=$nomisRoomDescription")

  @Transactional
  fun deleteVisitMappings(onlyMigrated: Boolean) =
    onlyMigrated.takeIf { it }?.apply {
      visitIdRepository.deleteByMappingType(MappingType.MIGRATED)
    } ?: run {
      visitIdRepository.deleteAll()
    }

  fun getVisitMappingsByMigrationId(pageRequest: Pageable, migrationId: String): Page<MappingDto> {
    val visits = visitIdRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
      label = migrationId,
      MappingType.MIGRATED,
      pageRequest
    )

    val count = visitIdRepository.countAllByLabelAndMappingType(migrationId, mappingType = MappingType.MIGRATED)

    return PageImpl(
      visits.map { MappingDto(it) },
      pageRequest, count
    )
  }

  fun getVisitMappingForLatestMigrated(): MappingDto =
    visitIdRepository.findFirstByMappingTypeOrderByWhenCreatedDesc(MappingType.MIGRATED)
      ?.let { MappingDto(it) }
      ?: throw NotFoundException("No migrated mapping found")
}

class NotFoundException(message: String) : RuntimeException(message)
