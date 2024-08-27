package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans.CSIPPlanMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans.CSIPPlanMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CSIPMappingService(
  private val csipMappingRepository: CSIPMappingRepository,
  private val csipPlanMappingRepository: CSIPPlanMappingRepository,
) {

  @Transactional
  suspend fun createCSIPMapping(mapping: CSIPMappingDto) =
    csipMappingRepository.save(mapping.fromDto())

  suspend fun getMappingByNomisCSIPId(nomisCSIPId: Long): CSIPMappingDto =
    csipMappingRepository.findOneByNomisCSIPId(
      nomisCSIPId = nomisCSIPId,
    )
      ?.toDto()
      ?: throw NotFoundException("No CSIP mapping found for nomisCSIPId=$nomisCSIPId")

  suspend fun getMappingByDPSCSIPId(dpsCSIPId: String): CSIPMappingDto =
    csipMappingRepository.findById(dpsCSIPId)
      ?.toDto()
      ?: throw NotFoundException("No CSIP mapping found for dpsCSIPId=$dpsCSIPId")

  @Transactional
  suspend fun deleteMappingByDPSId(dpsCSIPId: String) {
    deleteChildMappingsByDPSId(dpsCSIPId)
    csipMappingRepository.deleteById(dpsCSIPId)
  }

  @Transactional
  suspend fun deleteMigratedChildren() {
    csipPlanMappingRepository.deleteByMappingTypeEquals(CSIPPlanMappingType.MIGRATED)
  }

  @Transactional
  suspend fun deleteAllChildren() {
    csipPlanMappingRepository.deleteAll()
  }

  @Transactional
  suspend fun deleteChildMappingsByDPSId(dpsCSIPId: String) {
    csipPlanMappingRepository.deleteByDpsCSIPReportId(dpsCSIPId)
  }

  @Transactional
  suspend fun deleteMappings(onlyMigrated: Boolean) =
    onlyMigrated.takeIf { it }?.apply {
      // The status of the child mapping will match the top level report status
      deleteMigratedChildren()
      csipMappingRepository.deleteByMappingTypeEquals(CSIPMappingType.MIGRATED)
    } ?: run {
      deleteAllChildren()
      csipMappingRepository.deleteAll()
    }

  suspend fun getByMigrationId(pageRequest: Pageable, migrationId: String): Page<CSIPMappingDto> =
    coroutineScope {
      val csipMapping = async {
        csipMappingRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
          label = migrationId,
          CSIPMappingType.MIGRATED,
          pageRequest,
        )
      }

      val count = async {
        csipMappingRepository.countAllByLabelAndMappingType(migrationId, mappingType = CSIPMappingType.MIGRATED)
      }

      PageImpl(
        csipMapping.await().toList().map { it.toDto() },
        pageRequest,
        count.await(),
      )
    }

  suspend fun getMappingForLatestMigrated(): CSIPMappingDto =
    csipMappingRepository.findFirstByMappingTypeOrderByWhenCreatedDesc(CSIPMappingType.MIGRATED)
      ?.toDto()
      ?: throw NotFoundException("No migrated mapping found")
}
fun CSIPMapping.toDto() = CSIPMappingDto(
  dpsCSIPId = dpsCSIPId,
  nomisCSIPId = nomisCSIPId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

fun CSIPMappingDto.fromDto() = CSIPMapping(
  dpsCSIPId = dpsCSIPId,
  nomisCSIPId = nomisCSIPId,
  label = label,
  mappingType = mappingType,
)
