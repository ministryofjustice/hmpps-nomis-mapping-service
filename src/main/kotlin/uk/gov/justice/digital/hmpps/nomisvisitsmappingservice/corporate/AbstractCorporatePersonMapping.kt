package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.corporate

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

abstract class AbstractCorporateMappingTyped<T>(
  val label: String? = null,
  val mappingType: CorporateMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<T> {

  override fun isNew(): Boolean = new
}

typealias AbstractCorporateMapping = AbstractCorporateMappingTyped<String>
