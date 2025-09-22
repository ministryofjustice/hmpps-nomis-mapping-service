package uk.gov.justice.digital.hmpps.nomismappingservice.finance

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class PrisonBalanceService(
  private val repository: PrisonBalanceMappingRepository,
) {
  suspend fun getMappingByNomisId(nomisId: String) = repository.findOneByNomisId(nomisId)
    ?.toDto()
    ?: throw NotFoundException("No prison balance mapping found for nomisId=$nomisId")

  suspend fun getMappingByDpsId(dpsId: String) = repository.findOneByDpsId(dpsId = dpsId)
    ?.toDto()
    ?: throw NotFoundException("No prison balance mapping found for dps Id=$dpsId")

  suspend fun getMappingByDpsIdOrNull(dpsId: String) = repository.findOneByDpsId(dpsId = dpsId)
    ?.toDto()

  @Transactional
  suspend fun createMapping(mapping: PrisonBalanceMappingDto) {
    repository.save(mapping.fromDto())
  }

  suspend fun getMappingsByMigrationId(pageRequest: Pageable, migrationId: String): PagedModel<PrisonBalanceMappingDto> = coroutineScope {
    val mappings = async {
      repository.findAllByLabelAndMappingTypeOrderByLabelDesc(
        label = migrationId,
        mappingType = PrisonBalanceMappingType.MIGRATED,
        pageRequest = pageRequest,
      )
    }

    val count = async {
      repository.countAllByLabelAndMappingType(
        migrationId = migrationId,
        mappingType = PrisonBalanceMappingType.MIGRATED,
      )
    }
    PagedModel(
      PageImpl(
        mappings.await().toList().map { it.toDto() },
        pageRequest,
        count.await(),
      ),
    )
  }

  @Transactional
  suspend fun deletePrisonBalanceMappingByDpsId(dpsId: String) = repository.deleteById(dpsId)

  @Transactional
  suspend fun deleteAllMappings() {
    repository.deleteAll()
  }
}

fun PrisonBalanceMappingDto.fromDto() = PrisonBalanceMapping(
  dpsId = dpsId,
  nomisId = nomisId,
  label = label,
  mappingType = mappingType,
)

private fun PrisonBalanceMapping.toDto() = PrisonBalanceMappingDto(
  dpsId = dpsId,
  nomisId = nomisId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
