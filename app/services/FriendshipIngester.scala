package services

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.google.inject.{ImplementedBy, Singleton}
import io.{HerokuClient, Neo4jClient}
import models.FriendshipTransaction
import org.joda.time.DateTime
import play.Configuration
import play.api.Logger

import scala.concurrent.ExecutionContext

@ImplementedBy(classOf[FriendshipIngesterImpl])
trait FriendshipIngester {

  /**
    * Begin ingesting friendship transactions from Heroku. By default,
    * it will start ingesting transactions from now. To start from the
    * beginning for the transactions, see application.conf.
    */
  def beginIngestion(): Unit
}

@Singleton
class FriendshipIngesterImpl(
  implicit ec: ExecutionContext,
  materializer: Materializer,
  herokuClient: HerokuClient,
  neo4jClient: Neo4jClient
) extends FriendshipIngester {

  override def beginIngestion(): Unit = {
    Logger.info("Beginning ingestion. Stop w/ control+C if you are local and don't want to thrash.")
    val maybeConfSince = Option(Configuration.root().getLong("transactions.since")) map { millis =>
      new DateTime(millis)
    }
    val sinceTime = maybeConfSince getOrElse DateTime.now()
    herokuClient.getFriendTransactions(sinceTime) map { source =>
      insertToGraph(source)
    } onComplete { _ =>
      // TODO: Get last inserted time and resume from then
      resumeIngestion(DateTime.now())
    }
  }

  /**
    * Resume ingestion by specifying a start time. OnComplete, calls self for
    * continuous ingestion.
    */
  private def resumeIngestion(sinceTime: DateTime): Unit = {
    Logger.info("Resuming ingestion.")
    herokuClient.getFriendTransactions(sinceTime) map { source =>
      insertToGraph(source)
    } onComplete { _ =>
      // TODO: Get last inserted time and resume from then
      resumeIngestion(DateTime.now())
    }
  }

  /**
    * Takes a Source of FriendshipTransactions, and for each one, updates the
    * graph db according to it's content.
    */
  private def insertToGraph(transactionSource: Source[FriendshipTransaction, _]) = {
    transactionSource runForeach { transaction =>
      neo4jClient.updateFriendship(transaction)
    }
  }
}
