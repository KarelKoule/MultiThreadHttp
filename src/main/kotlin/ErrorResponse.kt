import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val reason: String) {}