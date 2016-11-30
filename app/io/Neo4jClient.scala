package io

import models.{FriendList, FriendshipTransaction, Neo4JRequest, User}
import play.Configuration
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess}
import com.google.inject.{ImplementedBy, Singleton}
import play.api.libs.ws._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Methods for in/output with database. Constructs and executes cypher queries to
  * the configured Neo4J instance.
  */
@ImplementedBy(classOf[Neo4jClientImpl])
trait Neo4jClient {

  /**
    * Either add or remove a friendship based on the type of transaction.
    * @param transaction A single friendship transaction denoting a relationship to add or remove.
    * @return Whether or not the operation was successful.
    */
  def updateFriendship(transaction: FriendshipTransaction): Future[Boolean]

  /**
    * Query db by a user's id to find friendships. Parses Neo4J response into Scala objects.
    * @param id The user's id
    * @return List of [[User]]s that the user is friends with. Empty if user is not found or has no friends :(
    */
  def getFriends(id: String): Future[FriendList]
}

@Singleton
class Neo4jClientImpl (implicit ws: WSClient, ec: ExecutionContext) extends CypherHelper with Neo4jClient {

  // Config getters. Static for speed
  private val neo4jRoute = Configuration.root().getString("neo4j.default.url")
  private val neo4jUsername = Configuration.root().getString("neo4j.default.username")
  private val neo4jPassword = Configuration.root().getString("neo4j.default.password")

  // A singular url is used for all transactions; the body contains the cypher itself
  private val neo4jURL = s"$neo4jRoute/db/data/transaction/commit"

  private val neo4JHeaders = Seq(("Content-Type", "application/json"))

  /**
    * Attaches the required headers and authentication for the Neo4J request.
    */
  private def formatRequest: WSRequest = ws.url(neo4jURL)
    .withHeaders(neo4JHeaders: _*)
    .withAuth(neo4jUsername, neo4jPassword, WSAuthScheme.BASIC)

  override def updateFriendship(transaction: FriendshipTransaction): Future[Boolean] = {
    if (transaction.areFriends) {
      addFriendship(transaction.from, transaction.to, transaction.timestamp)
    } else {
      removeFriendship(transaction.from, transaction.to)
    }
  }

  /**
    * Adds a friendship to db.
    * Upsert (merge) each user so there are no duplicates, and then upsert their friendship.
    * @param timestamp The timestamp of the friendship in milliseconds, represented as a String to
    *                  avoid expensive type conversions.
    * @return Whether or not the operation was successful.
    *
    * @note Friendships are stored directionally as per Neo4J standards but represent mutual friendships.
    */
  private def addFriendship(firstUser: User, secondUser: User, timestamp: String): Future[Boolean] = {
    val neo4JRequest = Neo4JRequest(Seq(
      upsertUserQuery(firstUser),
      upsertUserQuery(secondUser),
      upsertFriendshipQuery(firstUser, secondUser, timestamp)
    ))

    formatRequest.post(neo4JRequest) map { response =>
      if (response.status != 200) {
        Logger.logger.warn("Failed Neo4J friendship creation. Carrying on.")
        false
      } else true
    }
  }

  /**
    * Remove a friendship from db.
    * Matches upon each of the two users and removes the relationship between them, if present.
    * @return Whether or not the operation was successful.
    */
  private def removeFriendship(firstUser: User, secondUser: User): Future[Boolean] = {
    val neo4JRequest = Neo4JRequest(Seq(
      removeFriendshipQuery(firstUser, secondUser)
    ))

    formatRequest.post(neo4JRequest) map { response =>
      if (response.status != 200) {
        Logger.logger.warn("Failed Neo4J friendship removal. Carrying on.")
        false
      } else {
        true
      }
    }
  }

  override def getFriends(id: String): Future[FriendList] = {
    val neo4JRequest = Neo4JRequest(Seq(
      getFriendsQuery(id)
    ))

    formatRequest.post[Neo4JRequest](neo4JRequest) map { response =>
      response.json.validate[FriendList] match {
        case JsSuccess(friends, _) =>
          friends
        case e: JsError =>
          Logger.logger.error(s"Failed Neo4J friendship retrieval. Returning empty FriendList." +
            s" Received body:\n${response.body}")
          FriendList.empty
      }
    }
  }
}
