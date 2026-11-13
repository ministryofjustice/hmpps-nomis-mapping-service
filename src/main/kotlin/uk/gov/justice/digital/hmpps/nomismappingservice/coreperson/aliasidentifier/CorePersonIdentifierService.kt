package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.aliasidentifier

import jakarta.validation.ValidationException
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

  suspend fun getOffenderIdentifierMappingsByNomisOffenderId(nomisOffenderId: Long) = offenderIdentifierMappingRepository
    .findAllByNomisOffenderIdIn(nomisOffenderIds = listOf(nomisOffenderId))
    .map { it.toDto() }

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

  @Transactional
  suspend fun replaceOffenderIdentifierMappings(nomisOffenderId: Long, mappings: List<OffenderIdentifierMappingDto>) {
    mappings.filter { it.nomisOffenderId != nomisOffenderId }.takeIf { it.isNotEmpty() }?.let { mismatched ->
      throw ValidationException("All mappings must be for nomisOffenderId=$nomisOffenderId but found ${mismatched.map { it.nomisOffenderId }.distinct()}")
    }
    offenderIdentifierMappingRepository.deleteAllByNomisOffenderId(nomisOffenderId)
    // Remove any dangling mappings held against another offender for the same CPR ids
    mappings.forEach { offenderIdentifierMappingRepository.deleteById(it.cprId) }
    mappings.forEach { offenderIdentifierMappingRepository.save(it.toMapping()) }
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
