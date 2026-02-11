package uk.gov.justice.digital.hmpps.nomismappingservice.csra

import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CsraMappingRepository : CoroutineCrudRepository<CsraMapping, UUID> {
  suspend fun findOneByNomisBookingIdAndNomisSequence(nomisBookingId: Long, sequence: Int): CsraMapping?
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: CsraMappingType): CsraMapping?

  @Modifying
  suspend fun deleteByNomisBookingIdAndNomisSequence(nomisBookingId: Long, sequence: Int): Int

  suspend fun findAllByOffenderNoOrderByNomisBookingIdAscNomisSequenceAsc(offenderNo: String): List<CsraMapping>

  @Modifying
  @Query("UPDATE CSRA_MAPPING SET offender_no = :toOffenderNo WHERE offender_no = :fromOffenderNo")
  suspend fun updateOffenderNo(fromOffenderNo: String, toOffenderNo: String): Int

  @Query("UPDATE CSRA_MAPPING SET offender_no = :toOffenderNo WHERE nomis_booking_id = :bookingId returning *")
  suspend fun updateOffenderNoByBooking(bookingId: Long, toOffenderNo: String): List<CsraMapping>
}
