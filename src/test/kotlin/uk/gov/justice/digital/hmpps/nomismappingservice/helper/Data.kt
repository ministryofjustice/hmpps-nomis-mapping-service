package uk.gov.justice.digital.hmpps.nomismappingservice.helper

// Update sync service reads as if these are Maps
class TestDuplicateErrorResponse(
  val moreInfo: TestDuplicateErrorContent,
)

data class TestDuplicateErrorContent(
  val duplicate: Map<String, *>,
  val existing: Map<String, *>? = null,
)
