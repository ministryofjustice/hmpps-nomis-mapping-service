package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.courtsentencing

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class CourtCasePrisonerMigration(

  @Id
  val offenderNo: String,

  val count: Int = 0,

  /**
   * ISO timestamp of migration batch job
   */
  val label: String? = null,

  val mappingType: CourtCaseMappingType = CourtCaseMappingType.MIGRATED,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<String> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CourtCasePrisonerMigration) return false

    if (offenderNo != other.offenderNo) return false

    return true
  }

  override fun hashCode(): Int = offenderNo.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): String = offenderNo
}
