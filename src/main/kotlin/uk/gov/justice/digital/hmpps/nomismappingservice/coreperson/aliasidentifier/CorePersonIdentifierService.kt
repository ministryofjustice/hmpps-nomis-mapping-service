package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.aliasidentifier

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CorePersonIdentifierService(
  private val offenderIdentifierMappingRepository: OffenderIdentifierMappingRepository,
) {
  suspend fun getOffenderIdentifierMappingByNomisId(nomisOffenderId: Long, nomisIdentifierSequence: Int) = offenderIdentifierMappingRepository
    .findOneByNomisOffenderIdAndNomisIdentifierSequence(
      nomisOffenderId = nomisOffenderId,
      nomisIdentifierSequence = nomisIdentifierSequence,
    )?.toDto()
    ?: throw NotFoundException("No offender identifier mapping found for nomisOffenderId=$nomisOffenderId and nomisIdentifierSequence=$nomisIdentifierSequence")

  @Transactional
  suspend fun deleteOffenderIdentifierMappingByNomisId(nomisOffenderId: Long, nomisIdentifierSequence: Int) = offenderIdentifierMappingRepository
    .deleteByNomisOffenderIdAndNomisIdentifierSequence(
      nomisOffenderId = nomisOffenderId,
      nomisIdentifierSequence = nomisIdentifierSequence,
    )

  suspend fun getOffenderIdentifierMappingByCprId(cprId: String) = getOffenderIdentifierMappingByCprIdOrNull(cprId)
    ?: throw NotFoundException("No offender identifier mapping found for cprId=$cprId")

  suspend fun getOffenderIdentifierMappingByCprIdOrNull(cprId: String) = offenderIdentifierMappingRepository
    .findOneByCprId(cprId = cprId)
    ?.toDto()

  @Transactional
  suspend fun createOffenderIdentifierMapping(mapping: OffenderIdentifierMappingDto) {
    offenderIdentifierMappingRepository.save(mapping.toMapping())
  }
}

internal fun OffenderIdentifierMapping.toDto() = OffenderIdentifierMappingDto(
  cprId = cprId,
  nomisOffenderId = nomisOffenderId,
  nomisIdentifierSequence = nomisIdentifierSequence,
  nomisPrisonNumber = nomisPrisonNumber,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

internal fun OffenderIdentifierMappingDto.toMapping() = OffenderIdentifierMapping(
  cprId = cprId,
  nomisOffenderId = nomisOffenderId,
  nomisIdentifierSequence = nomisIdentifierSequence,
  nomisPrisonNumber = nomisPrisonNumber,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
