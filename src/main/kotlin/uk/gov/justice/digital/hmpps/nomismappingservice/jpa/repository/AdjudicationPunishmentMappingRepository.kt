package uk.gov.justice.digital.hmpps.nomismappingservice.jpa.repository

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.AdjudicationPunishmentMapping

@Repository
interface AdjudicationPunishmentMappingRepository : CoroutineCrudRepository<AdjudicationPunishmentMapping, String> {
  suspend fun deleteByNomisBookingIdAndNomisSanctionSequence(nomisBookingId: Long, nomisSanctionSequence: Int)
  suspend fun findByNomisBookingIdAndNomisSanctionSequence(nomisBookingId: Long, nomisSanctionSequence: Int): AdjudicationPunishmentMapping?
}
