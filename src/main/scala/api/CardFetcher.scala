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

object CardFetcher:
  // https://api.pokemontcg.io/v2/cards?q=name:charizard&x-api-key=2e04bfdb-5eb1-4eaf-9c3d-316a1abb6c70
  // is the url to get the card data of charizard cards
  // this returns a json object with a data field that contains a list of cards
  // the cards have an id, name, imageUrl, and types field

  private val apiKey = "2e04bfdb-5eb1-4eaf-9c3d-316a1abb6c70"
  private val baseUrl = "https://api.pokemontcg.io/v2/cards"

  // fetchCards but asynchonously
  private def fetchCardsAsync(name: String): Future[List[Card]] =
    implicit val formats: Formats = Serialization.formats(NoTypeHints)
    val url = s"$baseUrl?q=name:*$name*&x-api-key=$apiKey"
    Future {
      val response = Source.fromInputStream(new URL(url).openStream()).mkString
      val json = parse(response)
      val data = (json \ "data").extract[List[JValue]]
      data.map(Card(_))
    }

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

  def sanitizedQuery(query: String): String = query replaceAll(" ", "") replaceAll("[^a-zA-Z0-9'-_]", "")

  //public method to fetch a card with its id
  def fetchCard(id: String): Future[Card] = fetchCardAsync(sanitizedQuery(id))

  // public method to fetch a list of cards with a name
  def fetchCards(name: String): Future[List[Card]] = fetchCardsAsync(sanitizedQuery(name))
