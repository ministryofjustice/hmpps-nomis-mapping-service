package uk.gov.justice.digital.hmpps.nomismappingservice.officialvisits

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
@Transactional
class OfficialVisitsService(
  private val officialVisitMappingRepository: OfficialVisitMappingRepository,
  private val visitorMappingRepository: VisitorMappingRepository,
) {
  suspend fun getOfficialVisitMappingByNomisId(nomisId: Long) = officialVisitMappingRepository.findOneByNomisId(
    nomisId = nomisId,
  )
    ?.toDto()
    ?: throw NotFoundException("No visit mapping found for nomisId=$nomisId")

  suspend fun getOfficialVisitorMappingByNomisId(nomisId: Long) = visitorMappingRepository.findOneByNomisId(
    nomisId = nomisId,
  )
    ?.toDto()
    ?: throw NotFoundException("No visitor mapping found for nomisId=$nomisId")

  suspend fun getOfficialVisitMappingByDpsIdOrNull(dpsId: String) = officialVisitMappingRepository.findOneByDpsId(dpsId)
    ?.toDto()

  suspend fun getOfficialVisitorMappingByDpsIdOrNull(dpsId: String) = visitorMappingRepository.findOneByDpsId(dpsId)
    ?.toDto()

  suspend fun getOfficialVisitorMappingByDpsId(dpsId: String) = getOfficialVisitorMappingByDpsIdOrNull(dpsId)
    ?: throw NotFoundException("No visitor mapping found for dpsId=$dpsId")

  suspend fun getOfficialVisitMappingByDpsId(dpsId: String) = getOfficialVisitMappingByDpsIdOrNull(dpsId)
    ?: throw NotFoundException("No visit mapping found for dpsId=$dpsId")

  suspend fun createMappings(mappings: OfficialVisitMigrationMappingDto) {
    with(mappings) {
      officialVisitMappingRepository.save(
        OfficialVisitMapping(
          dpsId = dpsId,
          nomisId = nomisId,
          label = label,
          mappingType = mappingType,
          whenCreated = whenCreated,
        ),
      )

      visitors.forEach {
        visitorMappingRepository.save(
          VisitorMapping(
            dpsId = it.dpsId,
            nomisId = it.nomisId,
            label = label,
            mappingType = mappingType,
            whenCreated = whenCreated,
          ),
        )
      }
    }
  }
  suspend fun createVisitMapping(mapping: OfficialVisitMappingDto) {
    officialVisitMappingRepository.save(
      with(mapping) {
        OfficialVisitMapping(
          dpsId = dpsId,
          nomisId = nomisId,
          label = label,
          mappingType = mappingType,
          whenCreated = whenCreated,
        )
      },
    )
  }
  suspend fun createVisitorMapping(mapping: OfficialVisitorMappingDto) {
    visitorMappingRepository.save(
      with(mapping) {
        VisitorMapping(
          dpsId = dpsId,
          nomisId = nomisId,
          label = label,
          mappingType = mappingType,
          whenCreated = whenCreated,
        )
      },
    )
  }

  suspend fun getOfficialVisitMappingsByMigrationId(
    pageRequest: Pageable,
    migrationId: String,
  ): Page<OfficialVisitMappingDto> = coroutineScope {
    val mappings = async {
      officialVisitMappingRepository.findAllByLabelOrderByLabelDesc(
        label = migrationId,
        pageRequest = pageRequest,
      )
    }

    val count = async {
      officialVisitMappingRepository.countAllByLabel(
        migrationId = migrationId,
      )
    }

    PageImpl(
      mappings.await().toList().map { it.toDto() },
      pageRequest,
      count.await(),
    )
  }

  suspend fun deleteAllMappings() {
    officialVisitMappingRepository.deleteAll()
    visitorMappingRepository.deleteAll()
  }

  suspend fun deleteOfficialVisitMappingByNomisId(nomisId: Long) = officialVisitMappingRepository.deleteByNomisId(nomisId)

  suspend fun deleteOfficialVisitorMappingByNomisId(nomisId: Long) = visitorMappingRepository.deleteByNomisId(nomisId)
}

private fun OfficialVisitMapping.toDto() = OfficialVisitMappingDto(
  dpsId = dpsId,
  nomisId = nomisId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private fun VisitorMapping.toDto() = OfficialVisitorMappingDto(
  dpsId = dpsId,
  nomisId = nomisId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
