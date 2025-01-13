package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.coreperson

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
class CorePersonService(
  private val corePersonMappingRepository: CorePersonMappingRepository,
  private val corePersonAddressMappingRepository: CorePersonAddressMappingRepository,
  private val corePersonPhoneMappingRepository: CorePersonPhoneMappingRepository,
  private val corePersonEmailMappingRepository: CorePersonEmailAddressMappingRepository,
) {
  @Transactional
  suspend fun createMappings(mappings: CorePersonMappingsDto) {
    with(mappings) {
      corePersonMappingRepository.save(toCorePersonMapping())
      addressMappings.forEach {
        corePersonAddressMappingRepository.save(toMapping(it))
      }
      phoneMappings.forEach {
        corePersonPhoneMappingRepository.save(toMapping(it))
      }
      emailMappings.forEach {
        corePersonEmailMappingRepository.save(toMapping(it))
      }
    }
  }

  suspend fun getCorePersonMappingsByMigrationId(pageRequest: Pageable, migrationId: String): Page<CorePersonMappingDto> =
    coroutineScope {
      val mappings = async {
        corePersonMappingRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
          label = migrationId,
          mappingType = CorePersonMappingType.MIGRATED,
          pageRequest = pageRequest,
        )
      }

      val count = async {
        corePersonMappingRepository.countAllByLabelAndMappingType(
          migrationId = migrationId,
          mappingType = CorePersonMappingType.MIGRATED,
        )
      }

      PageImpl(
        mappings.await().toList().map { it.toDto() },
        pageRequest,
        count.await(),
      )
    }

  suspend fun getCorePersonMappingByNomisPrisonNumber(nomisPrisonNumber: String) =
    corePersonMappingRepository.findOneByNomisPrisonNumber(nomisPrisonNumber = nomisPrisonNumber)
      ?.toDto()
      ?: throw NotFoundException("No core person mapping found for nomisPrisonNumber=$nomisPrisonNumber")

  suspend fun getCorePersonMappingByCprId(cprId: String) =
    corePersonMappingRepository.findOneByCprId(cprId = cprId)
      ?.toDto()
      ?: throw NotFoundException("No core person mapping found for cprId=$cprId")

  suspend fun getCorePersonMappingByCprIdOrNull(cprId: String) =
    corePersonMappingRepository.findOneByCprId(cprId = cprId)
      ?.toDto()

  @Transactional
  suspend fun deleteAllMappings() {
    corePersonEmailMappingRepository.deleteAll()
    corePersonPhoneMappingRepository.deleteAll()
    corePersonAddressMappingRepository.deleteAll()
    corePersonMappingRepository.deleteAll()
  }

  suspend fun getAddressMappingByNomisId(nomisId: Long) =
    corePersonAddressMappingRepository.findOneByNomisId(nomisId = nomisId)
      ?.toDto()
      ?: throw NotFoundException("No core person address mapping found for nomisId=$nomisId")

  suspend fun getAddressMappingByCprId(cprId: String) =
    corePersonAddressMappingRepository.findOneByCprId(cprId = cprId)
      ?.toDto()
      ?: throw NotFoundException("No core person address mapping found for cprId=$cprId")

  suspend fun getPhoneMappingByNomisId(nomisId: Long) =
    corePersonPhoneMappingRepository.findOneByNomisId(nomisId = nomisId) ?.toDto()
      ?: throw NotFoundException("No core person phone mapping found for nomisId=$nomisId")

  suspend fun getPhoneMappingByCprId(cprId: String, cprPhoneType: CprPhoneType) =
    corePersonPhoneMappingRepository.findOneByCprIdAndCprPhoneType(cprId = cprId, cprPhoneType = cprPhoneType)
      ?.toDto()
      ?: throw NotFoundException("No core person phone mapping found for cprId=$cprId")

  suspend fun getEmailAddressMappingByNomisId(nomisId: Long) =
    corePersonEmailMappingRepository.findOneByNomisId(nomisId = nomisId) ?.toDto()
      ?: throw NotFoundException("No core person email mapping found for nomisId=$nomisId")

  suspend fun getEmailAddressMappingByCprId(cprId: String) =
    corePersonEmailMappingRepository.findOneByCprId(cprId = cprId)
      ?.toDto()
      ?: throw NotFoundException("No core person email mapping found for cprId=$cprId")
}

private fun CorePersonMappingsDto.toCorePersonMapping() = CorePersonMapping(
  cprId = personMapping.cprId,
  nomisPrisonNumber = personMapping.nomisPrisonNumber,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private inline fun <reified T : AbstractCorePersonMapping> CorePersonMappingsDto.toMapping(mapping: CorePersonSimpleMappingIdDto): T =
  T::class.java.getDeclaredConstructor(
    String::class.java,
    Long::class.java,
    String::class.java,
    CorePersonMappingType::class.java,
    LocalDateTime::class.java,
  ).newInstance(
    mapping.cprId,
    mapping.nomisId,
    this.label,
    this.mappingType,
    this.whenCreated,
  )

private fun CorePersonMappingsDto.toMapping(mapping: CorePersonPhoneMappingIdDto) = CorePersonPhoneMapping(
  nomisId = mapping.nomisId,
  cprId = mapping.cprId,
  cprPhoneType = mapping.cprPhoneType,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private fun CorePersonMapping.toDto() = CorePersonMappingDto(
  cprId = cprId,
  nomisPrisonNumber = nomisPrisonNumber,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private fun CorePersonAddressMapping.toDto() = CorePersonAddressMappingDto(
  cprId = cprId,
  nomisId = nomisId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private fun CorePersonPhoneMapping.toDto() = CorePersonPhoneMappingDto(
  nomisId = nomisId,
  cprId = cprId,
  cprPhoneType = cprPhoneType,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
private fun CorePersonEmailAddressMapping.toDto() = CorePersonEmailAddressMappingDto(
  nomisId = nomisId,
  cprId = cprId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
