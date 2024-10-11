package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.reviews

import kotlinx.coroutines.flow.collect
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPChildMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees.toDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CSIPReviewMappingService(
  private val repository: CSIPReviewMappingRepository,
) {
  @Transactional
  suspend fun createMapping(mappingDto: CSIPChildMappingDto): CSIPReviewMapping =
    repository.save(mappingDto.fromDto())

  @Transactional
  suspend fun createMappings(mappingDtoList: List<CSIPChildMappingDto>) {
    repository.saveAll(mappingDtoList.map { it.fromDto() }).collect()
  }

  suspend fun findAllByDpsCSIPReportId(dpsCSIPReportId: String) = repository.findAllByDpsCSIPReportId(dpsCSIPReportId).map { it.toDto() }

  suspend fun getMappingByNomisId(nomisCSIPReviewId: Long): CSIPChildMappingDto =
    repository.findOneByNomisCSIPReviewId(
      nomisCSIPReviewId = nomisCSIPReviewId,
    )
      ?.toDto()
      ?: throw NotFoundException("No CSIP Review mapping for  nomisCSIPReviewId=$nomisCSIPReviewId")

  suspend fun getMappingByDpsId(dpsCSIPReviewId: String): CSIPChildMappingDto =
    repository.findById(dpsCSIPReviewId)
      ?.toDto()
      ?: throw NotFoundException("No CSIP review mapping found for dpsCSIPReviewId=$dpsCSIPReviewId")

  @Transactional
  suspend fun deleteMappingByDpsId(dpsCSIPReviewId: String) =
    repository.deleteById(dpsCSIPReviewId)

  fun alreadyExistsMessage(
    duplicateMapping: CSIPChildMappingDto,
    existingMapping: CSIPChildMappingDto,
  ) =
    """CSIPReview Review mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
    """.trimMargin()
}

fun CSIPReviewMapping.toDto() = CSIPChildMappingDto(
  nomisId = nomisCSIPReviewId,
  dpsId = dpsCSIPReviewId,
  dpsCSIPReportId = dpsCSIPReportId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

fun CSIPChildMappingDto.fromDto() = CSIPReviewMapping(
  nomisCSIPReviewId = nomisId,
  dpsCSIPReviewId = dpsId,
  dpsCSIPReportId = dpsCSIPReportId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
