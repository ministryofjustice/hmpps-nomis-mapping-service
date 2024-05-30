package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SentenceMappingRepository : CoroutineCrudRepository<SentenceMapping, String> {
  suspend fun findByNomisBookingIdAndNomisSentenceSequence(nomisBookingId: Long, nomisSentenceSeq: Int): SentenceMapping?
  suspend fun deleteByNomisBookingIdAndNomisSentenceSequence(nomisBookingId: Long, nomisSentenceSeq: Int)
}
