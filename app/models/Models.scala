package models

import play.api.http.{ContentTypeOf, ContentTypes, Writeable}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc.Codec

/**
  * Models used to represent user and transaction data, as
  * well as their companion objects with JSON parsing information.
  */

/** A representation of a user */
case class User (
  id: String,
  name: String
)

object User {

  implicit val write: Writes[User] = Json.writes[User]

  implicit val reads: Reads[User] = (
    ((JsPath \\ "row")(0) \ "id").read[String] and
      ((JsPath \\ "row")(0) \ "name").read[String]
    )(User.apply _)
}

/** A friendship transaction */
case class FriendshipTransaction(
  from: User,
  to: User,
  areFriends: Boolean,
  timestamp: String // Having this as a String is ugly, but converting to a DateTime or even
                    //   a Long is too expensive to be worth the type-safety.
)

object FriendshipTransaction {

  implicit val reads: Reads[FriendshipTransaction] = Json.reads[FriendshipTransaction]
}

/** A list of a user's friends */
case class FriendList(friends: Seq[User])

object FriendList {

  val empty = FriendList(Seq())

  implicit val writes: Writes[FriendList] = Json.writes[FriendList]

  implicit val reads: Reads[FriendList] = {
    (JsPath \\ "data").read[Seq[User]] map { seq => FriendList(seq) }
  }
}

/** Wrapper for a Cypher query. */
case class CypherStatement(statement: String)

object CypherStatement {

  implicit val format: Format[CypherStatement] = Json.format[CypherStatement]
}

case class Neo4JRequest(statements: Seq[CypherStatement])

/** A representation of a Neo4J request, which defines it's interface for         *
  * receiving requests here: https://neo4j.com/docs/rest-docs/current/            */
object Neo4JRequest {

  implicit val writes: Writes[Neo4JRequest] = Json.writes[Neo4JRequest]

  implicit def writeable(implicit codec: Codec): Writeable[Neo4JRequest] = {
    Writeable(data => codec.encode(Json.toJson(data).toString))
  }

  implicit def contentType(implicit codec: Codec): ContentTypeOf[Neo4JRequest] = {
    ContentTypeOf(Some(ContentTypes.TEXT))
  }
}
