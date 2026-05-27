package uk.gov.justice.digital.hmpps.nomismappingservice.users

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class UserService(
  private val repository: UserMappingRepository,
) {
  suspend fun getMappingByNomisId(nomisId: Long) = repository.findOneByNomisId(nomisId)
    ?.toDto()
    ?: throw NotFoundException("No staff user mapping found for nomisUserId=$nomisId")

  suspend fun getMappingByDpsId(dpsId: String) = repository.findOneByDpsId(dpsId)
    ?.toDto()
    ?: throw NotFoundException("No staff user mapping found for dpsUserId=$dpsId")

  @Transactional
  suspend fun createMapping(mapping: UserMappingDto) {
    repository.save(mapping.fromDto())
  }

  @Transactional
  suspend fun deleteMappingByDpsId(dpsId: String) = repository.deleteById(dpsId)

  @Transactional
  suspend fun deleteAllMappings() {
    repository.deleteAll()
  }
}

fun UserMappingDto.fromDto() = UserMapping(
  dpsId = dpsId,
  nomisId = nomisId,
  label = label,
  mappingType = mappingType,
)

private fun UserMapping.toDto() = UserMappingDto(
  dpsId = dpsId,
  nomisId = nomisId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
