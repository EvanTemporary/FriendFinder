package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.google.inject.{ImplementedBy, Singleton}
import modules.Module
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[ApplicationImpl])
trait Application {

  /**
    * Begins ingestion: the process of stream data from Heroku and inserting it to db.
    * Runs continuously with no end.
    */
  def ingest: Action[AnyContent]

  /**
    * Fetches a user's friends given his/her id.
    * @param userId The user's identifier in the system.
    * @return A sequence of users, or an empty seq if the id can't be
    *         resolved to a user that has friends in the system.
    */
  def getFriends(userId: String): Action[AnyContent]
}

@Singleton
class ApplicationImpl @Inject() (
  implicit ws: WSClient,
  ec: ExecutionContext,
  system: ActorSystem,
  materializer: Materializer
) extends Controller() with Application {

  lazy val module = new Module

  override def ingest = Action.async {
    module.friendshipIngester.beginIngestion()
    Future.successful(Ok("{}"))
  }

  override def getFriends(userId: String) = Action.async {
    module.friendshipService.getFriendshipByUserId(userId) map { friends =>
      Ok(Json.toJson(friends))
    }
  }
}
