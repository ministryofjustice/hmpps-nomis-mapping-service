package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors

import kotlinx.coroutines.flow.collect
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CSIPFactorMappingService(
  private val repository: CSIPFactorMappingRepository,
) {
  @Transactional
  suspend fun createMapping(mappingDto: CSIPFactorMappingDto): CSIPFactorMapping =
    repository.save(mappingDto.fromDto())

  @Transactional
  suspend fun createMappings(mappingDtoList: List<CSIPFactorMappingDto>) {
    repository.saveAll(mappingDtoList.map { it.fromDto() }).collect()
  }

  suspend fun findAllByDpsCSIPReportId(dpsCSIPReportId: String) = repository.findAllByDpsCSIPReportId(dpsCSIPReportId).map { it.toDto() }

  suspend fun getMappingByNomisId(nomisCSIPFactorId: Long): CSIPFactorMappingDto =
    repository.findOneByNomisCSIPFactorId(
      nomisCSIPFactorId = nomisCSIPFactorId,
    )
      ?.toDto()
      ?: throw NotFoundException("No CSIP Factor mapping for  nomisCSIPFactorId=$nomisCSIPFactorId")

  suspend fun getMappingByDpsId(dpsCSIPFactorId: String): CSIPFactorMappingDto =
    repository.findById(dpsCSIPFactorId)
      ?.toDto()
      ?: throw NotFoundException("No CSIP factor mapping found for dpsCSIPFactorId=$dpsCSIPFactorId")

  @Transactional
  suspend fun deleteMappingByDpsId(dpsCSIPFactorId: String) =
    repository.deleteById(dpsCSIPFactorId)

  fun alreadyExistsMessage(
    duplicateMapping: CSIPFactorMappingDto,
    existingMapping: CSIPFactorMappingDto,
  ) =
    """CSIPFactor Factor mapping already exists.
       |Existing mapping: $existingMapping
       |Duplicate mapping: $duplicateMapping
    """.trimMargin()
}

fun CSIPFactorMapping.toDto() = CSIPFactorMappingDto(
  nomisCSIPFactorId = nomisCSIPFactorId,
  dpsCSIPFactorId = dpsCSIPFactorId,
  dpsCSIPReportId = dpsCSIPReportId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

fun CSIPFactorMappingDto.fromDto() = CSIPFactorMapping(
  nomisCSIPFactorId = nomisCSIPFactorId,
  dpsCSIPFactorId = dpsCSIPFactorId,
  dpsCSIPReportId = dpsCSIPReportId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
