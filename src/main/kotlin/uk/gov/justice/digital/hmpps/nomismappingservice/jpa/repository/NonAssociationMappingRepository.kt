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
  suspend fun findOneByFirstOffenderNoAndSecondOffenderNoAndNomisTypeSequence(
    firstOffenderNo: String,
    secondOffenderNo: String,
    nomisTypeSequence: Int,
  ): NonAssociationMapping?

  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: NonAssociationMappingType): NonAssociationMapping?
  suspend fun countAllByLabelAndMappingType(label: String, mappingType: NonAssociationMappingType): Long
  fun findAllByLabelAndMappingTypeOrderByLabelDesc(
    label: String,
    mappingType: NonAssociationMappingType,
    pageable: Pageable,
  ): Flow<NonAssociationMapping>

  @Modifying
  suspend fun deleteByMappingTypeEquals(mappingType: NonAssociationMappingType): NonAssociationMapping?

  suspend fun findByFirstOffenderNoOrSecondOffenderNo(
    offenderNo1: String,
    offenderNo2: String,
  ): List<NonAssociationMapping>

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

  @Query(
    """SELECT *
    FROM non_association_mapping na1
    WHERE (na1.first_offender_no = :offenderNo1
        and na1.second_offender_no in (select na11.second_offender_no
                                        from non_association_mapping na11
                                        where na11.first_offender_no = :offenderNo2
                                          and na11.nomis_type_sequence = na1.nomis_type_sequence
                                     union
                                        select na12.first_offender_no
                                          from non_association_mapping na12
                                          where na12.second_offender_no = :offenderNo2
                                            and na12.nomis_type_sequence = na1.nomis_type_sequence
                                      )
        )
      or (na1.second_offender_no = :offenderNo1
        and na1.first_offender_no in (select na21.first_offender_no
                                       from non_association_mapping na21
                                       where na21.second_offender_no = :offenderNo2
                                          and na21.nomis_type_sequence = na1.nomis_type_sequence
                                     union
                                       select na22.second_offender_no
                                         from non_association_mapping na22
                                         where na22.first_offender_no = :offenderNo2
                                           and na22.nomis_type_sequence = na1.nomis_type_sequence
                                     )
        )
      or 
        (na1.first_offender_no = :offenderNo2
        and na1.second_offender_no in (select na11.second_offender_no
                                        from non_association_mapping na11
                                        where na11.first_offender_no = :offenderNo1
                                          and na11.nomis_type_sequence = na1.nomis_type_sequence
                                     union
                                        select na12.first_offender_no
                                          from non_association_mapping na12
                                          where na12.second_offender_no = :offenderNo1
                                            and na12.nomis_type_sequence = na1.nomis_type_sequence
                                      )
        )
      or (na1.second_offender_no = :offenderNo2
        and na1.first_offender_no in (select na21.first_offender_no
                                       from non_association_mapping na21
                                       where na21.second_offender_no = :offenderNo1
                                          and na21.nomis_type_sequence = na1.nomis_type_sequence
                                     union
                                       select na22.second_offender_no
                                         from non_association_mapping na22
                                         where na22.first_offender_no = :offenderNo1
                                           and na22.nomis_type_sequence = na1.nomis_type_sequence
                                     )
        )
    """,
  )
  suspend fun findCommonThirdParties(offenderNo1: String, offenderNo2: String): List<NonAssociationMapping>

  @Modifying
  @Query(
    """UPDATE non_association_mapping
       SET nomis_type_sequence = :sequence
       WHERE non_association_id = :nonAssociationId
    """,
  )
  suspend fun resetSequence(nonAssociationId: Long, sequence: Int): Int
}
