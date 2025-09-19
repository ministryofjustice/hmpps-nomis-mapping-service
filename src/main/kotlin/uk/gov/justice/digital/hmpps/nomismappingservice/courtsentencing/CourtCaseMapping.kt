package uk.gov.justice.digital.hmpps.nomismappingservice.courtsentencing

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class CourtCaseMapping(

  @Id
  val dpsCourtCaseId: String,

  val nomisCourtCaseId: Long,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: CourtCaseMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<String> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CourtCaseMapping) return false

    return dpsCourtCaseId == other.dpsCourtCaseId
  }

  override fun hashCode(): Int = dpsCourtCaseId.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): String = dpsCourtCaseId
}

enum class CourtCaseMappingType {
  MIGRATED,
  DPS_CREATED,
  NOMIS_CREATED,
}
