import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.server.application.call
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.future
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


val logger = KotlinLogging.logger {}

val sleeper = SleepingClient()

fun startHttpServer() {
    logger.info("Starting http server...")
    embeddedServer(Netty, 8080, configure = {
        workerGroupSize = 20
        shareWorkGroup = true
    }) {
//        install(ContentNegotiation) {
//            json()
//        }
        routing {
            get("/") {
                call.respondText("is running...")
            }

            get("/{message}/{sleep}") {


                logger.info { "start responding with ${call.parameters["message"]} and timeout: ${call.parameters["sleep"]}" }
//                call.respond(HttpStatusCode.BadRequest, ErrorResponse("reason of failure"))
//                call.respond(HttpStatusCode.BadRequest, "text error")

                val job = future {
                    sleeper.getData(call.parameters["sleep"].toString().toLong())
                }

                job.whenCompleteAsync { data, exception ->
                    logger.info { "obtained data: '$data' with timeout ${call.parameters["sleep"]}" }
                }

                val data = withTimeoutOrNull(500) {
                    job.asDeferred().await()
                }

                call.respond(HttpStatusCode.OK, OkResponse("ok: $data "))



                logger.info { "finished responding ${call.parameters["message"]} with $data adn timeout ${call.parameters["sleep"]}" }
            }
        }
    }.start(wait = true)
    logger.info("Http server has started")
}


fun main() {
    runBlocking {
        launch(Dispatchers.Default) {
            startHttpServer()
        }
    }

}
