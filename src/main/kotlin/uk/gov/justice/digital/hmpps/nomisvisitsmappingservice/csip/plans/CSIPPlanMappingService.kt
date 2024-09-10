package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans

import kotlinx.coroutines.flow.collect
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees.toDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.reviews.fromDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CSIPPlanMappingService(
  private val repository: CSIPPlanMappingRepository,
) {
  @Transactional
  suspend fun createMapping(mappingDto: CSIPPlanMappingDto): CSIPPlanMapping =
    repository.save(mappingDto.fromDto())

  @Transactional
  suspend fun createMappings(mappingDtoList: List<CSIPPlanMappingDto>) {
    repository.saveAll(mappingDtoList.map { it.fromDto() }).collect()
  }

  suspend fun findAllByDpsCSIPReportId(dpsCSIPReportId: String) = repository.findAllByDpsCSIPReportId(dpsCSIPReportId).map { it.toDto() }

  suspend fun getMappingByNomisId(nomisCSIPPlanId: Long): CSIPPlanMappingDto =
    repository.findOneByNomisCSIPPlanId(
      nomisCSIPPlanId = nomisCSIPPlanId,
    )
      ?.toDto()
      ?: throw NotFoundException("No CSIP Plan mapping for  nomisCSIPPlanId=$nomisCSIPPlanId")

  suspend fun getMappingByDpsId(dpsCSIPPlanId: String): CSIPPlanMappingDto =
    repository.findById(dpsCSIPPlanId)
      ?.toDto()
      ?: throw NotFoundException("No CSIP plan mapping found for dpsCSIPPlanId=$dpsCSIPPlanId")

  @Transactional
  suspend fun deleteMappingByDpsId(dpsCSIPPlanId: String) =
    repository.deleteById(dpsCSIPPlanId)

  fun alreadyExistsMessage(
    duplicateMapping: CSIPPlanMappingDto,
    existingMapping: CSIPPlanMappingDto,
  ) =
    """CSIPPlan Plan mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
    """.trimMargin()
}

fun CSIPPlanMapping.toDto() = CSIPPlanMappingDto(
  nomisCSIPPlanId = nomisCSIPPlanId,
  dpsCSIPPlanId = dpsCSIPPlanId,
  dpsCSIPReportId = dpsCSIPReportId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

fun CSIPPlanMappingDto.fromDto() = CSIPPlanMapping(
  nomisCSIPPlanId = nomisCSIPPlanId,
  dpsCSIPPlanId = dpsCSIPPlanId,
  dpsCSIPReportId = dpsCSIPReportId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
