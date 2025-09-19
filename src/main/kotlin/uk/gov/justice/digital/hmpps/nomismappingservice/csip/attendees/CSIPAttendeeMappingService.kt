package uk.gov.justice.digital.hmpps.nomismappingservice.csip.attendees

import kotlinx.coroutines.flow.collect
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.csip.CSIPChildMappingDto
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CSIPAttendeeMappingService(
  private val repository: CSIPAttendeeMappingRepository,
) {

  @Transactional
  suspend fun createMapping(mappingDto: CSIPChildMappingDto): CSIPAttendeeMapping = repository.save(mappingDto.fromDto())

  @Transactional
  suspend fun createMappings(mappingDtoList: List<CSIPChildMappingDto>) {
    repository.saveAll(mappingDtoList.map { it.fromDto() }).collect()
  }

  suspend fun getMappingByNomisId(nomisCSIPAttendeeId: Long): CSIPChildMappingDto = repository.findOneByNomisCSIPAttendeeId(
    nomisCSIPAttendeeId = nomisCSIPAttendeeId,
  )
    ?.toDto()
    ?: throw NotFoundException("No CSIP Attendee mapping for  nomisCSIPAttendeeId=$nomisCSIPAttendeeId")

  suspend fun findAllByDpsCSIPReportId(dpsCSIPReportId: String) = repository.findAllByDpsCSIPReportId(dpsCSIPReportId).map { it.toDto() }

  suspend fun getMappingByDpsId(dpsCSIPAttendeeId: String): CSIPChildMappingDto = repository.findById(dpsCSIPAttendeeId)
    ?.toDto()
    ?: throw NotFoundException("No CSIP attendee mapping found for dpsCSIPAttendeeId=$dpsCSIPAttendeeId")

  @Transactional
  suspend fun deleteMappingByDpsId(dpsCSIPAttendeeId: String) = repository.deleteById(dpsCSIPAttendeeId)

  fun alreadyExistsMessage(
    duplicateMapping: CSIPChildMappingDto,
    existingMapping: CSIPChildMappingDto,
  ) = """CSIPAttendee Attendee mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
  """.trimMargin()
}

fun CSIPAttendeeMapping.toDto() = CSIPChildMappingDto(
  nomisId = nomisCSIPAttendeeId,
  dpsId = dpsCSIPAttendeeId,
  dpsCSIPReportId = dpsCSIPReportId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

fun CSIPChildMappingDto.fromDto() = CSIPAttendeeMapping(
  nomisCSIPAttendeeId = nomisId,
  dpsCSIPAttendeeId = dpsId,
  dpsCSIPReportId = dpsCSIPReportId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
