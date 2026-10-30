package uk.gov.justice.digital.hmpps.nomismappingservice.bedassignment

import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BedAssignmentMappingRepository : CoroutineCrudRepository<BedAssignmentMapping, UUID> {
  suspend fun findOneByNomisBookingIdAndNomisSequence(nomisBookingId: Long, sequence: Int): BedAssignmentMapping?
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: BedAssignmentMappingType): BedAssignmentMapping?

  @Modifying
  suspend fun deleteByNomisBookingIdAndNomisSequence(nomisBookingId: Long, sequence: Int): Int

  suspend fun deleteAllByOffenderNo(offenderNo: String)

  suspend fun findAllByOffenderNoOrderByNomisBookingIdAscNomisSequenceAsc(offenderNo: String): List<BedAssignmentMapping>

  @Modifying
  @Query("UPDATE BED_ASSIGNMENT_MAPPING SET offender_no = :toOffenderNo WHERE offender_no = :fromOffenderNo")
  suspend fun updateOffenderNo(fromOffenderNo: String, toOffenderNo: String): Int

  @Query("UPDATE BED_ASSIGNMENT_MAPPING SET offender_no = :toOffenderNo WHERE nomis_booking_id = :bookingId AND offender_no = :fromOffenderNo returning *")
  suspend fun updateOffenderNoByBooking(bookingId: Long, fromOffenderNo: String, toOffenderNo: String): List<BedAssignmentMapping>
}
