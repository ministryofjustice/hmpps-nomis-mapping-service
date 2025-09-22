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
class PrisonerBalanceService(
  private val repository: PrisonerBalanceMappingRepository,
) {
  suspend fun getMappingByNomisId(nomisRootOffenderId: Long) = repository.findOneByNomisRootOffenderId(nomisRootOffenderId)
    ?.toDto()
    ?: throw NotFoundException("No prisoner balance mapping found for nomisRootOffenderId=$nomisRootOffenderId")

  suspend fun getMappingByDpsId(dpsId: String) = repository.findOneByDpsId(dpsId = dpsId)
    ?.toDto()
    ?: throw NotFoundException("No prisoner balance mapping found for dps Id=$dpsId")

  suspend fun getMappingByDpsIdOrNull(dpsId: String) = repository.findOneByDpsId(dpsId = dpsId)
    ?.toDto()

  @Transactional
  suspend fun createMapping(mapping: PrisonerBalanceMappingDto) {
    repository.save(mapping.fromDto())
  }

  suspend fun getMappingsByMigrationId(pageRequest: Pageable, migrationId: String): PagedModel<PrisonerBalanceMappingDto> = coroutineScope {
    val mappings = async {
      repository.findAllByLabelAndMappingTypeOrderByLabelDesc(
        label = migrationId,
        mappingType = PrisonerBalanceMappingType.MIGRATED,
        pageRequest = pageRequest,
      )
    }

    val count = async {
      repository.countAllByLabelAndMappingType(
        migrationId = migrationId,
        mappingType = PrisonerBalanceMappingType.MIGRATED,
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
  suspend fun deletePrisonerBalanceMappingByDpsId(dpsId: String) = repository.deleteById(dpsId)

  @Transactional
  suspend fun deleteAllMappings() {
    repository.deleteAll()
  }
}

fun PrisonerBalanceMappingDto.fromDto() = PrisonerBalanceMapping(
  dpsId = dpsId,
  nomisRootOffenderId = nomisRootOffenderId,
  label = label,
  mappingType = mappingType,
)

private fun PrisonerBalanceMapping.toDto() = PrisonerBalanceMappingDto(
  dpsId = dpsId,
  nomisRootOffenderId = nomisRootOffenderId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
