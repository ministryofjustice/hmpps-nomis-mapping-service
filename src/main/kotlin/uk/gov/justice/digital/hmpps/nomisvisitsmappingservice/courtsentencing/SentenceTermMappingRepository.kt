package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SentenceTermMappingRepository : CoroutineCrudRepository<SentenceTermMapping, String> {
  suspend fun findByNomisBookingIdAndNomisSentenceSequenceAndNomisTermSequence(nomisBookingId: Long, nomisSentenceSeq: Int, nomisTermSeq: Int): SentenceTermMapping?
  suspend fun deleteByNomisBookingIdAndNomisSentenceSequenceAndNomisTermSequence(nomisBookingId: Long, nomisSentenceSeq: Int, nomisTermSeq: Int)
}
