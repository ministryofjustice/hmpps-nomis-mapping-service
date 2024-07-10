package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.prisonperson

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class PrisonPersonMigrationMapping(

  @Id
  val nomisPrisonerNumber: String,

  @CreatedDate
  val whenCreated: LocalDateTime? = null,

  val label: String,

  @Transient
  @Value("false")
  val new: Boolean = true,
) : Persistable<String> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is PrisonPersonMigrationMapping) return false

    if (nomisPrisonerNumber != other.nomisPrisonerNumber) return false

    return true
  }

  override fun hashCode(): Int = nomisPrisonerNumber.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): String = nomisPrisonerNumber
}
