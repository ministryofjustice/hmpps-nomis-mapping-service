package uk.gov.justice.digital.hmpps.nomismappingservice.visitbalances

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class VisitBalanceService(
  private val repository: VisitBalanceMappingRepository,
) {
  suspend fun getMappingByNomisId(nomisVisitBalanceId: Long) = repository.findOneByNomisVisitBalanceId(nomisVisitBalanceId)
    ?.toDto()
    ?: throw NotFoundException("No visit order balance mapping found for nomisVisitBalanceId=$nomisVisitBalanceId")

  suspend fun getMappingByDpsId(dpsId: String) = repository.findOneByDpsId(dpsId = dpsId)
    ?.toDto()
    ?: throw NotFoundException("No visit order balance mapping found for dps Id=$dpsId")

  suspend fun getMappingByDpsIdOrNull(dpsId: String) = repository.findOneByDpsId(dpsId = dpsId)
    ?.toDto()

  @Transactional
  suspend fun createMapping(mapping: VisitBalanceMappingDto) {
    repository.save(mapping.fromDto())
  }

  suspend fun getMappingsByMigrationId(pageRequest: Pageable, migrationId: String): Page<VisitBalanceMappingDto> = coroutineScope {
    val mappings = async {
      repository.findAllByLabelAndMappingTypeOrderByLabelDesc(
        label = migrationId,
        mappingType = VisitBalanceMappingType.MIGRATED,
        pageRequest = pageRequest,
      )
    }

    val count = async {
      repository.countAllByLabelAndMappingType(
        migrationId = migrationId,
        mappingType = VisitBalanceMappingType.MIGRATED,
      )
    }
    PageImpl(
      mappings.await().toList().map { it.toDto() },
      pageRequest,
      count.await(),
    )
  }

  @Transactional
  suspend fun deleteVisitBalanceMappingByDpsId(dpsId: String) = repository.deleteById(dpsId)

  @Transactional
  suspend fun deleteAllMappings() {
    repository.deleteAll()
  }
}

fun VisitBalanceMappingDto.fromDto() = VisitBalanceMapping(
  dpsId = dpsId,
  nomisVisitBalanceId = nomisVisitBalanceId,
  label = label,
  mappingType = mappingType,
)

private fun VisitBalanceMapping.toDto() = VisitBalanceMappingDto(
  dpsId = dpsId,
  nomisVisitBalanceId = nomisVisitBalanceId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
