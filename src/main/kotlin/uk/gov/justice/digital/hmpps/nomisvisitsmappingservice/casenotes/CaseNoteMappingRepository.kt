package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.casenotes

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CaseNoteMappingRepository : CoroutineCrudRepository<CaseNoteMapping, String> {
  suspend fun findOneByNomisCaseNoteId(nomisCaseNoteId: Long): CaseNoteMapping?
  suspend fun findByNomisCaseNoteIdIn(nomisCaseNoteIds: List<Long>): List<CaseNoteMapping>
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: CaseNoteMappingType): CaseNoteMapping?

  @Query(
    """select count(distinct m.OFFENDER_NO) from CASE_NOTE_MAPPING m 
    where m.LABEL = :label and m.MAPPING_TYPE = :mappingType""",
  )
  suspend fun countDistinctPrisoners(label: String, mappingType: CaseNoteMappingType): Long

  // plain distinct count: 814264 in 3m30s

  // plain count(*) 108976723 takes 38s

  suspend fun deleteByMappingTypeEquals(mappingType: CaseNoteMappingType): CaseNoteMapping?
  suspend fun deleteByNomisCaseNoteId(nomisCaseNoteId: Long)
  suspend fun deleteAllByOffenderNo(offenderNo: String)
  suspend fun findAllByOffenderNoOrderByNomisBookingIdAscNomisCaseNoteIdAsc(offenderNo: String): List<CaseNoteMapping>
}
