package uk.gov.justice.digital.hmpps.nomismappingservice.finance

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime
import java.util.UUID

data class TransactionMapping(

  @Id
  val nomisTransactionId: Long,

  val dpsTransactionId: UUID,

  val offenderNo: String? = null,

  val nomisBookingId: Long? = null,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: TransactionMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,
) : Persistable<Long> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is TransactionMapping) return false

    return nomisTransactionId != other.nomisTransactionId
  }

  override fun hashCode(): Int = nomisTransactionId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): Long = nomisTransactionId
}

enum class TransactionMappingType {
  MIGRATED,
  NOMIS_CREATED,
  DPS_CREATED,
}
