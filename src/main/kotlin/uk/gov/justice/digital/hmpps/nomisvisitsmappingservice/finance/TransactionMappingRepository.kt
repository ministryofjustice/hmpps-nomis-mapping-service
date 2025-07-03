package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.finance

import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TransactionMappingRepository : CoroutineCrudRepository<TransactionMapping, Long> {
  suspend fun findByDpsTransactionId(dpsTransactionId: UUID): TransactionMapping?
  suspend fun findByNomisTransactionIdIn(nomisTransactionIds: List<Long>): List<TransactionMapping>
  suspend fun findFirstByMappingTypeOrderByWhenCreatedDesc(mappingType: TransactionMappingType): TransactionMapping?

  @Modifying
  suspend fun deleteByDpsTransactionId(dpsTransactionId: UUID)

  suspend fun findAllByOffenderNoOrderByNomisBookingIdAscNomisTransactionIdAsc(offenderNo: String): List<TransactionMapping>

  @Modifying
  @Query("UPDATE TRANSACTION_MAPPING SET offender_no = :toOffenderNo WHERE offender_no = :fromOffenderNo")
  suspend fun updateOffenderNo(fromOffenderNo: String, toOffenderNo: String): Int

  @Query("UPDATE TRANSACTION_MAPPING SET offender_no = :toOffenderNo WHERE nomis_booking_id = :bookingId returning *")
  suspend fun updateOffenderNoByBooking(bookingId: Long, toOffenderNo: String): List<TransactionMapping>

  @Modifying
  @Query("UPDATE TRANSACTION_MAPPING SET offender_no = :toOffenderNo WHERE nomis_transaction_id = :nomisId")
  suspend fun updateOffenderNoById(toOffenderNo: String, nomisId: Long): Int
}
