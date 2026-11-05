package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.aliasidentifier

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CorePersonAliasService(
  private val offenderAliasMappingRepository: OffenderAliasMappingRepository,
) {
  suspend fun getOffenderAliasMappingByNomisId(nomisOffenderId: Long) = offenderAliasMappingRepository
    .findOneByNomisOffenderId(nomisOffenderId = nomisOffenderId)
    ?.toDto()
    ?: throw NotFoundException("No offender alias mapping found for nomisOffenderId=$nomisOffenderId")

  suspend fun getOffenderAliasMappingByCprId(cprId: String) = getOffenderAliasMappingByCprIdOrNull(cprId)
    ?: throw NotFoundException("No offender alias mapping found for cprId=$cprId")

  suspend fun getOffenderAliasMappingByCprIdOrNull(cprId: String) = offenderAliasMappingRepository
    .findOneByCprId(cprId = cprId)
    ?.toDto()

  @Transactional
  suspend fun deleteOffenderAliasMappingByNomisId(nomisOffenderId: Long) = offenderAliasMappingRepository
    .deleteByNomisOffenderId(nomisOffenderId = nomisOffenderId)

  @Transactional
  suspend fun createOffenderAliasMapping(mapping: OffenderAliasMappingDto) = offenderAliasMappingRepository
    .save(mapping.toMapping())
}

private fun OffenderAliasMapping.toDto() = OffenderAliasMappingDto(
  cprId = cprId,
  nomisOffenderId = nomisOffenderId,
  nomisPrisonNumber = nomisPrisonNumber,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

internal fun OffenderAliasMappingDto.toMapping() = OffenderAliasMapping(
  cprId = cprId,
  nomisOffenderId = nomisOffenderId,
  nomisPrisonNumber = nomisPrisonNumber,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
