package ptcgtool
package api

import org.json4s.*
import org.json4s.native.JsonMethods.*
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.Source
import collection.parallel.CollectionConverters.ImmutableSeqIsParallelizable
import collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable
import collection.parallel.CollectionConverters.seqIsParallelizable
import collection.parallel.CollectionConverters.IterableIsParallelizable

object CardFetcher:
  // https://api.pokemontcg.io/v2/cards?q=name:charizard&x-api-key=2e04bfdb-5eb1-4eaf-9c3d-316a1abb6c70
  // is the url to get the card data of charizard cards
  // this returns a json object with a data field that contains a list of cards
  // the cards have an id, name, imageUrl, and types field

  private val apiKey = "2e04bfdb-5eb1-4eaf-9c3d-316a1abb6c70"
  private val baseUrl = "https://api.pokemontcg.io/v2/cards"
  private val orderedBySetReleaseDateAndNumber = "&orderBy=set.releaseDate,number"
  private val filterExpanded = " legalities.expanded:legal"

  implicit val formats: Formats = Serialization.formats(NoTypeHints)
  // fetchCards but asynchonously
  private def fetchCardsAsync(name: String): Future[List[Card]] =

    val url = s"$baseUrl?q=name:*$name*$filterExpanded$orderedBySetReleaseDateAndNumber&x-api-key=$apiKey"
    Future {
      val response = Source.fromInputStream(new URL(url).openStream()).mkString
      val json = parse(response)
      val data = (json \ "data").extract[List[JValue]]
      data.par.map(Card(_)).seq.toList
    }

  private def fetchCardsfromPageAsync(name: String, page: Int): Future[List[Card]] =
    val url = s"$baseUrl?q=name:*$name*$filterExpanded$orderedBySetReleaseDateAndNumber&page=$page&x-api-key=$apiKey"
    Future {
      val response = Source.fromInputStream(new URL(url).openStream()).mkString
      val json = parse(response)
      val data = (json \ "data").extract[List[JValue]]
      data.par.map(Card(_)).seq.toList
    }

  private def fetchCardsFromAllPagesAsync(name: String): Future[List[Card]] =
    val url = s"$baseUrl?q=name:*$name*$filterExpanded$orderedBySetReleaseDateAndNumber&x-api-key=$apiKey"

    val response = Source.fromInputStream(new URL(url).openStream()).mkString
    val json = parse(response)
    val pageSize = (json \ "pageSize").extract[Int]
    val total = (json \ "totalCount").extract[Int]
    val totalPage = (total + pageSize - 1) / pageSize
    val data = (json \ "data").extract[List[JValue]]

    val firstPageFuture = Future {
      data.par.map(Card(_)).seq.toList
    }
    val restOfPages = (2 to totalPage).par.map(fetchCardsfromPageAsync(name,_))

    Future.sequence(firstPageFuture :: restOfPages.toList).map(_.flatten)

  // fetch a single card with its id asynchronously
  private def fetchCardAsync(id: String): Future[Card] =
    implicit val formats: Formats = Serialization.formats(NoTypeHints)
    val url = s"$baseUrl/$id?x-api-key=$apiKey"
    Future {
      val response = Source.fromInputStream(new URL(url).openStream()).mkString
      val json = parse(response)
      val data = (json \ "data").extract[JValue]
      Card(data)
    }

  private def sanitizedQuery(query: String): String =
    query replaceAll(" ", "") replaceAll("[^a-zA-Z0-9'-_]", "")

  //public method to fetch a card with its id
  def fetchCard(id: String): Future[Card] = fetchCardAsync(sanitizedQuery(id))

  // public method to fetch a list of cards with a name
  def fetchCards(name: String): Future[List[Card]] = fetchCardsFromAllPagesAsync(sanitizedQuery(name))
