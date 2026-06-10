package uk.gov.justice.digital.hmpps.nomismappingservice.staff

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
class StaffService(
  private val repository: StaffMappingRepository,
) {
  suspend fun getMappingByNomisId(nomisId: Long) = repository.findOneByNomisId(nomisId)
    ?.toDto()
    ?: throw NotFoundException("No staff mapping found for nomisStaffId=$nomisId")

  suspend fun getMappingByDpsId(dpsId: String) = repository.findOneByDpsId(dpsId)
    ?.toDto()
    ?: throw NotFoundException("No staff mapping found for dpsId=$dpsId")

  @Transactional
  suspend fun createMapping(mapping: StaffMappingDto) {
    repository.save(mapping.fromDto())
  }

  @Transactional
  suspend fun deleteMappingByDpsId(dpsId: String) = repository.deleteById(dpsId)

  @Transactional
  suspend fun deleteAllMappings() {
    repository.deleteAll()
  }

  suspend fun getMappingsByMigrationId(pageRequest: Pageable, migrationId: String): Page<StaffMappingDto> = coroutineScope {
    val mappings = async {
      repository.findAllByLabelOrderByLabelDesc(
        label = migrationId,
        pageRequest = pageRequest,
      )
    }

    val count = async {
      repository.countAllByLabel(
        migrationId = migrationId,
      )
    }
    PageImpl(
      mappings.await().toList().map { it.toDto() },
      pageRequest,
      count.await(),
    )
  }
}

fun StaffMappingDto.fromDto() = StaffMapping(
  dpsId = dpsId,
  nomisId = nomisId,
  label = label,
  mappingType = mappingType,
)

private fun StaffMapping.toDto() = StaffMappingDto(
  dpsId = dpsId,
  nomisId = nomisId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
