package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.visitorders

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class VisitOrderBalanceService(
  private val repository: VisitOrderBalanceMappingRepository,
) {
  suspend fun getMappingByNomisId(nomisPrisonNumber: String) = repository.findOneByNomisPrisonNumber(nomisPrisonNumber = nomisPrisonNumber)
    ?.toDto()
    ?: throw NotFoundException("No visit order balance mapping found for nomisPrisonNumber=$nomisPrisonNumber")

  suspend fun getMappingByDpsId(dpsId: String) = repository.findOneByDpsId(dpsId = dpsId)
    ?.toDto()
    ?: throw NotFoundException("No visit order balance mapping found for dps Id=$dpsId")

  suspend fun getMappingByDpsIdOrNull(dpsId: String) = repository.findOneByDpsId(dpsId = dpsId)
    ?.toDto()

  @Transactional
  suspend fun createMapping(mapping: VisitOrderBalanceMappingDto) {
    repository.save(mapping.fromDto())
  }

  suspend fun getMappingsByMigrationId(pageRequest: Pageable, migrationId: String): Page<VisitOrderBalanceMappingDto> = coroutineScope {
    val mappings = async {
      repository.findAllByLabelAndMappingTypeOrderByLabelDesc(
        label = migrationId,
        mappingType = VisitOrderBalanceMappingType.MIGRATED,
        pageRequest = pageRequest,
      )
    }

    val count = async {
      repository.countAllByLabelAndMappingType(
        migrationId = migrationId,
        mappingType = VisitOrderBalanceMappingType.MIGRATED,
      )
    }
    PageImpl(
      mappings.await().toList().map { it.toDto() },
      pageRequest,
      count.await(),
    )
  }

  @Transactional
  suspend fun deleteVisitOrderBalanceMappingByDpsId(dpsId: String) = repository.deleteById(dpsId)

  @Transactional
  suspend fun deleteAllMappings() {
    repository.deleteAll()
  }
}

fun VisitOrderBalanceMappingDto.fromDto() = VisitOrderBalanceMapping(
  dpsId = dpsId,
  nomisPrisonNumber = nomisPrisonNumber,
  label = label,
  mappingType = mappingType,
)

private fun VisitOrderBalanceMapping.toDto() = VisitOrderBalanceMappingDto(
  dpsId = dpsId,
  nomisPrisonNumber = nomisPrisonNumber,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
