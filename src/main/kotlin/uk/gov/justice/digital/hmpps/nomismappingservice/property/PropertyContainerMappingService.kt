package uk.gov.justice.digital.hmpps.nomismappingservice.property

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException
import java.util.UUID

@Service
@Transactional(readOnly = true)
class PropertyContainerMappingService(
  private val repository: PropertyContainerMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun alreadyExistsMessage(
    duplicateMapping: PropertyContainerMappingDto,
    existingMapping: PropertyContainerMappingDto,
  ) = """PropertyContainer mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
  """.trimMargin()

  @Transactional
  suspend fun createMapping(createMappingRequest: PropertyContainerMappingDto) {
    repository.findOneByNomisPropertyContainerId(createMappingRequest.nomisPropertyContainerId)
      ?.let { mapping ->
        if (mapping.dpsPropertyContainerId.toString() == createMappingRequest.dpsPropertyContainerId) {
          log.debug(
            "Not creating. All OK: {}",
            alreadyExistsMessage(
              duplicateMapping = createMappingRequest,
              existingMapping = PropertyContainerMappingDto(mapping),
            ),
          )
          return
        }
        throw DuplicateMappingException(
          messageIn = alreadyExistsMessage(
            duplicateMapping = createMappingRequest,
            existingMapping = PropertyContainerMappingDto(mapping),
          ),
          duplicate = createMappingRequest,
          existing = PropertyContainerMappingDto(mapping),
        )
      }

    repository.save(createMappingRequest.fromDto())
    telemetryClient.trackEvent(
      "property-container-mapping-created",
      mapOf(
        "dpsPropertyContainerId" to createMappingRequest.dpsPropertyContainerId,
        "nomisPropertyContainerId" to createMappingRequest.nomisPropertyContainerId.toString(),
        "batchId" to createMappingRequest.label,
      ),
      null,
    )
  }

  suspend fun getMappingByNomisId(nomisPropertyContainerId: Long): PropertyContainerMappingDto = repository
    .findOneByNomisPropertyContainerId(nomisPropertyContainerId)
    ?.let { PropertyContainerMappingDto(it) }
    ?: throw NotFoundException("property container with booking id =$nomisPropertyContainerId not found")

  suspend fun getMappingByDpsId(dpsPropertyContainerId: String): PropertyContainerMappingDto = repository
    .findById(UUID.fromString(dpsPropertyContainerId))
    ?.let { PropertyContainerMappingDto(it) }
    ?: throw NotFoundException("PropertyContainer with dpsPropertyContainerId=$dpsPropertyContainerId not found")

  suspend fun getMappingForLatestMigrated(): PropertyContainerMappingDto = repository
    .findFirstByMappingTypeOrderByWhenCreatedDesc(PropertyContainerMappingType.MIGRATED)
    ?.let { PropertyContainerMappingDto(it) }
    ?: throw NotFoundException("No migrated mapping found")

  suspend fun getCountByMigrationId(
    migrationId: String,
  ): Long = repository.countAllByLabel(migrationId)

  @Transactional
  suspend fun deleteMapping(dpsPropertyContainerId: String) = repository
    .deleteById(UUID.fromString(dpsPropertyContainerId))

  @Transactional
  suspend fun deleteMapping(nomisPropertyContainerId: Long) = repository
    .deleteByNomisPropertyContainerId(nomisPropertyContainerId)

  fun PropertyContainerMapping.toDto() = PropertyContainerMappingDto(this)

  suspend fun PropertyContainerMappingDto.fromDto() = PropertyContainerMapping(
    dpsPropertyContainerId = UUID.fromString(dpsPropertyContainerId),
    nomisPropertyContainerId = nomisPropertyContainerId,
    bookingId = bookingId,
    label = label,
    mappingType = mappingType,
    whenCreated = whenCreated,
  )
}
