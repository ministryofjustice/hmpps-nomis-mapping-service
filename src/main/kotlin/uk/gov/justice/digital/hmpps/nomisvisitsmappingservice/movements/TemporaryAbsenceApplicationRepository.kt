package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.movements

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TemporaryAbsenceApplicationRepository : CoroutineCrudRepository<TemporaryAbsenceApplicationMapping, UUID> {
  suspend fun findByNomisApplicationId(nomisApplicationId: Long): TemporaryAbsenceApplicationMapping?
}
