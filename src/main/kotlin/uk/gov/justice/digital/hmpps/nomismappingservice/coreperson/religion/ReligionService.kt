package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.religion

import kotlinx.coroutines.flow.collect
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException

@Service
@Transactional
class ReligionService(
  private val religionMappingRepository: ReligionMappingRepository,
) {

  suspend fun existsReligionMappingByNomisPrisonNumber(nomisPrisonNumber: String) = religionMappingRepository
    .existsByNomisPrisonNumber(nomisPrisonNumber)

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

  suspend fun getReligionMappingsByCprIds(cprIds: List<String>) = religionMappingRepository.findByCprIdIn(
    cprIds = cprIds,
  ).map { it.toDto() }

  suspend fun getReligionMappingByCprIdOrNull(cprId: String) = religionMappingRepository.findOneByCprId(
    cprId = cprId,
  )
    ?.toDto()

  suspend fun replaceMappings(mappings: ReligionsMigrationMappingDto) {
    with(mappings) {
      religionMappingRepository.deleteAllByNomisPrisonNumber(nomisPrisonNumber)
      // also ensure that our belief ids aren't mapped to a different prisoner (due to a merge)
      religionMappingRepository.deleteAllById(religions.map { it.nomisId })

      religionMappingRepository.saveAll(
        religions.map {
          CorePersonReligionMapping(
            cprId = it.cprId,
            nomisId = it.nomisId,
            nomisPrisonNumber = it.nomisPrisonNumber,
            label = label,
            mappingType = mappingType,
            whenCreated = whenCreated,
          )
        },
      ).collect()
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

  suspend fun deleteAllMappings() {
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
