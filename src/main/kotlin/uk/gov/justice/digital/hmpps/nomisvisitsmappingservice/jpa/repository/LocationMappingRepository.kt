package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.LocationMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.LocationMappingType

@Repository
interface LocationMappingRepository : CoroutineCrudRepository<LocationMapping, String> {
  suspend fun findOneByNomisLocationId(nomisLocationId: Long): LocationMapping?
  suspend fun findAllByNomisLocationIdIn(nomisLocationIds: List<Long>): Flow<LocationMapping>
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: LocationMappingType): LocationMapping?
  suspend fun countAllByLabelAndMappingType(label: String, mappingType: LocationMappingType): Long
  fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: LocationMappingType, pageable: Pageable): Flow<LocationMapping>
  suspend fun deleteByMappingTypeEquals(mappingType: LocationMappingType): LocationMapping?
  suspend fun deleteByNomisLocationId(nomisLocationId: Long)
}
