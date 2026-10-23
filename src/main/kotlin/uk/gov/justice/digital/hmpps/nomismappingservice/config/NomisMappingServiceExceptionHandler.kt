package uk.gov.justice.digital.hmpps.nomismappingservice.config

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.reactive.resource.NoResourceFoundException
import org.springframework.web.server.ServerWebInputException
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestControllerAdvice
class NomisMappingServiceExceptionHandler {
  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(HttpStatus.FORBIDDEN)
    .body(ErrorResponse(status = (HttpStatus.FORBIDDEN.value()))).also {
      log.debug("Forbidden (403) returned with message {}", e.message)
    }

  // handles kotlin exceptions when parsing request objects and hitting non-nullable fields
  // The useful property name is in the nested exception
  @ExceptionHandler(ServerWebInputException::class)
  fun handleSpring400Exception(e: ServerWebInputException): ResponseEntity<ErrorResponse> {
    val message = e.cause?.message ?: e.message
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: $message",
          developerMessage = message,
        ),
      ).also { log.info("Validation exception: {}", message) }
  }

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Validation exception: {}", e.message) }

  @Suppress("UNCHECKED_CAST")
  @ExceptionHandler(DuplicateMappingException::class)
  fun <MAPPING> handleDuplicateException(e: DuplicateMappingException): ResponseEntity<DuplicateMappingErrorResponse<MAPPING>> = ResponseEntity
    .status(CONFLICT)
    .body(
      DuplicateMappingErrorResponse(
        moreInfo = DuplicateErrorContent(
          duplicate = e.duplicate,
          existing = e.existing,
        ),
        userMessage = "Conflict: ${e.message}",
        developerMessage = e.message,
      ) as DuplicateMappingErrorResponse<MAPPING>,
    ).also { log.error("Duplicate mapping exception: {}", e.message) }

  @ExceptionHandler(NotFoundException::class)
  fun handleNotFoundException(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(HttpStatus.NOT_FOUND)
    .body(
      ErrorResponse(
        status = HttpStatus.NOT_FOUND,
        userMessage = "Not Found: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Not Found: {}", e.message) }

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(HttpStatus.NOT_FOUND)
    .body(
      ErrorResponse(
        status = HttpStatus.NOT_FOUND,
        userMessage = "No resource found failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("No resource found exception: {}", e.message) }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(INTERNAL_SERVER_ERROR)
    .body(
      ErrorResponse(
        status = INTERNAL_SERVER_ERROR,
        userMessage = "Unexpected error: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.error("Unexpected exception", e) }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

class DuplicateMappingErrorResponse<MAPPING>(
  val moreInfo: DuplicateErrorContent<MAPPING>,
  val status: HttpStatus = CONFLICT,
  val errorCode: Int = 1409,
  val userMessage: String,
  val developerMessage: String?,
)

data class DuplicateErrorContent<MAPPING>(
  val duplicate: MAPPING,
  val existing: MAPPING,
)

class DuplicateMappingException(
  val duplicate: Any,
  val existing: Any? = null,
  messageIn: String?,
  cause: Exception? = null,
) : RuntimeException(messageIn, cause)
