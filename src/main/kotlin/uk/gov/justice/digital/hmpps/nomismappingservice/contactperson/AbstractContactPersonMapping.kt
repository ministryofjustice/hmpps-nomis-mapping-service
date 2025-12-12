package uk.gov.justice.digital.hmpps.nomismappingservice.contactperson

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

abstract class AbstractContactPersonMappingTyped<T : Any>(
  val label: String? = null,
  val mappingType: ContactPersonMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<T> {

  override fun isNew(): Boolean = new
}

typealias AbstractContactPersonMapping = AbstractContactPersonMappingTyped<String>
