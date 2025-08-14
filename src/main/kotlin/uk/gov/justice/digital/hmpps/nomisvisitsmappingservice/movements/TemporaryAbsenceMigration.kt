package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.movements

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class TemporaryAbsenceMigration(

  @Id
  val offenderNo: String,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,
) : Persistable<String> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is TemporaryAbsenceApplicationMapping) return false

    return offenderNo != other.offenderNo
  }

  override fun hashCode(): Int = offenderNo.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): String = offenderNo
}
