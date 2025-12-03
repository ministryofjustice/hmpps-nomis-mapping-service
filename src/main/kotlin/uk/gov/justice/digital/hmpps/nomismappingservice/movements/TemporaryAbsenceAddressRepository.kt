package uk.gov.justice.digital.hmpps.nomismappingservice.movements

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TemporaryAbsenceAddressRepository : CoroutineCrudRepository<TemporaryAbsenceAddressMapping, Long> {
  suspend fun findByNomisAddressIdAndNomisAddressOwnerClassAndNomisOffenderNo(nomisAddressId: Long, nomisAddressOwnerClass: String, nomisOffenderNo: String?): TemporaryAbsenceAddressMapping?
}
