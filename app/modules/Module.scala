package modules

import akka.actor.ActorSystem
import akka.stream.Materializer
import io.{HerokuClientImpl, Neo4jClientImpl}
import play.api.libs.ws.WSClient
import services.{FriendshipIngesterImpl, FriendshipServiceImpl}

import scala.concurrent.ExecutionContext

/** Simple Module just to get things wried up and running. */
class Module(implicit ws: WSClient, ec: ExecutionContext, system: ActorSystem, materializer: Materializer) {
  lazy implicit val neo4jClient = new Neo4jClientImpl
  lazy implicit val herokuClient = new HerokuClientImpl
  lazy implicit val friendshipService = new FriendshipServiceImpl
  lazy implicit val friendshipIngester = new FriendshipIngesterImpl
}
