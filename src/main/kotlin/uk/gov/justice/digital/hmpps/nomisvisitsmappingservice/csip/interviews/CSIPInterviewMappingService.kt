package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.interviews

import kotlinx.coroutines.flow.collect
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.CSIPChildMappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees.toDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CSIPInterviewMappingService(
  private val repository: CSIPInterviewMappingRepository,
) {

  @Transactional
  suspend fun createMapping(mappingDto: CSIPChildMappingDto): CSIPInterviewMapping = repository.save(mappingDto.fromDto())

  @Transactional
  suspend fun createMappings(mappingDtoList: List<CSIPChildMappingDto>) {
    repository.saveAll(mappingDtoList.map { it.fromDto() }).collect()
  }

  suspend fun findAllByDpsCSIPReportId(dpsCSIPReportId: String) = repository.findAllByDpsCSIPReportId(dpsCSIPReportId).map { it.toDto() }

  suspend fun getMappingByNomisId(nomisCSIPInterviewId: Long): CSIPChildMappingDto = repository.findOneByNomisCSIPInterviewId(
    nomisCSIPInterviewId = nomisCSIPInterviewId,
  )
    ?.toDto()
    ?: throw NotFoundException("No CSIP Interview mapping for  nomisCSIPInterviewId=$nomisCSIPInterviewId")

  suspend fun getMappingByDpsId(dpsCSIPInterviewId: String): CSIPChildMappingDto = repository.findById(dpsCSIPInterviewId)
    ?.toDto()
    ?: throw NotFoundException("No CSIP interview mapping found for dpsCSIPInterviewId=$dpsCSIPInterviewId")

  @Transactional
  suspend fun deleteMappingByDpsId(dpsCSIPInterviewId: String) = repository.deleteById(dpsCSIPInterviewId)

  fun alreadyExistsMessage(
    duplicateMapping: CSIPChildMappingDto,
    existingMapping: CSIPChildMappingDto,
  ) = """CSIPInterview Interview mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
  """.trimMargin()
}

fun CSIPInterviewMapping.toDto() = CSIPChildMappingDto(
  nomisId = nomisCSIPInterviewId,
  dpsId = dpsCSIPInterviewId,
  dpsCSIPReportId = dpsCSIPReportId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

fun CSIPChildMappingDto.fromDto() = CSIPInterviewMapping(
  nomisCSIPInterviewId = nomisId,
  dpsCSIPInterviewId = dpsId,
  dpsCSIPReportId = dpsCSIPReportId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
