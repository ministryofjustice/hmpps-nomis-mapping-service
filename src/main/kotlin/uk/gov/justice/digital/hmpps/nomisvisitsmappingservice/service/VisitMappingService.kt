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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.CreateRoomMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.RoomMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.VisitMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.MappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.RoomId
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.VisitId
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.RoomIdRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.VisitIdRepository

@Service
class VisitMappingService(
  var visitIdRepository: VisitIdRepository,
  private val telemetryClient: TelemetryClient,
  private val roomIdRepository: RoomIdRepository,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  private fun alreadyExistsMessage(
    duplicateMapping: VisitMappingDto,
    existingMapping: VisitMappingDto,
  ) =
    "Visit mapping already exists. \nExisting mapping: $existingMapping\nDuplicate mapping: $duplicateMapping"

  @Transactional
  suspend fun createVisitMapping(createMappingRequest: VisitMappingDto) =
    with(createMappingRequest) {
      visitIdRepository.findById(nomisId)?.run {
        if (this@run.vsipId == this@with.vsipId) {
          log.debug("Visit mapping already exists for nomisId: $nomisId and vsipId: $vsipId so not creating. All OK")
          return
        }
        throw DuplicateMappingException(
          messageIn = alreadyExistsMessage(
            duplicateMapping = createMappingRequest,
            existingMapping = VisitMappingDto(this@run),
          ),
          duplicate = createMappingRequest,
          existing = VisitMappingDto(this@run),
        )
      }

      visitIdRepository.findOneByVsipId(vsipId)?.run {
        throw DuplicateMappingException(
          messageIn = alreadyExistsMessage(
            duplicateMapping = createMappingRequest,
            existingMapping = VisitMappingDto(this@run),
          ),
          duplicate = createMappingRequest,
          existing = VisitMappingDto(this),
        )
      }

      visitIdRepository.save(VisitId(nomisId, vsipId, label, MappingType.valueOf(mappingType)))
      telemetryClient.trackEvent(
        "visit-created",
        mapOf(
          "nomisVisitId" to nomisId.toString(),
          "vsipVisitId" to vsipId,
          "batchId" to label,
        ),
        null,
      )
      log.debug("Mapping created with VSIP visit id = $vsipId, Nomis visit id = $nomisId")
    }

  @Transactional(readOnly = true)
  suspend fun getVisitMappingGivenNomisId(nomisId: Long): VisitMappingDto =
    visitIdRepository.findById(nomisId)
      ?.let { VisitMappingDto(it) }
      ?: throw NotFoundException("NOMIS visit id=$nomisId")

  @Transactional(readOnly = true)
  suspend fun getVisitMappingGivenVsipId(vsipId: String): VisitMappingDto =
    visitIdRepository.findOneByVsipId(vsipId)
      ?.let { VisitMappingDto(it) }
      ?: throw NotFoundException("VSIP visit id=$vsipId")

  @Transactional(readOnly = true)
  suspend fun getRoomMapping(prisonId: String, nomisRoomDescription: String): RoomMappingDto =
    roomIdRepository.findOneByPrisonIdAndNomisRoomDescription(prisonId, nomisRoomDescription)
      ?.let { RoomMappingDto(it.vsipId, it.nomisRoomDescription, it.prisonId, it.isOpen) }
      ?: throw NotFoundException("prison id=$prisonId, nomis room id=$nomisRoomDescription")

  @Transactional
  suspend fun deleteVisitMappings(onlyMigrated: Boolean) =
    onlyMigrated.takeIf { it }?.apply {
      visitIdRepository.deleteByMappingTypeEquals(MappingType.MIGRATED)
    } ?: run {
      visitIdRepository.deleteAll()
    }

  @Transactional(readOnly = true)
  suspend fun getVisitMappingsByMigrationId(pageRequest: Pageable, migrationId: String): Page<VisitMappingDto> =
    coroutineScope {
      val visits = async {
        visitIdRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
          label = migrationId,
          MappingType.MIGRATED,
          pageRequest,
        )
      }

      val count = async {
        visitIdRepository.countAllByLabelAndMappingType(migrationId, mappingType = MappingType.MIGRATED)
      }

      PageImpl(
        visits.await().toList().map { VisitMappingDto(it) },
        pageRequest,
        count.await(),
      )
    }

  @Transactional(readOnly = true)
  suspend fun getVisitMappingForLatestMigrated(): VisitMappingDto =
    visitIdRepository.findFirstByMappingTypeOrderByWhenCreatedDesc(MappingType.MIGRATED)
      ?.let { VisitMappingDto(it) }
      ?: throw NotFoundException("No migrated mapping found")

  @Transactional(readOnly = true)
  suspend fun getRoomMappings(prisonId: String): List<RoomMappingDto> =
    roomIdRepository.findByPrisonIdOrderByNomisRoomDescription(prisonId).map {
      RoomMappingDto(it.vsipId, it.nomisRoomDescription, it.prisonId, it.isOpen)
    }

  @Transactional
  suspend fun createRoomMapping(prisonId: String, createRoomMappingDto: CreateRoomMappingDto) {
    roomIdRepository.findOneByPrisonIdAndNomisRoomDescription(prisonId, createRoomMappingDto.nomisRoomDescription)
      ?.run {
        throw ValidationException("Visit room mapping for prison $prisonId and nomis room = ${createRoomMappingDto.nomisRoomDescription} already exists")
      }

    roomIdRepository.save(
      RoomId(
        nomisRoomDescription = createRoomMappingDto.nomisRoomDescription,
        vsipId = createRoomMappingDto.vsipId,
        isOpen = createRoomMappingDto.isOpen,
        prisonId = prisonId,
      ),
    )
    telemetryClient.trackEvent(
      "visit-room-mapping-created",
      mapOf(
        "prisonId" to prisonId,
        "nomisRoomDescription" to createRoomMappingDto.nomisRoomDescription,
        "vsipRoomDescription" to createRoomMappingDto.vsipId,
        "isOpen" to createRoomMappingDto.isOpen.toString(),
      ),
      null,
    )
    log.debug(
      "Room Mapping created with VSIP visit description = ${createRoomMappingDto.vsipId}, Nomis room description = " +
        "${createRoomMappingDto.nomisRoomDescription}, open = ${createRoomMappingDto.isOpen}, prison = $prisonId",
    )
  }

  @Transactional
  suspend fun deleteRoomMapping(prisonId: String, nomisRoomDescription: String) {
    roomIdRepository.findOneByPrisonIdAndNomisRoomDescription(prisonId, nomisRoomDescription)?.run {
      roomIdRepository.deleteById(this.id)
      telemetryClient.trackEvent(
        "visit-room-mapping-deleted",
        mapOf(
          "prisonId" to prisonId,
          "nomisRoomDescription" to nomisRoomDescription,
        ),
        null,
      )
      log.debug(
        "Room Mapping deleted, Nomis room description = $nomisRoomDescription, prison = $prisonId",
      )
    }
  }

  @Transactional
  suspend fun deleteVisitMappingsByMigrationId(migrationId: String) =
    visitIdRepository.deleteByLabel(migrationId)
}
