package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class ContactPersonService(
  private val personMappingRepository: PersonMappingRepository,
  private val personAddressMappingRepository: PersonAddressMappingRepository,
  private val personPhoneMappingRepository: PersonPhoneMappingRepository,
  private val personEmailMappingRepository: PersonEmailMappingRepository,
  private val personEmploymentMappingRepository: PersonEmploymentMappingRepository,
  private val personIdentifierMappingRepository: PersonIdentifierMappingRepository,
  private val personRestrictionMappingRepository: PersonRestrictionMappingRepository,
  private val personContactMappingRepository: PersonContactMappingRepository,
  private val personContactRestrictionMappingRepository: PersonContactRestrictionMappingRepository,
) {
  suspend fun getPersonMappingByNomisId(nomisId: Long) =
    personMappingRepository.findOneByNomisId(nomisId = nomisId)
      ?.toDto()
      ?: throw NotFoundException("No alert mapping found for nomisId=$nomisId")

  suspend fun getPersonMappingByDpsId(dpsId: String) =
    personMappingRepository.findOneByDpsId(dpsId = dpsId)
      ?.toDto()
      ?: throw NotFoundException("No alert mapping found for dpsId=$dpsId")

  @Transactional
  suspend fun createMappings(mappings: ContactPersonMappingsDto) {
    with(mappings) {
      personMappingRepository.save(toPersonMapping())
      personAddressMapping.forEach {
        personAddressMappingRepository.save(toMapping(it))
      }
      personPhoneMapping.forEach {
        personPhoneMappingRepository.save(toMapping(it))
      }
      personEmailMapping.forEach {
        personEmailMappingRepository.save(toMapping(it))
      }
      personEmploymentMapping.forEach {
        personEmploymentMappingRepository.save(toMapping(it))
      }
      personIdentifierMapping.forEach {
        personIdentifierMappingRepository.save(toMapping(it))
      }
      personRestrictionMapping.forEach {
        personRestrictionMappingRepository.save(toMapping(it))
      }
      personContactMapping.forEach {
        personContactMappingRepository.save(toMapping(it))
      }
      personContactRestrictionMapping.forEach {
        personContactRestrictionMappingRepository.save(toMapping(it))
      }
    }
  }
}

private fun PersonMapping.toDto() = PersonMappingDto(
  nomisId = nomisId,
  dpsId = dpsId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private fun ContactPersonMappingsDto.toPersonMapping() = PersonMapping(
  dpsId = personMapping.dpsId,
  nomisId = personMapping.nomisId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private inline fun <reified T : AbstractContactPersonMapping> ContactPersonMappingsDto.toMapping(mapping: ContactPersonSequenceMappingIdDto): T =
  T::class.java.getDeclaredConstructor(
    String::class.java,
    Long::class.java,
    Long::class.java,
    String::class.java,
    ContactPersonMappingType::class.java,
    LocalDateTime::class.java,
  ).newInstance(
    mapping.dpsId,
    mapping.nomisPersonId,
    mapping.nomisSequenceNumber,
    this.label,
    this.mappingType,
    this.whenCreated,
  )

private inline fun <reified T : AbstractContactPersonMapping> ContactPersonMappingsDto.toMapping(mapping: ContactPersonSimpleMappingIdDto): T =
  T::class.java.getDeclaredConstructor(
    String::class.java,
    Long::class.java,
    String::class.java,
    ContactPersonMappingType::class.java,
    LocalDateTime::class.java,
  ).newInstance(
    mapping.dpsId,
    mapping.nomisId,
    this.label,
    this.mappingType,
    this.whenCreated,
  )
