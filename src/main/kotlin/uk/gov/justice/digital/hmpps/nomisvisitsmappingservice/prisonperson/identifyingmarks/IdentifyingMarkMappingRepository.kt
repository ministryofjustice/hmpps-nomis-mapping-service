package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface IdentifyingMarkMappingRepository : CoroutineCrudRepository<IdentifyingMarkMapping, String> {
  suspend fun findByNomisBookingIdAndNomisMarksSequence(nomisBookingId: Long, nomisMarksSequence: Long): IdentifyingMarkMapping?
  suspend fun findByDpsId(dpsId: UUID): List<IdentifyingMarkMapping>
}
