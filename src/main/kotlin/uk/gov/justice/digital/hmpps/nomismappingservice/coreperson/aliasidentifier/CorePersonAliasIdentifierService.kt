package uk.gov.justice.digital.hmpps.nomismappingservice.coreperson.aliasidentifier

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CorePersonAliasIdentifierService(
  private val offenderAliasMappingRepository: OffenderAliasMappingRepository,
  private val offenderIdentifierMappingRepository: OffenderIdentifierMappingRepository,
) {

  @Transactional
  suspend fun replaceMappings(mappings: CorePersonAliasIdentifierMappingsDto) = replaceAliasesAndIdentifiersMappings(
    prisonNumber = mappings.personMapping.nomisPrisonNumber,
    aliases = mappings.aliases,
    identifiers = mappings.identifiers,
  )

  @Transactional
  suspend fun replaceAliasesAndIdentifiersMappings(
    prisonNumber: String,
    aliases: List<OffenderAliasMappingDto>,
    identifiers: List<OffenderIdentifierMappingDto>,
  ) {
    offenderAliasMappingRepository.deleteAllByNomisPrisonNumber(prisonNumber)
    offenderIdentifierMappingRepository.deleteAllByNomisPrisonNumber(prisonNumber)
    // In the event of a merge, it's possible that we need to delete dangling mappings.
    aliases.forEach {
      offenderAliasMappingRepository.deleteByNomisOffenderId(it.nomisOffenderId)
      offenderAliasMappingRepository.deleteById(it.cprId)
    }
    identifiers.forEach {
      offenderIdentifierMappingRepository.deleteByNomisOffenderIdAndNomisIdentifierSequence(it.nomisOffenderId, it.nomisIdentifierSequence)
      offenderIdentifierMappingRepository.deleteById(it.cprId)
    }
    aliases.forEach { offenderAliasMappingRepository.save(it.toMapping()) }
    identifiers.forEach { offenderIdentifierMappingRepository.save(it.toMapping()) }
  }

  suspend fun getMappingResultFromNomisIds(mappingRequest: CorePersonAliasIdentifierMappingsByNomisIdsRequest): CorePersonAliasIdentifierMappingsResult {
    val matchingAliases = offenderAliasMappingRepository.findAllByNomisOffenderIdIn(mappingRequest.aliases).map { it.toDto() }
    val requestedIdentifiers = mappingRequest.identifiers
    val matchingIdentifiers = offenderIdentifierMappingRepository.findAllByNomisOffenderIdIn(requestedIdentifiers.map { it.nomisId })
      .filter { mapping -> requestedIdentifiers.any { it.nomisId == mapping.nomisOffenderId && it.nomisSequence == mapping.nomisIdentifierSequence } }
      .map { it.toDto() }
    return CorePersonAliasIdentifierMappingsResult(
      aliases = matchingAliases,
      identifiers = matchingIdentifiers,
    )
  }

  @Transactional
  suspend fun deleteAll() {
    offenderAliasMappingRepository.deleteAll()
    offenderIdentifierMappingRepository.deleteAll()
  }
}
