package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CSIPAttendeeMappingRepository : CoroutineCrudRepository<CSIPAttendeeMapping, String> {
  suspend fun findOneByNomisCSIPAttendeeId(nomisCSIPAttendeeId: Long): CSIPAttendeeMapping?
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: CSIPAttendeeMappingType): CSIPAttendeeMapping?
  suspend fun countAllByLabelAndMappingType(label: String, mappingType: CSIPAttendeeMappingType): Long
  fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: CSIPAttendeeMappingType, pageable: Pageable): Flow<CSIPAttendeeMapping>
  suspend fun deleteByMappingTypeEquals(mappingType: CSIPAttendeeMappingType)
  suspend fun deleteByDpsCSIPReportId(dpsCSIPReportId: String)
  suspend fun findAllByDpsCSIPReportId(dpsCSIPReportId: String): List<CSIPAttendeeMapping>
}