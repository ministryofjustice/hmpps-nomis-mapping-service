package uk.gov.justice.digital.hmpps.nomismappingservice.property

import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PropertyContainerMappingRepository : CoroutineCrudRepository<PropertyContainerMapping, UUID> {
  suspend fun findOneByNomisPropertyContainerId(nomisPropertyContainerId: Long): PropertyContainerMapping?
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: PropertyContainerMappingType): PropertyContainerMapping?

  @Modifying
  suspend fun deleteByNomisPropertyContainerId(nomisPropertyContainerId: Long): Int

  @Modifying
  @Query("UPDATE PROPERTY_CONTAINER_MAPPING SET offender_no = :toOffenderNo WHERE offender_no = :fromOffenderNo")
  suspend fun updateOffenderNo(fromOffenderNo: String, toOffenderNo: String): Int

  @Query("UPDATE PROPERTY_CONTAINER_MAPPING SET offender_no = :toOffenderNo WHERE booking_id = :bookingId returning *")
  suspend fun updateOffenderNoByBooking(bookingId: Long, toOffenderNo: String): List<PropertyContainerMapping>

  suspend fun countAllByLabel(migrationId: String): Long
}
