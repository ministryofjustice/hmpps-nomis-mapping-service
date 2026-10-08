package uk.gov.justice.digital.hmpps.nomismappingservice.movements.prisonerlocation.migration

import org.springframework.stereotype.Service

@Service
class PrisonerLocationMigrationService {

  @Suppress("UNUSED_PARAMETER")
  suspend fun createMigrationMappings(mappings: PrisonerLocationsMappingDto) {
    // TODO SDIT-4314 implement in next PR
  }
}
