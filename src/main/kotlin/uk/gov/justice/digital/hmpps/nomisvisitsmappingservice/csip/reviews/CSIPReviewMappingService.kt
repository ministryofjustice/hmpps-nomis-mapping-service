package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.reviews

import kotlinx.coroutines.flow.collect
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees.toDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CSIPReviewMappingService(
  private val repository: CSIPReviewMappingRepository,
) {
  @Transactional
  suspend fun createMapping(mappingDto: CSIPReviewMappingDto): CSIPReviewMapping =
    repository.save(mappingDto.fromDto())

  @Transactional
  suspend fun createMappings(mappingDtoList: List<CSIPReviewMappingDto>) {
    repository.saveAll(mappingDtoList.map { it.fromDto() }).collect()
  }

  suspend fun findAllByDpsCSIPReportId(dpsCSIPReportId: String) = repository.findAllByDpsCSIPReportId(dpsCSIPReportId).map { it.toDto() }

  suspend fun getMappingByNomisId(nomisCSIPReviewId: Long): CSIPReviewMappingDto =
    repository.findOneByNomisCSIPReviewId(
      nomisCSIPReviewId = nomisCSIPReviewId,
    )
      ?.toDto()
      ?: throw NotFoundException("No CSIP Review mapping for  nomisCSIPReviewId=$nomisCSIPReviewId")

  suspend fun getMappingByDpsId(dpsCSIPReviewId: String): CSIPReviewMappingDto =
    repository.findById(dpsCSIPReviewId)
      ?.toDto()
      ?: throw NotFoundException("No CSIP review mapping found for dpsCSIPReviewId=$dpsCSIPReviewId")

  @Transactional
  suspend fun deleteMappingByDpsId(dpsCSIPReviewId: String) =
    repository.deleteById(dpsCSIPReviewId)

  fun alreadyExistsMessage(
    duplicateMapping: CSIPReviewMappingDto,
    existingMapping: CSIPReviewMappingDto,
  ) =
    """CSIPReview Review mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
    """.trimMargin()
}

fun CSIPReviewMapping.toDto() = CSIPReviewMappingDto(
  nomisCSIPReviewId = nomisCSIPReviewId,
  dpsCSIPReviewId = dpsCSIPReviewId,
  dpsCSIPReportId = dpsCSIPReportId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

fun CSIPReviewMappingDto.fromDto() = CSIPReviewMapping(
  nomisCSIPReviewId = nomisCSIPReviewId,
  dpsCSIPReviewId = dpsCSIPReviewId,
  dpsCSIPReportId = dpsCSIPReportId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
