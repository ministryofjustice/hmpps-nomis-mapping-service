package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.religion

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
class ReligionService(
  private val prisonerReligionMappingRepository: ReligionsMappingRepository,
  private val religionMappingRepository: ReligionMappingRepository,
) {
  suspend fun getReligionsMappingByNomisId(nomisPrisonNumber: String) = prisonerReligionMappingRepository
    .findOneByNomisPrisonNumber(nomisPrisonNumber)
    ?.toDto()
    ?: throw NotFoundException("No religions mapping found for nomisPrisonNumber=$nomisPrisonNumber")

  suspend fun getReligionsMappingByCprId(cprId: String) = prisonerReligionMappingRepository.findOneByCprId(cprId)
    ?.toDto()
    ?: throw NotFoundException("No religions mapping found for cprId=$cprId")

  suspend fun deleteReligionsMappingByNomisId(nomisPrisonNumber: String) {
    prisonerReligionMappingRepository.deleteByNomisPrisonNumber(nomisPrisonNumber = nomisPrisonNumber)
  }

  suspend fun getReligionMappingByNomisId(nomisId: Long) = religionMappingRepository.findOneByNomisId(
    nomisId = nomisId,
  )
    ?.toDto()
    ?: throw NotFoundException("No religion mapping found for nomisReligionId=$nomisId")

  suspend fun getReligionMappingByCprId(cprId: String) = religionMappingRepository.findOneByCprId(
    cprId = cprId,
  )
    ?.toDto()
    ?: throw NotFoundException("No religion mapping found for cprId=$cprId")

  suspend fun getReligionsMappingByCprIdOrNull(cprId: String) = prisonerReligionMappingRepository.findOneByCprId(cprId)
    ?.toDto()

  suspend fun getReligionMappingByCprIdOrNull(cprId: String) = religionMappingRepository.findOneByCprId(
    cprId = cprId,
  )
    ?.toDto()

  suspend fun deleteReligionMappingByNomisId(nomisId: Long) {
    religionMappingRepository.deleteByNomisId(
      nomisId = nomisId,
    )
  }

  suspend fun createMappings(mappings: ReligionsMigrationMappingDto) {
    with(mappings) {
      prisonerReligionMappingRepository.save(
        CorePersonReligionsMapping(
          cprId = cprId,
          nomisPrisonNumber = nomisPrisonNumber,
          label = label,
          mappingType = mappingType,
          whenCreated = whenCreated,
        ),
      )

      religions.forEach {
        religionMappingRepository.save(
          CorePersonReligionMapping(
            cprId = it.cprId,
            nomisId = it.nomisId,
            nomisPrisonNumber = it.nomisPrisonNumber,
            label = label,
            mappingType = mappingType,
            whenCreated = whenCreated,
          ),
        )
      }
    }
  }

  suspend fun createReligions(mapping: ReligionsMappingDto) {
    with(mapping) {
      prisonerReligionMappingRepository.save(
        CorePersonReligionsMapping(
          cprId = cprId,
          nomisPrisonNumber = nomisPrisonNumber,
          label = label,
          mappingType = mappingType,
          whenCreated = whenCreated,
        ),
      )
    }
  }
  suspend fun createReligion(mapping: ReligionMappingDto) {
    with(mapping) {
      religionMappingRepository.save(
        CorePersonReligionMapping(
          cprId = cprId,
          nomisId = nomisId,
          nomisPrisonNumber = nomisPrisonNumber,
          label = label,
          mappingType = mappingType,
          whenCreated = whenCreated,
        ),
      )
    }
  }

  suspend fun getReligionsMappingsByMigrationId(
    pageRequest: Pageable,
    migrationId: String,
  ): Page<ReligionsMappingDto> = coroutineScope {
    val mappings = async {
      prisonerReligionMappingRepository.findAllByLabelOrderByLabelDesc(
        label = migrationId,
        pageRequest = pageRequest,
      )
    }

    val count = async {
      prisonerReligionMappingRepository.countAllByLabel(
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
    prisonerReligionMappingRepository.deleteAll()
    religionMappingRepository.deleteAll()
  }
}

private fun CorePersonReligionsMapping.toDto() = ReligionsMappingDto(
  cprId = cprId,
  nomisPrisonNumber = nomisPrisonNumber,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
private fun CorePersonReligionMapping.toDto() = ReligionMappingDto(
  cprId = cprId,
  nomisId = nomisId,
  nomisPrisonNumber = nomisPrisonNumber,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
