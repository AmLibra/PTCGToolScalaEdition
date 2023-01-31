package ptcgtool
package api

import org.json4s.*
import org.json4s.JsonDSL.WithBigDecimal.seq2jvalue
import org.json4s.JsonDSL.WithDouble.seq2jvalue
import org.json4s.JsonDSL.seq2jvalue
import org.json4s.native.JsonMethods.*
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}
import ptcgtool.backend.IOTools.ensureIsCreated
import ptcgtool.backend.Card

import java.io.*
import java.net.http.HttpClient.Version
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.HttpResponse.BodyHandlers.ofString
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.net.{URI, URL}
import scala.collection.parallel.CollectionConverters.{ArrayIsParallelizable, ImmutableIterableIsParallelizable, ImmutableSeqIsParallelizable, IterableIsParallelizable, seqIsParallelizable}
import scala.collection.parallel.ParMap
import scala.collection.parallel.mutable.ParArray
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.language.postfixOps
import scala.util.{Failure, Success, Try, Using}

object CardFetcher:
  // https://api.pokemontcg.io/v2/cards?q=name:charizard&x-api-key=2e04bfdb-5eb1-4eaf-9c3d-316a1abb6c70
  // is the url to get the card data of charizard cards
  // this returns a json object with a data field that contains a list of cards
  // the cards have an id, name, imageUrl, and types field

  private val apiKey = "2e04bfdb-5eb1-4eaf-9c3d-316a1abb6c70"
  private val baseUrl = "https://api.pokemontcg.io/v2/cards"
  private val orderedBySetReleaseDateAndNumber = "&orderBy=set.releaseDate,number"
  private val filterExpanded = "%20legalities.expanded:legal"
  private val filterPostBlackAndWhiteReleases = "%20(set.id:bw*%20or%20set.id:xy*%20or%20set.id:sm*%20or%20set.id:swsh*)"

  private val orderAndFilters = filterExpanded + filterPostBlackAndWhiteReleases + orderedBySetReleaseDateAndNumber

  private val CARD_JSON_DATA_FOLDER_LOCATION = System.getProperty("user.dir") + "\\src\\resources\\data\\"
  private val cardJsonFolder = File(CARD_JSON_DATA_FOLDER_LOCATION)

  private val OPT_CARD_JSON_DATA_FOLDER_LOCATION = System.getProperty("user.dir") + "\\src\\resources\\opt_data\\"
  private val opt_cardJsonFolder = File(OPT_CARD_JSON_DATA_FOLDER_LOCATION)

  private def sanitizeCardFileName(name: String): String = name replaceAll("[^a-zA-Z0-9.'-]", "_") toLowerCase

  private def sanitizedQuery(query: String): String = query replaceAll(" ", "_") replaceAll("[^a-zA-Z0-9'-_]", "") toLowerCase

  implicit val formats: Formats = Serialization formats NoTypeHints

  private def getParsedJson(url: String): JValue =
    def callAPI(req: String): HttpResponse[String] =
      val request = HttpRequest.newBuilder uri (URI create req) header("X-Api-Key", apiKey) build
      val httpsClient = HttpClient.newBuilder version Version.HTTP_2 build;
      httpsClient send(request, ofString)

    parse(callAPI(url).body())

  private def fetchMultiPagesAsync(url: String): List[JValue] =
    def fetchCardsfromPageAsync(page: Int): List[JValue] =
      (getParsedJson(s"$url${if url == baseUrl then "?" else "&"}page=$page") \ "data").extract[List[JValue]]

    val json = getParsedJson(url)
    val pageSize = (json \ "pageSize").extract[Int]
    val total = (json \ "totalCount").extract[Int]
    val numOfPages = (total + pageSize - 1) / pageSize

    val firstPageData = (json \ "data").extract[List[JValue]]
    val restOfPages = (2 to numOfPages).par.map(fetchCardsfromPageAsync)
    firstPageData ++ restOfPages.flatten

  private def fetchCardsAsync(name: String): Future[List[Card]] = Future {
    fetchMultiPagesAsync(s"$baseUrl?q=name:*$name*$orderAndFilters") map (Card(_))
  }

  private def fetchCardAsync(id: String): Future[Card] = Future {
    Card((getParsedJson(s"$baseUrl/$id") \ "data").extract[JValue])
  }

  /**
   * Used to update the database, this will fetch all cards from the API and write them to disk
   * This function may take a while to run, even though it looks small
   */
  def updateDB(): Unit =
    def writeToDisk(jsons: List[JValue]): Unit =
      jsons.par foreach (json =>
        val cardId = (json \ "id").extract[String]
        val setFolder = cardId.split("-")(0) + "\\"
        val setFolderFile = File(CARD_JSON_DATA_FOLDER_LOCATION + setFolder)
        ensureIsCreated(setFolderFile)
        val fileName = CARD_JSON_DATA_FOLDER_LOCATION + setFolder + s"${sanitizeCardFileName(cardId)}.json"
        if (!File(fileName).exists)
          Using(PrintWriter(fileName))(writer => writer write write(json))
        )

    writeToDisk(fetchMultiPagesAsync(baseUrl))

  // costly function, takes about 20 ms to run
  private def readCardFromDB(id: String): Card =
    val setFolder = id.split("-")(0) + "\\"
    val fileName = CARD_JSON_DATA_FOLDER_LOCATION + setFolder + id + {
      if id.endsWith(".json") then "" else ".json"
    }
    Using(FileReader(fileName))(reader => Card(parse(reader).extract[JValue])
    ) match
      case Success(card) => card
      case Failure(_) => throw new Exception(s"Card with id $id not found in DB")

  def generateSearchDirectoryV2(): Unit =
    def generateSearchDirectoryFiles(name: String): Seq[String] =
      name.toLowerCase().split("[^a-z0-9'.]").filter(_.nonEmpty)
        .distinct map (name => OPT_CARD_JSON_DATA_FOLDER_LOCATION + "\\" + name)

    ensureIsCreated(opt_cardJsonFolder)
    val cardJsonFolders = cardJsonFolder.listFiles
    cardJsonFolders.par.foreach(setFolder =>
      val allCards = setFolder.listFiles.par.map(file => readCardFromDB(file.getName))
      val cardKeyWordFiles: ParMap[Seq[String], String] = allCards.map(card => generateSearchDirectoryFiles(card.name) -> card.id) toMap;
      cardKeyWordFiles.foreach((files, id) =>
        files.par.foreach(file =>
          Using(PrintWriter(FileOutputStream(file, true)))(writer => writer write id + "\n")
        )
      )
    )
  
  
  private def searchCardsInDB(name: String): Future[List[Card]] =
    def readFileCards(fileName: String): List[Future[Card]] =
      Using(BufferedReader(FileReader(fileName))) { reader =>
        reader.lines.toArray.mkString("\n").split("\r?\n").par.map(id => fetchCard(id)).toList
      } match
        case Success(cards) => cards
        case Failure(_) => List.empty
    
    Future.sequence({
      val start = System.currentTimeMillis()
      val keywords = name.split("_").filter(_.nonEmpty)
      val out = opt_cardJsonFolder.listFiles.par.filter(file => keywords.exists(file.getName.contains))
        .flatMap(file => readFileCards(file.getPath)) toList;
      println(s"Search for $name took ${System.currentTimeMillis() - start}ms")
      out
    })


  /**
   * This function will fetch a card from the database asynchronously using its id and return it
   *
   * @param id the id of the card
   * @return the card future
   */
  def fetchCard(id: String): Future[Card] = Future(readCardFromDB(id))

  /**
   * This function will fetch a list of cards whose names contain the given name
   * from the database asynchronously and return them
   *
   * @param name the name of the card
   * @return the cards future
   */
  def fetchCards(name: String, standOnly: Boolean): Future[List[Card]] = searchCardsInDB(sanitizedQuery(name) strip)

