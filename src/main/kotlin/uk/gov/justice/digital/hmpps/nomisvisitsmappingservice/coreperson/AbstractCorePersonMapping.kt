package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.coreperson

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

abstract class AbstractCorePersonMappingTyped<T>(
  val nomisPrisonNumber: String,
  val label: String? = null,
  val mappingType: CorePersonMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<T> {

  override fun isNew(): Boolean = new
}

typealias AbstractCorePersonMapping = AbstractCorePersonMappingTyped<String>
