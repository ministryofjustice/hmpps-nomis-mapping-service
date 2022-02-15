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
  suspend fun createVisitMapping(createMappingRequest: MappingDto) =
    with(createMappingRequest) {
      visitIdRepository.findById(nomisId)?.run {
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

  suspend fun getVisitMappingGivenNomisId(nomisId: Long): MappingDto =
    visitIdRepository.findById(nomisId)
      ?.let { MappingDto(nomisId, it.vsipId, it.label, it.mappingType.name) }
      ?: throw NotFoundException("NOMIS visit id=$nomisId")

  suspend fun getVisitMappingGivenVsipId(vsipId: String): MappingDto =
    visitIdRepository.findOneByVsipId(vsipId)
      ?.let { MappingDto(it.nomisId, vsipId, it.label, it.mappingType.name) }
      ?: throw NotFoundException("VSIP visit id=$vsipId")

  suspend fun getRoomMapping(prisonId: String, nomisRoomDescription: String): RoomMappingDto =
    roomIdRepository.findOneByPrisonIdAndNomisRoomDescription(prisonId, nomisRoomDescription)
      ?.let { RoomMappingDto(it.vsipId, it.nomisRoomDescription, it.prisonId, it.isOpen) }
      ?: throw NotFoundException("prison id=$prisonId, nomis room id=$nomisRoomDescription")

  suspend fun deleteVisitMappings() = visitIdRepository.deleteAll()

  suspend fun getVisitMappingsByMigrationId(pageRequest: Pageable, migrationId: String): Page<MappingDto> =
    coroutineScope {
      val visits = async {
        visitIdRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
          label = migrationId,
          MappingType.MIGRATED,
          pageRequest
        )
      }

      val count = async {
        visitIdRepository.countAllByLabelAndMappingType(migrationId, mappingType = MappingType.MIGRATED)
      }

      PageImpl(
        visits.await().toList().map { mapping ->
          MappingDto(
            mapping.nomisId,
            mapping.vsipId,
            mapping.label,
            mapping.mappingType.name
          )
        },
        pageRequest, count.await()
      )
    }
}

class NotFoundException(message: String) : RuntimeException(message)
