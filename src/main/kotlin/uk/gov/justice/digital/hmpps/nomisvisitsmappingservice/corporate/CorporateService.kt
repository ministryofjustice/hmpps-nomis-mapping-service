package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.corporate

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
class CorporateService(
  private val corporateMappingRepository: CorporateMappingRepository,
  private val corporateAddressMappingRepository: CorporateAddressMappingRepository,
  private val corporateAddressPhoneMappingRepository: CorporateAddressPhoneMappingRepository,
  private val corporatePhoneMappingRepository: CorporatePhoneMappingRepository,
  private val corporateEmailMappingRepository: CorporateEmailMappingRepository,
  private val corporateWebMappingRepository: CorporateWebMappingRepository,
) {
  @Transactional
  suspend fun createMappings(mappings: CorporateMappingsDto) {
    with(mappings) {
      corporateMappingRepository.save(toCorporateMapping())
      corporateAddressMapping.forEach {
        corporateAddressMappingRepository.save(toMapping(it))
      }
      corporateAddressPhoneMapping.forEach {
        corporateAddressPhoneMappingRepository.save(toMapping(it))
      }
      corporatePhoneMapping.forEach {
        corporatePhoneMappingRepository.save(toMapping(it))
      }
      corporateEmailMapping.forEach {
        corporateEmailMappingRepository.save(toMapping(it))
      }
      corporateWebMapping.forEach {
        corporateWebMappingRepository.save(toMapping(it))
      }
    }
  }

  suspend fun getCorporateMappingsByMigrationId(pageRequest: Pageable, migrationId: String): Page<CorporateMappingDto> =
    coroutineScope {
      val mappings = async {
        corporateMappingRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
          label = migrationId,
          mappingType = CorporateMappingType.MIGRATED,
          pageRequest = pageRequest,
        )
      }

      val count = async {
        corporateMappingRepository.countAllByLabelAndMappingType(
          migrationId = migrationId,
          mappingType = CorporateMappingType.MIGRATED,
        )
      }

      PageImpl(
        mappings.await().toList().map { it.toDto() },
        pageRequest,
        count.await(),
      )
    }
  suspend fun getAllCorporateMappings(pageRequest: Pageable): Page<CorporateMappingDto> = coroutineScope {
    val mappings = async {
      corporateMappingRepository.findAllBy(
        pageRequest = pageRequest,
      )
    }

    val count = async {
      corporateMappingRepository.count()
    }

    PageImpl(
      mappings.await().toList().map { it.toDto() },
      pageRequest,
      count.await(),
    )
  }

  @Transactional
  suspend fun deleteAllMappings() {
    corporateWebMappingRepository.deleteAll()
    corporateEmailMappingRepository.deleteAll()
    corporatePhoneMappingRepository.deleteAll()
    corporateAddressPhoneMappingRepository.deleteAll()
    corporateAddressMappingRepository.deleteAll()
    corporateMappingRepository.deleteAll()
  }

  @Transactional
  suspend fun createMapping(mapping: CorporateMappingDto) {
    corporateMappingRepository.save(mapping.toMapping())
  }

  suspend fun getCorporateMappingByNomisId(nomisId: Long) =
    corporateMappingRepository.findOneByNomisId(nomisId = nomisId)
      ?.toDto<CorporateMappingDto>()
      ?: throw NotFoundException("No corporate mapping found for nomisId=$nomisId")

  suspend fun getCorporateMappingByDpsIdOrNull(dpsId: String) =
    corporateMappingRepository.findOneByDpsId(dpsId = dpsId)
      ?.toDto<CorporateMappingDto>()

  suspend fun getCorporateMappingByDpsId(dpsId: String) =
    getCorporateMappingByDpsIdOrNull(dpsId)
      ?: throw NotFoundException("No corporate mapping found for dpsId=$dpsId")

  @Transactional
  suspend fun deleteCorporateMappingByNomisId(nomisId: Long) =
    corporateMappingRepository.deleteByNomisId(nomisId = nomisId)

  @Transactional
  suspend fun createMapping(mapping: CorporateAddressMappingDto) {
    corporateAddressMappingRepository.save(mapping.toMapping())
  }

  suspend fun getAddressMappingByNomisId(nomisId: Long) =
    corporateAddressMappingRepository.findOneByNomisId(nomisId = nomisId)
      ?.toDto<CorporateAddressMappingDto>() ?: throw NotFoundException("No address mapping found for nomisId=$nomisId")

  suspend fun getAddressMappingByDpsIdOrNull(dpsId: String) =
    corporateAddressMappingRepository.findOneByDpsId(dpsId = dpsId)
      ?.toDto<CorporateAddressMappingDto>()

  suspend fun getAddressMappingByDpsId(dpsId: String) =
    getAddressMappingByDpsIdOrNull(dpsId)
      ?: throw NotFoundException("No address mapping found for dpsId=$dpsId")

  @Transactional
  suspend fun deleteAddressMappingByNomisId(nomisId: Long) =
    corporateAddressMappingRepository.deleteByNomisId(nomisId = nomisId)

  @Transactional
  suspend fun createMapping(mapping: CorporateAddressPhoneMappingDto) {
    corporateAddressPhoneMappingRepository.save(mapping.toMapping())
  }

  suspend fun getAddressPhoneMappingByNomisId(nomisId: Long) =
    corporateAddressPhoneMappingRepository.findOneByNomisId(nomisId = nomisId)
      ?.toDto<CorporateAddressPhoneMappingDto>() ?: throw NotFoundException("No address phone mapping found for nomisId=$nomisId")

  suspend fun getAddressPhoneMappingByDpsIdOrNull(dpsId: String) =
    corporateAddressPhoneMappingRepository.findOneByDpsId(dpsId = dpsId)
      ?.toDto<CorporateAddressPhoneMappingDto>()

  suspend fun getAddressPhoneMappingByDpsId(dpsId: String) =
    getAddressPhoneMappingByDpsIdOrNull(dpsId)
      ?: throw NotFoundException("No address phone mapping found for dpsId=$dpsId")

  @Transactional
  suspend fun deleteAddressPhoneMappingByNomisId(nomisId: Long) =
    corporateAddressPhoneMappingRepository.deleteByNomisId(nomisId = nomisId)

  @Transactional
  suspend fun createMapping(mapping: CorporatePhoneMappingDto) {
    corporatePhoneMappingRepository.save(mapping.toMapping())
  }

  suspend fun getPhoneMappingByNomisId(nomisId: Long) =
    corporatePhoneMappingRepository.findOneByNomisId(nomisId = nomisId)
      ?.toDto<CorporatePhoneMappingDto>() ?: throw NotFoundException("No phone mapping found for nomisId=$nomisId")

  suspend fun getPhoneMappingByDpsIdOrNull(dpsId: String) =
    corporatePhoneMappingRepository.findOneByDpsId(dpsId = dpsId)
      ?.toDto<CorporatePhoneMappingDto>()

  suspend fun getPhoneMappingByDpsId(dpsId: String) =
    getPhoneMappingByDpsIdOrNull(dpsId)
      ?: throw NotFoundException("No phone mapping found for dpsId=$dpsId")

  @Transactional
  suspend fun deletePhoneMappingByNomisId(nomisId: Long) =
    corporatePhoneMappingRepository.deleteByNomisId(nomisId = nomisId)

  @Transactional
  suspend fun createMapping(mapping: CorporateEmailMappingDto) {
    corporateEmailMappingRepository.save(mapping.toMapping())
  }

  suspend fun getEmailMappingByNomisId(nomisId: Long) =
    corporateEmailMappingRepository.findOneByNomisId(nomisId = nomisId)
      ?.toDto<CorporateEmailMappingDto>() ?: throw NotFoundException("No email mapping found for nomisId=$nomisId")

  suspend fun getEmailMappingByDpsIdOrNull(dpsId: String) =
    corporateEmailMappingRepository.findOneByDpsId(dpsId = dpsId)
      ?.toDto<CorporateEmailMappingDto>()

  suspend fun getEmailMappingByDpsId(dpsId: String) =
    getEmailMappingByDpsIdOrNull(dpsId)
      ?: throw NotFoundException("No email mapping found for dpsId=$dpsId")

  @Transactional
  suspend fun deleteEmailMappingByNomisId(nomisId: Long) =
    corporateEmailMappingRepository.deleteByNomisId(nomisId = nomisId)

  @Transactional
  suspend fun createMapping(mapping: CorporateWebMappingDto) {
    corporateWebMappingRepository.save(mapping.toMapping())
  }

  suspend fun getWebMappingByNomisId(nomisId: Long) =
    corporateWebMappingRepository.findOneByNomisId(nomisId = nomisId)
      ?.toDto<CorporateWebMappingDto>() ?: throw NotFoundException("No web mapping found for nomisId=$nomisId")

  suspend fun getWebMappingByDpsIdOrNull(dpsId: String) =
    corporateWebMappingRepository.findOneByDpsId(dpsId = dpsId)
      ?.toDto<CorporateWebMappingDto>()

  suspend fun getWebMappingByDpsId(dpsId: String) =
    getWebMappingByDpsIdOrNull(dpsId)
      ?: throw NotFoundException("No web mapping found for dpsId=$dpsId")

  @Transactional
  suspend fun deleteWebMappingByNomisId(nomisId: Long) =
    corporateWebMappingRepository.deleteByNomisId(nomisId = nomisId)
}

private fun CorporateMappingsDto.toCorporateMapping() = CorporateMapping(
  dpsId = corporateMapping.dpsId,
  nomisId = corporateMapping.nomisId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private inline fun <reified T : AbstractCorporateMapping> CorporateMappingsDto.toMapping(mapping: CorporateMappingIdDto): T =
  T::class.java.getDeclaredConstructor(
    String::class.java,
    Long::class.java,
    String::class.java,
    CorporateMappingType::class.java,
    LocalDateTime::class.java,
  ).newInstance(
    mapping.dpsId,
    mapping.nomisId,
    this.label,
    this.mappingType,
    this.whenCreated,
  )

private inline fun <reified T : AbstractCorporateMapping> AbstractCorporateMappingDto.toMapping(): T =
  T::class.java.getDeclaredConstructor(
    String::class.java,
    Long::class.java,
    String::class.java,
    CorporateMappingType::class.java,
    LocalDateTime::class.java,
  ).newInstance(
    this.dpsId,
    this.nomisId,
    this.label,
    this.mappingType,
    this.whenCreated,
  )

private inline fun <reified T : AbstractCorporateMappingDto> AbstractCorporateMapping.toDto(): T =
  T::class.java.getDeclaredConstructor(
    String::class.java,
    Long::class.java,
    String::class.java,
    CorporateMappingType::class.java,
    LocalDateTime::class.java,
  ).newInstance(
    this.dpsId,
    this.nomisId,
    this.label,
    this.mappingType,
    this.whenCreated,
  )
