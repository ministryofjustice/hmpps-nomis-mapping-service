package uk.gov.justice.digital.hmpps.nomismappingservice.courtsentencing

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface SentenceTermMappingRepository : CoroutineCrudRepository<SentenceTermMapping, String> {
  suspend fun findByNomisBookingIdAndNomisSentenceSequenceAndNomisTermSequence(nomisBookingId: Long, nomisSentenceSeq: Int, nomisTermSeq: Int): SentenceTermMapping?
  suspend fun deleteByNomisBookingIdAndNomisSentenceSequenceAndNomisTermSequence(nomisBookingId: Long, nomisSentenceSeq: Int, nomisTermSeq: Int)
  suspend fun deleteByWhenCreatedAfter(dateTime: LocalDateTime)
}
