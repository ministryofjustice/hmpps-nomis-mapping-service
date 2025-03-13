package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson.profiledetails

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

data class ContactPersonProfileDetailMigrationMapping(

  val nomisPrisonerNumber: String,

  val label: String,

  var domesticStatusDpsIds: String,

  var numberOfChildrenDpsIds: String,

  @CreatedDate
  val whenCreated: LocalDateTime? = null,

  @Transient
  @Value("false")
  val new: Boolean = true,
) : Persistable<String> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ContactPersonProfileDetailMigrationMapping) return false

    if (id != other.id) return false

    return true
  }

  override fun hashCode(): Int = id.hashCode()

  override fun isNew(): Boolean = new

  override fun getId(): String = nomisPrisonerNumber + label
}
