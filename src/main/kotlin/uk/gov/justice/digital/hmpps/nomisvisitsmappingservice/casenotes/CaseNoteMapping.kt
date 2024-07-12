package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.casenotes

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime
import java.util.UUID

data class CaseNoteMapping(

  @Id
  val dpsCaseNoteId: UUID,

  val nomisCaseNoteId: Long,

  val offenderNo: String,

  val nomisBookingId: Long,

  /**
   * ISO timestamp of batch job if a migration
   */
  val label: String? = null,

  val mappingType: CaseNoteMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,
) : Persistable<UUID> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CaseNoteMapping) return false

    return dpsCaseNoteId != other.dpsCaseNoteId
  }

  override fun hashCode(): Int {
    return dpsCaseNoteId.hashCode()
  }

  override fun isNew(): Boolean = new

  override fun getId(): UUID = dpsCaseNoteId
}

enum class CaseNoteMappingType {
  MIGRATED,
  NOMIS_CREATED,
  DPS_CREATED,
}
