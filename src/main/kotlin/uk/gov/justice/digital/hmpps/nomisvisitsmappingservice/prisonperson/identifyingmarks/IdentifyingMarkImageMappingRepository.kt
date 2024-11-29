package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson.identifyingmarks

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface IdentifyingMarkImageMappingRepository : CoroutineCrudRepository<IdentifyingMarkImageMapping, Long> {
  suspend fun findByDpsId(dpsId: UUID): IdentifyingMarkImageMapping?
}
