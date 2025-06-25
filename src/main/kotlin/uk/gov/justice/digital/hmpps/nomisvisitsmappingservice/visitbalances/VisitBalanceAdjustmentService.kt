package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.visitbalances

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class VisitBalanceAdjustmentService(
  private val repository: VisitBalanceAdjustmentMappingRepository,
) {
  suspend fun getMappingByNomisId(nomisVisitBalanceAdjustmentId: Long) = repository.findOneByNomisId(nomisVisitBalanceAdjustmentId)
    ?.toDto()
    ?: throw NotFoundException("No visit balance adjustment mapping found for nomisVisitBalanceAdjustmentId=$nomisVisitBalanceAdjustmentId")

  suspend fun getMappingsByDpsId(dpsId: String) = repository.findAllByDpsIdOrderByNomisIdDesc(dpsId = dpsId).map { it.toDto() }

  @Transactional
  suspend fun createMapping(mapping: VisitBalanceAdjustmentMappingDto) {
    repository.save(mapping.fromDto())
  }

  @Transactional
  suspend fun deleteVisitBalanceAdjustmentMappingsByDpsId(dpsId: String) = repository.deleteById(dpsId)

  @Transactional
  suspend fun deleteAllMappings() {
    repository.deleteAll()
  }
}

fun VisitBalanceAdjustmentMappingDto.fromDto() = VisitBalanceAdjustmentMapping(
  dpsId = dpsId,
  nomisId = nomisVisitBalanceAdjustmentId,
  label = label,
  mappingType = mappingType,
)

private fun VisitBalanceAdjustmentMapping.toDto() = VisitBalanceAdjustmentMappingDto(
  dpsId = dpsId,
  nomisVisitBalanceAdjustmentId = nomisId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
