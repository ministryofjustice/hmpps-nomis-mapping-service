package uk.gov.justice.digital.hmpps.nomismappingservice.jpa.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.NonAssociationMapping
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.NonAssociationMappingType

@Repository
interface NonAssociationMappingRepository : CoroutineCrudRepository<NonAssociationMapping, Long> {
  suspend fun findOneByFirstOffenderNoAndSecondOffenderNoAndNomisTypeSequence(firstOffenderNo: String, secondOffenderNo: String, nomisTypeSequence: Int): NonAssociationMapping?
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: NonAssociationMappingType): NonAssociationMapping?
  suspend fun countAllByLabelAndMappingType(label: String, mappingType: NonAssociationMappingType): Long
  fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: NonAssociationMappingType, pageable: Pageable): Flow<NonAssociationMapping>

  @Modifying
  suspend fun deleteByMappingTypeEquals(mappingType: NonAssociationMappingType): NonAssociationMapping?

  suspend fun findByFirstOffenderNoOrSecondOffenderNo(offenderNo1: String, offenderNo2: String): List<NonAssociationMapping>

  @Modifying
  @Query("UPDATE non_association_mapping SET first_offender_no = :firstOffenderNo WHERE non_association_id = :nonAssociationId")
  suspend fun updateFirstOffenderNo(nonAssociationId: Long, firstOffenderNo: String): Int

  @Modifying
  @Query("UPDATE non_association_mapping SET second_offender_no = :secondOffenderNo WHERE non_association_id = :nonAssociationId")
  suspend fun updateSecondOffenderNo(nonAssociationId: Long, secondOffenderNo: String): Int

  @Modifying
  @Query(
    """UPDATE non_association_mapping
     SET first_offender_no = :newOffenderNo
     WHERE first_offender_no = :firstOffenderNo AND second_offender_no = :secondOffenderNo""",
  )
  suspend fun updateFirstOffenderNoByOffenderNos(
    firstOffenderNo: String,
    secondOffenderNo: String,
    newOffenderNo: String,
  ): Int

  @Modifying
  @Query(
    """UPDATE non_association_mapping
     SET second_offender_no = :newOffenderNo
     WHERE first_offender_no = :firstOffenderNo AND second_offender_no = :secondOffenderNo""",
  )
  suspend fun updateSecondOffenderNoByOffenderNos(
    firstOffenderNo: String,
    secondOffenderNo: String,
    newOffenderNo: String,
  ): Int
}
