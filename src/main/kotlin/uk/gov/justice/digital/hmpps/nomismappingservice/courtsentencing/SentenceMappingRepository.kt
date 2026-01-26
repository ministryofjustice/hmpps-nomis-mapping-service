package uk.gov.justice.digital.hmpps.nomismappingservice.courtsentencing

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface SentenceMappingRepository : CoroutineCrudRepository<SentenceMapping, String> {
  suspend fun findByNomisBookingIdAndNomisSentenceSequence(nomisBookingId: Long, nomisSentenceSeq: Int): SentenceMapping?
  suspend fun deleteByNomisBookingIdAndNomisSentenceSequence(nomisBookingId: Long, nomisSentenceSeq: Int)
  suspend fun deleteByWhenCreatedAfter(dateTime: LocalDateTime)
}
