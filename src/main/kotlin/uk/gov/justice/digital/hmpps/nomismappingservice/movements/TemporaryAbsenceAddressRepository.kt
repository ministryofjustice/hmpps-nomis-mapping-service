package uk.gov.justice.digital.hmpps.nomismappingservice.movements

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TemporaryAbsenceAddressRepository : CoroutineCrudRepository<TemporaryAbsenceAddressMapping, Long> {
  suspend fun findByNomisAddressIdAndNomisAddressOwnerClassAndNomisOffenderNo(nomisAddressId: Long, nomisAddressOwnerClass: String, nomisOffenderNo: String?): TemporaryAbsenceAddressMapping?
  suspend fun findFirstByNomisOffenderNoAndDpsUprnAndDpsAddressText(offenderNo: String, dpsUprn: Long?, dpsAddressText: String): TemporaryAbsenceAddressMapping?
  suspend fun findFirstByNomisAddressOwnerClassAndDpsUprnAndDpsAddressText(ownerClass: String, dpsUprn: Long?, dpsAddressText: String): TemporaryAbsenceAddressMapping?
  suspend fun findFirstByNomisOffenderNoAndNomisAddressId(offenderNo: String, nomisAddressId: Long): TemporaryAbsenceAddressMapping?
  suspend fun findFirstByNomisAddressOwnerClassAndNomisAddressId(ownerClass: String, nomisAddressId: Long): TemporaryAbsenceAddressMapping?
}
