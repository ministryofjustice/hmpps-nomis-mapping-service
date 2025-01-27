package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans

import kotlinx.coroutines.flow.collect
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPChildMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CSIPPlanMappingService(
  private val repository: CSIPPlanMappingRepository,
) {
  @Transactional
  suspend fun createMapping(mappingDto: CSIPChildMappingDto): CSIPPlanMapping = repository.save(mappingDto.fromDto())

  @Transactional
  suspend fun createMappings(mappingDtoList: List<CSIPChildMappingDto>) {
    repository.saveAll(mappingDtoList.map { it.fromDto() }).collect()
  }

  suspend fun findAllByDpsCSIPReportId(dpsCSIPReportId: String) = repository.findAllByDpsCSIPReportId(dpsCSIPReportId).map { it.toDto() }

  suspend fun getMappingByNomisId(nomisCSIPPlanId: Long): CSIPChildMappingDto = repository.findOneByNomisCSIPPlanId(
    nomisCSIPPlanId = nomisCSIPPlanId,
  )
    ?.toDto()
    ?: throw NotFoundException("No CSIP Plan mapping for  nomisCSIPPlanId=$nomisCSIPPlanId")

  suspend fun getMappingByDpsId(dpsCSIPPlanId: String): CSIPChildMappingDto = repository.findById(dpsCSIPPlanId)
    ?.toDto()
    ?: throw NotFoundException("No CSIP plan mapping found for dpsCSIPPlanId=$dpsCSIPPlanId")

  @Transactional
  suspend fun deleteMappingByDpsId(dpsCSIPPlanId: String) = repository.deleteById(dpsCSIPPlanId)

  fun alreadyExistsMessage(
    duplicateMapping: CSIPChildMappingDto,
    existingMapping: CSIPChildMappingDto,
  ) = """CSIPPlan Plan mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
  """.trimMargin()
}

fun CSIPPlanMapping.toDto() = CSIPChildMappingDto(
  nomisId = nomisCSIPPlanId,
  dpsId = dpsCSIPPlanId,
  dpsCSIPReportId = dpsCSIPReportId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

fun CSIPChildMappingDto.fromDto() = CSIPPlanMapping(
  nomisCSIPPlanId = nomisId,
  dpsCSIPPlanId = dpsId,
  dpsCSIPReportId = dpsCSIPReportId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
