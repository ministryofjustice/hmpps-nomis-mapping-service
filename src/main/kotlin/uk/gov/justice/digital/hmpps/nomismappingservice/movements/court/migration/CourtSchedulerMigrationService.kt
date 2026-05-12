package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.migration

import org.springframework.stereotype.Service

@Service
class CourtSchedulerMigrationService {

  suspend fun createMigrationMappings(mappings: CourtSchedulerPrisonerMappingsDto) {}
}
