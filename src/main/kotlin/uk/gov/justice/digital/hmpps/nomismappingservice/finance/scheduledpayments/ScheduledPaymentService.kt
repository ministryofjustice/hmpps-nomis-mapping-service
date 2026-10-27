package uk.gov.justice.digital.hmpps.nomismappingservice.finance.scheduledpayments

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
class ScheduledPaymentService(
  private val repository: ScheduledPaymentMappingRepository,
) {
  suspend fun getMappingByNomisId(nomisScheduledPaymentId: Long) = repository.findOneByNomisId(nomisScheduledPaymentId)
    ?.toDto()
    ?: throw NotFoundException("No scheduled payment mapping found for nomisScheduledPaymentId=$nomisScheduledPaymentId")

  suspend fun getMappingByDpsId(dpsId: String) = repository.findOneByDpsId(dpsId = dpsId)
    ?.toDto()
    ?: throw NotFoundException("No scheduled payment mapping found for dpsId=$dpsId")

  suspend fun getMappingByDpsIdOrNull(dpsId: String) = repository.findOneByDpsId(dpsId = dpsId)
    ?.toDto()

  @Transactional
  suspend fun createMapping(mapping: ScheduledPaymentMappingDto) {
    repository.save(mapping.fromDto())
  }

  suspend fun getAllMappings(pageRequest: Pageable): PagedModel<ScheduledPaymentMappingDto> = coroutineScope {
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

  suspend fun getMappingsByMigrationId(pageRequest: Pageable, migrationId: String): PagedModel<ScheduledPaymentMappingDto> = coroutineScope {
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
  suspend fun deleteScheduledPaymentMappingByDpsId(dpsId: String) = repository.deleteById(dpsId)

  @Transactional
  suspend fun deleteAllMappings() {
    repository.deleteAll()
  }
}

fun ScheduledPaymentMappingDto.fromDto() = ScheduledPaymentMapping(
  dpsId = dpsId,
  nomisId = nomisScheduledPaymentId,
  label = label,
  mappingType = mappingType,
)

private fun ScheduledPaymentMapping.toDto() = ScheduledPaymentMappingDto(
  dpsId = dpsId,
  nomisScheduledPaymentId = nomisId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
