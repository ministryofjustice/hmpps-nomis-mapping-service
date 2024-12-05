package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.contactperson

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
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
      ?: throw NotFoundException("No person mapping found for nomisId=$nomisId")

  suspend fun getPersonMappingByDpsId(dpsId: String) =
    personMappingRepository.findOneByDpsId(dpsId = dpsId)
      ?.toDto()
      ?: throw NotFoundException("No person mapping found for dpsId=$dpsId")

  suspend fun getPersonMappingByDpsIdOrNull(dpsId: String) =
    personMappingRepository.findOneByDpsId(dpsId = dpsId)
      ?.toDto()

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

  @Transactional
  suspend fun createMapping(mapping: PersonMappingDto) {
    personMappingRepository.save(mapping.toPersonMapping())
  }

  suspend fun getPersonContactMappingByNomisId(nomisId: Long) =
    personContactMappingRepository.findOneByNomisId(nomisId = nomisId)
      ?.toDto()
      ?: throw NotFoundException("No person contact mapping found for nomisId=$nomisId")

  suspend fun getPersonContactMappingByDpsId(dpsId: String) =
    personContactMappingRepository.findOneByDpsId(dpsId = dpsId)
      ?.toDto()
      ?: throw NotFoundException("No person contact mapping found for dpsId=$dpsId")

  suspend fun getPersonContactMappingByDpsIdOrNull(dpsId: String) =
    personContactMappingRepository.findOneByDpsId(dpsId = dpsId)
      ?.toDto()

  @Transactional
  suspend fun createMapping(mapping: PersonContactMappingDto) {
    personContactMappingRepository.save(mapping.toPersonContactMapping())
  }

  suspend fun getPersonMappingsByMigrationId(pageRequest: Pageable, migrationId: String): Page<PersonMappingDto> = coroutineScope {
    val mappings = async {
      personMappingRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
        label = migrationId,
        mappingType = ContactPersonMappingType.MIGRATED,
        pageRequest = pageRequest,
      )
    }

    val count = async {
      personMappingRepository.countAllByLabelAndMappingType(
        migrationId = migrationId,
        mappingType = ContactPersonMappingType.MIGRATED,
      )
    }

    PageImpl(
      mappings.await().toList().map { it.toDto() },
      pageRequest,
      count.await(),
    )
  }
  suspend fun getAllPersonMappings(pageRequest: Pageable): Page<PersonMappingDto> = coroutineScope {
    val mappings = async {
      personMappingRepository.findAllBy(
        pageRequest = pageRequest,
      )
    }

    val count = async {
      personMappingRepository.count()
    }

    PageImpl(
      mappings.await().toList().map { it.toDto() },
      pageRequest,
      count.await(),
    )
  }

  @Transactional
  suspend fun deleteAllMappings() {
    personContactRestrictionMappingRepository.deleteAll()
    personContactMappingRepository.deleteAll()
    personRestrictionMappingRepository.deleteAll()
    personIdentifierMappingRepository.deleteAll()
    personEmploymentMappingRepository.deleteAll()
    personEmailMappingRepository.deleteAll()
    personPhoneMappingRepository.deleteAll()
    personAddressMappingRepository.deleteAll()
    personMappingRepository.deleteAll()
  }

  suspend fun getPersonAddressMappingByNomisId(nomisId: Long) =
    personAddressMappingRepository.findOneByNomisId(nomisId = nomisId)
      ?.toDto()
      ?: throw NotFoundException("No person address mapping found for nomisId=$nomisId")

  suspend fun getPersonAddressMappingByDpsId(dpsId: String) =
    personAddressMappingRepository.findOneByDpsId(dpsId = dpsId)
      ?.toDto()
      ?: throw NotFoundException("No person address mapping found for dpsId=$dpsId")

  suspend fun getPersonAddressMappingByDpsIdOrNull(dpsId: String) =
    personAddressMappingRepository.findOneByDpsId(dpsId = dpsId)
      ?.toDto()

  @Transactional
  suspend fun createMapping(mapping: PersonAddressMappingDto) {
    personAddressMappingRepository.save(mapping.toPersonAddressMapping())
  }

  suspend fun getPersonEmailMappingByNomisId(nomisId: Long) =
    personEmailMappingRepository.findOneByNomisId(nomisId = nomisId) ?.toDto()
      ?: throw NotFoundException("No person email mapping found for nomisId=$nomisId")

  suspend fun getPersonEmailMappingByDpsId(dpsId: String) =
    personEmailMappingRepository.findOneByDpsId(dpsId = dpsId)
      ?.toDto()
      ?: throw NotFoundException("No person email mapping found for dpsId=$dpsId")

  suspend fun getPersonEmailMappingByDpsIdOrNull(dpsId: String) =
    personEmailMappingRepository.findOneByDpsId(dpsId = dpsId)?.toDto()

  @Transactional
  suspend fun createMapping(mapping: PersonEmailMappingDto) {
    personEmailMappingRepository.save(mapping.toPersonEmailMapping())
  }
  suspend fun getPersonPhoneMappingByNomisId(nomisId: Long) =
    personPhoneMappingRepository.findOneByNomisId(nomisId = nomisId) ?.toDto()
      ?: throw NotFoundException("No person phone mapping found for nomisId=$nomisId")

  suspend fun getPersonPhoneMappingByDpsId(dpsId: String, dpsPhoneType: DpsPersonPhoneType) =
    personPhoneMappingRepository.findOneByDpsIdAndDpsPhoneType(dpsId = dpsId, dpsPhoneType = dpsPhoneType)
      ?.toDto()
      ?: throw NotFoundException("No person phone mapping found for dpsId=$dpsId")

  suspend fun getPersonPhoneMappingByDpsIdOrNull(dpsId: String, dpsPhoneType: DpsPersonPhoneType) =
    personPhoneMappingRepository.findOneByDpsIdAndDpsPhoneType(dpsId = dpsId, dpsPhoneType = dpsPhoneType)?.toDto()

  @Transactional
  suspend fun createMapping(mapping: PersonPhoneMappingDto) {
    personPhoneMappingRepository.save(mapping.toPersonPhoneMapping())
  }

  suspend fun getPersonIdentifierMappingByNomisIds(nomisPersonId: Long, nomisSequenceNumber: Long) =
    personIdentifierMappingRepository.findOneByNomisPersonIdAndNomisSequenceNumber(nomisPersonId = nomisPersonId, nomisSequenceNumber = nomisSequenceNumber) ?.toDto()
      ?: throw NotFoundException("No person identifier mapping found for nomisPersonId=$nomisPersonId and nomisSequenceNumber=$nomisSequenceNumber")

  suspend fun getPersonIdentifierMappingByDpsId(dpsId: String) =
    personIdentifierMappingRepository.findOneByDpsId(dpsId = dpsId)
      ?.toDto()
      ?: throw NotFoundException("No person identifier mapping found for dpsId=$dpsId")

  suspend fun getPersonIdentifierMappingByDpsIdOrNull(dpsId: String) =
    personIdentifierMappingRepository.findOneByDpsId(dpsId = dpsId)?.toDto()

  @Transactional
  suspend fun createMapping(mapping: PersonIdentifierMappingDto) {
    personIdentifierMappingRepository.save(mapping.toPersonIdentifierMapping())
  }

  suspend fun getPersonContactRestrictionMappingByNomisId(nomisId: Long) =
    personContactRestrictionMappingRepository.findOneByNomisId(nomisId = nomisId) ?.toDto()
      ?: throw NotFoundException("No person contact restriction mapping found for nomisId=$nomisId")

  suspend fun getPersonContactRestrictionMappingByDpsId(dpsId: String) =
    personContactRestrictionMappingRepository.findOneByDpsId(dpsId = dpsId)
      ?.toDto()
      ?: throw NotFoundException("No person contact restriction mapping found for dpsId=$dpsId")

  suspend fun getPersonContactRestrictionMappingByDpsIdOrNull(dpsId: String) =
    personContactRestrictionMappingRepository.findOneByDpsId(dpsId = dpsId)?.toDto()

  @Transactional
  suspend fun createMapping(mapping: PersonContactRestrictionMappingDto) {
    personContactRestrictionMappingRepository.save(mapping.toPersonContactRestrictionMapping())
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

private fun PersonMappingDto.toPersonMapping() = PersonMapping(
  dpsId = dpsId,
  nomisId = nomisId,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private fun PersonContactMappingDto.toPersonContactMapping() = PersonContactMapping(
  dpsId = dpsId,
  nomisId = nomisId,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private fun PersonContactMapping.toDto() = PersonContactMappingDto(
  nomisId = nomisId,
  dpsId = dpsId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private fun PersonAddressMappingDto.toPersonAddressMapping() = PersonAddressMapping(
  dpsId = dpsId,
  nomisId = nomisId,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private fun PersonAddressMapping.toDto() = PersonAddressMappingDto(
  nomisId = nomisId,
  dpsId = dpsId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private fun PersonEmailMappingDto.toPersonEmailMapping() = PersonEmailMapping(
  dpsId = dpsId,
  nomisId = nomisId,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private fun PersonEmailMapping.toDto() = PersonEmailMappingDto(
  nomisId = nomisId,
  dpsId = dpsId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private fun PersonPhoneMappingDto.toPersonPhoneMapping() = PersonPhoneMapping(
  dpsId = dpsId,
  nomisId = nomisId,
  dpsPhoneType = dpsPhoneType,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private fun PersonPhoneMapping.toDto() = PersonPhoneMappingDto(
  nomisId = nomisId,
  dpsId = dpsId,
  dpsPhoneType = dpsPhoneType,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private fun PersonIdentifierMappingDto.toPersonIdentifierMapping() = PersonIdentifierMapping(
  dpsId = dpsId,
  nomisPersonId = nomisPersonId,
  nomisSequenceNumber = nomisSequenceNumber,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private fun PersonIdentifierMapping.toDto() = PersonIdentifierMappingDto(
  nomisPersonId = nomisPersonId,
  nomisSequenceNumber = nomisSequenceNumber,
  dpsId = dpsId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private fun PersonContactRestrictionMappingDto.toPersonContactRestrictionMapping() = PersonContactRestrictionMapping(
  dpsId = dpsId,
  nomisId = nomisId,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private fun PersonContactRestrictionMapping.toDto() = PersonContactRestrictionMappingDto(
  nomisId = nomisId,
  dpsId = dpsId,
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

private fun ContactPersonMappingsDto.toMapping(mapping: ContactPersonPhoneMappingIdDto) = PersonPhoneMapping(
  nomisId = mapping.nomisId,
  dpsId = mapping.dpsId,
  dpsPhoneType = mapping.dpsPhoneType,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
