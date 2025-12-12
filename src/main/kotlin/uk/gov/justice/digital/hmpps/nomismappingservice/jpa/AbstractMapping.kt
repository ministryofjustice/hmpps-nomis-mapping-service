package uk.gov.justice.digital.hmpps.nomismappingservice.jpa

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import java.time.LocalDateTime

abstract class AbstractMappingTyped<T : Any>(
  val label: String? = null,
  val mappingType: StandardMappingType,

  @Transient
  @Value("false")
  val new: Boolean = true,

  val whenCreated: LocalDateTime? = null,

) : Persistable<T> {

  override fun isNew(): Boolean = new
}

enum class StandardMappingType {
  MIGRATED,
  DPS_CREATED,
  NOMIS_CREATED,
}
