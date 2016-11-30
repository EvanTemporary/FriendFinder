package io

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Framing, Source}
import akka.util.ByteString
import com.google.inject.{ImplementedBy, Singleton}
import models.FriendshipTransaction
import org.joda.time.DateTime
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods._
import play.Configuration
import play.api.libs.ws.{StreamedResponse, WSClient}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Methods for in/output with Heroku, which provides user data. Uses Akka streams for ongoing parsing
  * of the unlimited chunked JSON response from the Heroku endpoint.
  */
@ImplementedBy(classOf[HerokuClientImpl])
trait HerokuClient {

  /**
    * Queries Heroku for a source of friendship transactions. Parses the byte stream to
    * Scala objects for continuous, streamed usage down the line.
    * @param since Start time of the earliest transaction to request.
    * @return A Source of FriendshipTransactions
    */
  def getFriendTransactions(since: DateTime): Future[Source[FriendshipTransaction, _]]
}

@Singleton
class HerokuClientImpl(
  implicit ws: WSClient,
  ec: ExecutionContext,
  system: ActorSystem,
  materializer: Materializer
) extends HerokuClient {

  private implicit val formats = DefaultFormats

  override def getFriendTransactions(since: DateTime): Future[Source[FriendshipTransaction, _]] = {

    // Construct URL with query param
    val baseUrl = Configuration.root().getString("heroku.baseurl")
    val urlWithSince = baseUrl + s"/?since=${since.getMillis}"

    // Request and parse as stream
    ws.url(urlWithSince).withMethod("GET").stream() map { response: StreamedResponse =>
      response.body.via(Framing.delimiter(ByteString("\n"), Int.MaxValue))
        .map(_.utf8String)
        // Parse FriendshipTransactions from Strings using json4s lib
        .map[FriendshipTransaction](parse(_).extract[FriendshipTransaction])
    }
  }
}
