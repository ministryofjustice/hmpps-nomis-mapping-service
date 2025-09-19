package uk.gov.justice.digital.hmpps.nomismappingservice.incidents

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
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException

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
  ) = """Incident mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
  """.trimMargin()

  @Transactional
  suspend fun createIncidentMapping(createMappingRequest: IncidentMappingDto) = with(createMappingRequest) {
    log.debug("creating incident {}", createMappingRequest)
    incidentMappingRepository.findById(dpsIncidentId)?.run {
      if (this@run.nomisIncidentId == this@with.nomisIncidentId) {
        log.debug(
          "Not creating. All OK: {}",
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
        dpsIncidentId = dpsIncidentId,
        nomisIncidentId = nomisIncidentId,
        label = label,
        mappingType = IncidentMappingType.valueOf(mappingType),
      ),
    )
    telemetryClient.trackEvent(
      "incident-mapping-created",
      mapOf(
        "dpsIncidentId" to dpsIncidentId,
        "nomisIncidentId" to nomisIncidentId.toString(),
        "batchId" to label,
      ),
      null,
    )
    log.debug("Mapping created with dpsIncidentId = $dpsIncidentId, nomisIncidentId=$nomisIncidentId")
  }

  suspend fun getMappingByNomisId(nomisIncidentId: Long): IncidentMappingDto = incidentMappingRepository.findOneByNomisIncidentId(
    nomisIncidentId = nomisIncidentId,
  )
    ?.let { IncidentMappingDto(it) }
    ?: throw NotFoundException("Incident with nomisIncidentId=$nomisIncidentId not found")

  suspend fun getMappingByDPSId(dpsIncidentId: String): IncidentMappingDto = incidentMappingRepository.findById(dpsIncidentId)
    ?.let { IncidentMappingDto(it) }
    ?: throw NotFoundException("dpsIncidentId=$dpsIncidentId")

  suspend fun getMappingsByNomisId(nomisIncidentIds: List<Long>): List<IncidentMappingDto> = incidentMappingRepository.findByNomisIncidentIdIn(nomisIncidentIds = nomisIncidentIds).takeIf { nomisIncidentIds.size == it.size }
    ?.map { IncidentMappingDto(it) }
    ?: throw NotFoundException("Could not find all incident mappings for $nomisIncidentIds")

  @Transactional
  suspend fun deleteMappingByDPSId(dpsIncidentId: String) = incidentMappingRepository.deleteById(dpsIncidentId)

  @Transactional
  suspend fun deleteMappings(onlyMigrated: Boolean) = onlyMigrated.takeIf { it }?.apply {
    incidentMappingRepository.deleteByMappingTypeEquals(IncidentMappingType.MIGRATED)
  } ?: run {
    incidentMappingRepository.deleteAll()
  }

  suspend fun getMappingsByMigrationId(pageRequest: Pageable, migrationId: String): Page<IncidentMappingDto> = coroutineScope {
    val incidentMapping = async {
      incidentMappingRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
        label = migrationId,
        IncidentMappingType.MIGRATED,
        pageRequest,
      )
    }

    val count = async {
      incidentMappingRepository.countAllByLabelAndMappingType(migrationId, mappingType = IncidentMappingType.MIGRATED)
    }

    PageImpl(
      incidentMapping.await().toList().map { IncidentMappingDto(it) },
      pageRequest,
      count.await(),
    )
  }

  suspend fun getMappingForLatestMigrated(): IncidentMappingDto = incidentMappingRepository.findFirstByMappingTypeOrderByWhenCreatedDesc(IncidentMappingType.MIGRATED)
    ?.let { IncidentMappingDto(it) }
    ?: throw NotFoundException("No migrated mapping found")
}
