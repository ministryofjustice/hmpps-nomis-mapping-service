package uk.gov.justice.digital.hmpps.nomismappingservice.bedassignment

import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BedAssignmentMappingRepository : CoroutineCrudRepository<BedAssignmentMapping, UUID> {
  suspend fun findOneByNomisBookingIdAndNomisSequence(nomisBookingId: Long, sequence: Int): BedAssignmentMapping?
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: BedAssignmentMappingType): BedAssignmentMapping?

  @Modifying
  suspend fun deleteByNomisBookingIdAndNomisSequence(nomisBookingId: Long, sequence: Int): Int

  @Modifying
  suspend fun deleteAllByNomisBookingIdIn(nomisBookingIds: Collection<Long>): Int
}
