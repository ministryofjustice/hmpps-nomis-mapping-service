package uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.application

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TapApplicationRepository : CoroutineCrudRepository<TapApplicationMapping, UUID> {
  suspend fun findByNomisApplicationId(nomisApplicationId: Long): TapApplicationMapping?
  suspend fun findByBookingId(bookingId: Long): List<TapApplicationMapping>
  suspend fun findByOffenderNo(offenderNo: String): List<TapApplicationMapping>
  suspend fun deleteByNomisApplicationId(nomisApplicationId: Long)
  suspend fun deleteByOffenderNo(offenderNo: String)
}
