package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees

import kotlinx.coroutines.flow.collect
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CSIPAttendeeMappingService(
  private val repository: CSIPAttendeeMappingRepository,
) {

  @Transactional
  suspend fun createMapping(mappingDto: CSIPAttendeeMappingDto): CSIPAttendeeMapping =
    repository.save(mappingDto.fromDto())

  @Transactional
  suspend fun createMappings(mappingDtoList: List<CSIPAttendeeMappingDto>) {
    repository.saveAll(mappingDtoList.map { it.fromDto() }).collect()
  }

  suspend fun getMappingByNomisId(nomisCSIPAttendeeId: Long): CSIPAttendeeMappingDto =
    repository.findOneByNomisCSIPAttendeeId(
      nomisCSIPAttendeeId = nomisCSIPAttendeeId,
    )
      ?.toDto()
      ?: throw NotFoundException("No CSIP Attendee mapping for  nomisCSIPAttendeeId=$nomisCSIPAttendeeId")

  suspend fun findAllByDpsCSIPReportId(dpsCSIPReportId: String) = repository.findAllByDpsCSIPReportId(dpsCSIPReportId).map { it.toDto() }

  suspend fun getMappingByDpsId(dpsCSIPAttendeeId: String): CSIPAttendeeMappingDto =
    repository.findById(dpsCSIPAttendeeId)
      ?.toDto()
      ?: throw NotFoundException("No CSIP attendee mapping found for dpsCSIPAttendeeId=$dpsCSIPAttendeeId")

  @Transactional
  suspend fun deleteMappingByDpsId(dpsCSIPAttendeeId: String) =
    repository.deleteById(dpsCSIPAttendeeId)

  fun alreadyExistsMessage(
    duplicateMapping: CSIPAttendeeMappingDto,
    existingMapping: CSIPAttendeeMappingDto,
  ) =
    """CSIPAttendee Attendee mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
    """.trimMargin()
}

fun CSIPAttendeeMapping.toDto() = CSIPAttendeeMappingDto(
  nomisCSIPAttendeeId = nomisCSIPAttendeeId,
  dpsCSIPAttendeeId = dpsCSIPAttendeeId,
  dpsCSIPReportId = dpsCSIPReportId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

fun CSIPAttendeeMappingDto.fromDto() = CSIPAttendeeMapping(
  nomisCSIPAttendeeId = nomisCSIPAttendeeId,
  dpsCSIPAttendeeId = dpsCSIPAttendeeId,
  dpsCSIPReportId = dpsCSIPReportId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
