package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AdjudicationMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AdjudicationPunishmentMapping

@Repository
interface AdjudicationPunishmentMappingRepository : CoroutineCrudRepository<AdjudicationPunishmentMapping, String> {
  suspend fun deleteByLabel(label: String)
  suspend fun deleteAllByMappingType(adjudicationMappingType: AdjudicationMappingType)
  suspend fun deleteByNomisBookingIdAndNomisSanctionSequence(nomisBookingId: Long, nomisSanctionSequence: Int)
  suspend fun findByNomisBookingIdAndNomisSanctionSequence(nomisBookingId: Long, nomisSanctionSequence: Int): AdjudicationPunishmentMapping?
}
