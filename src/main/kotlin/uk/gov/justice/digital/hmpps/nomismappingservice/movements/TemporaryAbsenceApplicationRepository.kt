package uk.gov.justice.digital.hmpps.nomismappingservice.movements

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TemporaryAbsenceApplicationRepository : CoroutineCrudRepository<TemporaryAbsenceApplicationMapping, UUID> {
  suspend fun findByNomisApplicationId(nomisApplicationId: Long): TemporaryAbsenceApplicationMapping?
  suspend fun findByBookingId(bookingId: Long): List<TemporaryAbsenceApplicationMapping>
  suspend fun deleteByNomisApplicationId(nomisApplicationId: Long)
  suspend fun deleteByOffenderNo(offenderNo: String)
}
