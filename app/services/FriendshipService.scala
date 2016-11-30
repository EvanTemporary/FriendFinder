package services

import com.google.inject.{ImplementedBy, Singleton}
import io.Neo4jClient
import models.{FriendList, User}
import play.api.Logger

import scala.concurrent.Future

@ImplementedBy(classOf[FriendshipServiceImpl])
trait FriendshipService {

  /**
    * Retrieves a user's friends by query db and parsing to Scala objects.
    * @param userId The user's identifier
    * @return A sequence of [[User]]s
    */
  def getFriendshipByUserId(userId: String): Future[FriendList]
}

@Singleton
class FriendshipServiceImpl (implicit neo4jClient: Neo4jClient) extends FriendshipService {

  override def getFriendshipByUserId(userId: String): Future[FriendList] = {
    Logger.info(s"Received request for users friends with id=$userId.")
    neo4jClient.getFriends(userId)
  }
}
