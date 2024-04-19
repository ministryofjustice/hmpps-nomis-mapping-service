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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config.DuplicateMappingException
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.LocationMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.LocationMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.LocationMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.LocationMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.LocationMappingRepository

@Service
@Transactional(readOnly = true)
class LocationMappingService(
  private val locationMappingRepository: LocationMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun alreadyExistsMessage(
    duplicateMapping: LocationMappingDto,
    existingMapping: LocationMappingDto,
  ) =
    """Location mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
    """.trimMargin()

  @Transactional
  suspend fun createMapping(createMappingRequest: LocationMappingDto) =
    with(createMappingRequest) {
      log.debug("creating location {}", createMappingRequest)
      locationMappingRepository.findById(dpsLocationId)?.run {
        if (this@run.nomisLocationId == this@with.nomisLocationId) {
          log.debug(
            "Not creating. All OK: {}",
            alreadyExistsMessage(
              duplicateMapping = createMappingRequest,
              existingMapping = LocationMappingDto(this@run),
            ),
          )
          return
        }
        throw DuplicateMappingException(
          messageIn = alreadyExistsMessage(
            duplicateMapping = createMappingRequest,
            existingMapping = LocationMappingDto(this@run),
          ),
          duplicate = createMappingRequest,
          existing = LocationMappingDto(this@run),
        )
      }

      locationMappingRepository.findOneByNomisLocationId(nomisLocationId)?.run {
        throw DuplicateMappingException(
          messageIn = alreadyExistsMessage(
            duplicateMapping = createMappingRequest,
            existingMapping = LocationMappingDto(this@run),
          ),
          duplicate = createMappingRequest,
          existing = LocationMappingDto(this),
        )
      }

      locationMappingRepository.save(
        LocationMapping(
          dpsLocationId = dpsLocationId,
          nomisLocationId = nomisLocationId,
          label = label,
          mappingType = LocationMappingType.valueOf(mappingType),
        ),
      )
      telemetryClient.trackEvent(
        "location-mapping-created",
        mapOf(
          "id" to dpsLocationId,
          "nomisLocationId" to nomisLocationId.toString(),
          "batchId" to label,
        ),
        null,
      )
      log.debug("Mapping created with id = $dpsLocationId, nomisLocationId=$nomisLocationId")
    }

  suspend fun getMappingByNomisId(nomisLocationId: Long): LocationMappingDto =
    locationMappingRepository.findOneByNomisLocationId(nomisLocationId)
      ?.let { LocationMappingDto(it) }
      ?: throw NotFoundException("Location with nomisLocationId=$nomisLocationId not found")

  suspend fun getMappingByDpsId(dpsLocationId: String): LocationMappingDto =
    locationMappingRepository.findById(dpsLocationId)
      ?.let { LocationMappingDto(it) }
      ?: throw NotFoundException("Location with dpsLocationId=$dpsLocationId not found")

  suspend fun getMappingsByMigrationId(pageRequest: Pageable, migrationId: String): Page<LocationMappingDto> =
    coroutineScope {
      val locationMapping = async {
        locationMappingRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
          label = migrationId,
          MIGRATED,
          pageRequest,
        )
      }

      val count = async {
        locationMappingRepository.countAllByLabelAndMappingType(migrationId, mappingType = MIGRATED)
      }

      PageImpl(
        locationMapping.await().toList().map { LocationMappingDto(it) },
        pageRequest,
        count.await(),
      )
    }

  suspend fun getMappingForLatestMigrated(): LocationMappingDto =
    locationMappingRepository.findFirstByMappingTypeOrderByWhenCreatedDesc(MIGRATED)
      ?.let { LocationMappingDto(it) }
      ?: throw NotFoundException("No migrated mapping found")

  @Transactional
  suspend fun deleteMapping(dpsLocationId: String) = locationMappingRepository.deleteById(dpsLocationId)

  @Transactional
  suspend fun deleteMapping(nomisLocationId: Long) = locationMappingRepository.deleteByNomisLocationId(nomisLocationId)

  @Transactional
  suspend fun deleteMappings(onlyMigrated: Boolean) =
    onlyMigrated.takeIf { it }?.apply {
      locationMappingRepository.deleteByMappingTypeEquals(MIGRATED)
    } ?: run {
      locationMappingRepository.deleteAll()
    }
}
