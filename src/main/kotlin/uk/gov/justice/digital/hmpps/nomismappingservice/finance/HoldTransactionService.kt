package uk.gov.justice.digital.hmpps.nomismappingservice.finance

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.StandardMappingType
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class HoldTransactionService(
  private val repository: HoldTransactionMappingRepository,
) {
  suspend fun getMappingByNomisId(nomisTransactionId: Long) = repository.findOneByNomisId(nomisTransactionId)
    ?.toDto()
    ?: throw NotFoundException("No hold balance mapping found for nomisTransactionId=$nomisTransactionId")

  suspend fun getMappingByDpsId(dpsId: String) = repository.findOneByDpsId(dpsId = dpsId)
    ?.toDto()
    ?: throw NotFoundException("No hold balance mapping found for dps Id=$dpsId")

  suspend fun getMappingByDpsIdOrNull(dpsId: String) = repository.findOneByDpsId(dpsId = dpsId)
    ?.toDto()

  @Transactional
  suspend fun createMapping(mapping: HoldTransactionMappingDto) {
    repository.save(mapping.fromDto())
  }

  suspend fun getAllMappings(pageRequest: Pageable): PagedModel<HoldTransactionMappingDto> = coroutineScope {
    val mappings = async {
      repository.findAllBy(
        pageRequest = pageRequest,
      )
    }

    val count = async {
      repository.count()
    }
    PagedModel(
      PageImpl(
        mappings.await().toList().map { it.toDto() },
        pageRequest,
        count.await(),
      ),
    )
  }

  suspend fun getMappingsByMigrationId(pageRequest: Pageable, migrationId: String): PagedModel<HoldTransactionMappingDto> = coroutineScope {
    val mappings = async {
      repository.findAllByLabelAndMappingTypeOrderByLabelDesc(
        label = migrationId,
        mappingType = StandardMappingType.MIGRATED,
        pageRequest = pageRequest,
      )
    }

    val count = async {
      repository.countAllByLabelAndMappingType(
        migrationId = migrationId,
        mappingType = StandardMappingType.MIGRATED,
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
  suspend fun deleteHoldTransactionMappingByDpsId(dpsId: String) = repository.deleteById(dpsId)

  @Transactional
  suspend fun deleteAllMappings() {
    repository.deleteAll()
  }
}

fun HoldTransactionMappingDto.fromDto() = HoldTransactionMapping(
  dpsId = dpsId,
  nomisId = nomisTransactionId,
  label = label,
  mappingType = mappingType,
)

private fun HoldTransactionMapping.toDto() = HoldTransactionMappingDto(
  dpsId = dpsId,
  nomisTransactionId = nomisId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
