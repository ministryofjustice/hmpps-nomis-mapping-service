package uk.gov.justice.digital.hmpps.nomismappingservice.property

import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PropertyContainerMappingRepository : CoroutineCrudRepository<PropertyContainerMapping, UUID> {
  suspend fun findOneByNomisPropertyContainerId(nomisPropertyContainerId: Long): PropertyContainerMapping?
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: PropertyContainerMappingType): PropertyContainerMapping?

  @Modifying
  suspend fun deleteByNomisPropertyContainerId(nomisPropertyContainerId: Long): Int

  suspend fun countAllByLabel(migrationId: String): Long
}
