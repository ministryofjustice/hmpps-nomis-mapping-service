package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AppointmentMapping
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.AppointmentMappingType

@Repository
interface AppointmentMappingRepository : CoroutineCrudRepository<AppointmentMapping, Long> {
  suspend fun findOneByNomisEventId(nomisEventId: Long): AppointmentMapping?

  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: AppointmentMappingType): AppointmentMapping?

  suspend fun countAllByLabelAndMappingType(label: String, mappingType: AppointmentMappingType): Long

  fun findAllByLabelAndMappingTypeOrderByLabelDesc(
    label: String,
    mappingType: AppointmentMappingType,
    pageable: Pageable,
  ): Flow<AppointmentMapping>
}
