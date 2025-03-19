package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.casenotes

import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CaseNoteMappingRepository : CoroutineCrudRepository<CaseNoteMapping, Long> {
  suspend fun findByDpsCaseNoteId(dpsCaseNoteId: UUID): List<CaseNoteMapping>
  suspend fun findByNomisCaseNoteIdIn(nomisCaseNoteIds: List<Long>): List<CaseNoteMapping>
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: CaseNoteMappingType): CaseNoteMapping?

  @Query(
    """select count(distinct m.OFFENDER_NO) from CASE_NOTE_MAPPING m 
    where m.LABEL = :label and m.MAPPING_TYPE = :mappingType""",
  )
  suspend fun countDistinctPrisoners(label: String, mappingType: CaseNoteMappingType): Long

  // plain distinct count: 814264 in 3m30s

  // plain count(*) 108976723 takes 38s

  @Modifying
  suspend fun deleteByMappingTypeEquals(mappingType: CaseNoteMappingType): CaseNoteMapping?

  @Modifying
  suspend fun deleteByDpsCaseNoteId(dpsCaseNoteId: UUID)

  @Modifying
  suspend fun deleteAllByOffenderNo(offenderNo: String)
  suspend fun findAllByOffenderNoOrderByNomisBookingIdAscNomisCaseNoteIdAsc(offenderNo: String): List<CaseNoteMapping>

  @Modifying
  @Query("UPDATE CASE_NOTE_MAPPING SET offender_no = :toOffenderNo WHERE offender_no = :fromOffenderNo")
  suspend fun updateOffenderNo(fromOffenderNo: String, toOffenderNo: String): Int

  @Query("UPDATE CASE_NOTE_MAPPING SET offender_no = :toOffenderNo WHERE nomis_booking_id = :bookingId returning *")
  suspend fun updateOffenderNoByBooking(bookingId: Long, toOffenderNo: String): List<CaseNoteMapping>

  @Modifying
  @Query("UPDATE CASE_NOTE_MAPPING SET offender_no = :toOffenderNo WHERE nomis_case_note_id = :nomisId")
  suspend fun updateOffenderNoById(toOffenderNo: String, nomisId: Long): Int
}
