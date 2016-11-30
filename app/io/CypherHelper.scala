package io

import models.{CypherStatement, User}

/**
  * There are no Cypher libs compatible with Play 2.5! Here's a small custom one.
  */
trait CypherHelper {

  // These could be in configs but they are incredibly unlikely to change
  private val userLabel = "USER"
  private val friendshipRelation = "FRIENDS_WITH"
  private val idKey = "id"
  private val nameKey = "name"
  private val timestampKey = "timestamp"

  /** Upsert a User Node. */
  def upsertUserQuery(user: User) = CypherStatement(
    s"""
       | MERGE (u:$userLabel {id:"${user.id}", name:"${user.name}"})
    """.stripMargin
  )

  /** Match upon two users and upsert a relationship between them. */
  def upsertFriendshipQuery(firstUser: User, secondUser: User, timestamp: String) = CypherStatement(
    s"""
       | MATCH (a:$userLabel {$idKey:"${firstUser.id}", $nameKey:"${firstUser.name}"}),
       |   (b:$userLabel {$idKey:"${secondUser.id}", $nameKey:"${secondUser.name}"})
       | MERGE (a)-[:$friendshipRelation {$timestampKey: "$timestamp"}]-(b)
     """.stripMargin
  )

  /** Match up two users and remove a friendship between them if found. */
  def removeFriendshipQuery(firstUser: User, secondUser: User) = CypherStatement(
    s"""
       | MATCH
       |   (:$userLabel {$idKey:"${firstUser.id}", $nameKey:"${firstUser.name}"})
       |   -[r:$friendshipRelation]-
       |   (:$userLabel {$idKey:"${secondUser.id}", $nameKey:"${secondUser.name}"})
       | DELETE r
    """.stripMargin
  )

  /**  Match on friendships from a specific node. Return all its friends. */
  def getFriendsQuery(id: String) = CypherStatement(
    s"""
       | MATCH (:$userLabel {$idKey:"$id"})-[:$friendshipRelation]-(friend)
       | RETURN friend
     """.stripMargin
  )
}
