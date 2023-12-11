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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.NonAssociationMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.NonAssociationMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.NonAssociationMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.NonAssociationMappingType.MIGRATED
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.NonAssociationMappingRepository

@Service
@Transactional(readOnly = true)
class NonAssociationMappingService(
  private val nonAssociationMappingRepository: NonAssociationMappingRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun alreadyExistsMessage(
    duplicateMapping: NonAssociationMappingDto,
    existingMapping: NonAssociationMappingDto,
  ) =
    "Non-association mapping already exists. \nExisting mapping: $existingMapping\nDuplicate mapping: $duplicateMapping"

  @Transactional
  suspend fun createNonAssociationMapping(createMappingRequest: NonAssociationMappingDto) =
    with(createMappingRequest) {
      log.debug("creating nonAssociation $createMappingRequest")
      nonAssociationMappingRepository.findById(nonAssociationId)?.run {
        if (this@run.firstOffenderNo == this@with.firstOffenderNo &&
          this@run.secondOffenderNo == this@with.secondOffenderNo &&
          this@run.nomisTypeSequence == this@with.nomisTypeSequence
        ) {
          log.debug(
            "Not creating. All OK: " +
              alreadyExistsMessage(
                duplicateMapping = createMappingRequest,
                existingMapping = NonAssociationMappingDto(this@run),
              ),
          )
          return
        }
        throw DuplicateMappingException(
          messageIn = alreadyExistsMessage(
            duplicateMapping = createMappingRequest,
            existingMapping = NonAssociationMappingDto(this@run),
          ),
          duplicate = createMappingRequest,
          existing = NonAssociationMappingDto(this@run),
        )
      }

      nonAssociationMappingRepository.findOneByFirstOffenderNoAndSecondOffenderNoAndNomisTypeSequence(
        firstOffenderNo = firstOffenderNo,
        secondOffenderNo = secondOffenderNo,
        nomisTypeSequence = nomisTypeSequence,
      )?.run {
        throw DuplicateMappingException(
          messageIn = alreadyExistsMessage(
            duplicateMapping = createMappingRequest,
            existingMapping = NonAssociationMappingDto(this@run),
          ),
          duplicate = createMappingRequest,
          existing = NonAssociationMappingDto(this),
        )
      }

      nonAssociationMappingRepository.save(
        NonAssociationMapping(
          nonAssociationId = nonAssociationId,
          firstOffenderNo = firstOffenderNo,
          secondOffenderNo = secondOffenderNo,
          nomisTypeSequence = nomisTypeSequence,
          label = label,
          mappingType = NonAssociationMappingType.valueOf(mappingType),
        ),
      )
      telemetryClient.trackEvent(
        "nonAssociation-mapping-created",
        mapOf(
          "nonAssociationId" to nonAssociationId.toString(),
          "firstOffenderNo" to firstOffenderNo,
          "secondOffenderNo" to secondOffenderNo,
          "nomisTypeSequence" to nomisTypeSequence.toString(),
          "batchId" to label,
        ),
        null,
      )
      log.debug("Mapping created with nonAssociationId = $nonAssociationId, firstOffenderNo=$firstOffenderNo, secondOffenderNo=$secondOffenderNo, and nomisTypeSequence=$nomisTypeSequence")
    }

  suspend fun getNonAssociationMappingByNomisId(firstOffenderNo: String, secondOffenderNo: String, nomisTypeSequence: Int): NonAssociationMappingDto =
    nonAssociationMappingRepository.findOneByFirstOffenderNoAndSecondOffenderNoAndNomisTypeSequence(
      firstOffenderNo = firstOffenderNo,
      secondOffenderNo = secondOffenderNo,
      nomisTypeSequence = nomisTypeSequence,
    )
      ?.let { NonAssociationMappingDto(it) }
      ?: throw NotFoundException("Non-association with firstOffenderNo=$firstOffenderNo, secondOffenderNo=$secondOffenderNo, and nomisTypeSequence=$nomisTypeSequence not found")

  suspend fun getNonAssociationMappingByNonAssociationId(nonAssociationId: Long): NonAssociationMappingDto =
    nonAssociationMappingRepository.findById(nonAssociationId)
      ?.let { NonAssociationMappingDto(it) }
      ?: throw NotFoundException("nonAssociationId=$nonAssociationId")

  suspend fun getNonAssociationMappingsByMigrationId(pageRequest: Pageable, migrationId: String): Page<NonAssociationMappingDto> =
    coroutineScope {
      val nonAssociationMapping = async {
        nonAssociationMappingRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
          label = migrationId,
          MIGRATED,
          pageRequest,
        )
      }

      val count = async {
        nonAssociationMappingRepository.countAllByLabelAndMappingType(migrationId, mappingType = MIGRATED)
      }

      PageImpl(
        nonAssociationMapping.await().toList().map { NonAssociationMappingDto(it) },
        pageRequest,
        count.await(),
      )
    }

  suspend fun getNonAssociationMappingForLatestMigrated(): NonAssociationMappingDto =
    nonAssociationMappingRepository.findFirstByMappingTypeOrderByWhenCreatedDesc(MIGRATED)
      ?.let { NonAssociationMappingDto(it) }
      ?: throw NotFoundException("No migrated mapping found")

  @Transactional
  suspend fun deleteNonAssociationMapping(nonAssociationId: Long) = nonAssociationMappingRepository.deleteById(nonAssociationId)

  @Transactional
  suspend fun deleteNonAssociationMappings(onlyMigrated: Boolean) =
    onlyMigrated.takeIf { it }?.apply {
      nonAssociationMappingRepository.deleteByMappingTypeEquals(MIGRATED)
    } ?: run {
      nonAssociationMappingRepository.deleteAll()
    }
}
