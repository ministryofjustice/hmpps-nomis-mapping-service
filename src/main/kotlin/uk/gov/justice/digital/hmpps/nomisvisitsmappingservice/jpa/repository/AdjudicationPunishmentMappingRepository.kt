package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AdjudicationPunishmentMapping

@Repository
interface AdjudicationPunishmentMappingRepository : CoroutineCrudRepository<AdjudicationPunishmentMapping, String> {
  suspend fun deleteByLabel(label: String)
}
