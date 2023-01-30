package ptcgtool.backend

import ptcgtool.api.CardFetcher.fetchCard
import ptcgtool.backend.IOTools.cacheFolder

import java.io.*
import java.net.URL
import java.nio.file.Path
import scala.IArray.unzip
import scala.collection.parallel.CollectionConverters.{ImmutableIterableIsParallelizable, ImmutableSeqIsParallelizable, IterableIsParallelizable, seqIsParallelizable}
import scala.compiletime.ops.boolean.||
import scala.concurrent.Await.result
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.sequence
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.language.postfixOps
import scala.util.{Failure, Success, Try, Using}

// Bunch of syntax sugar for working with files
private def stream(fromURl: URL): InputStream = fromURl openStream

private def stream(toPath: String): OutputStream = FileOutputStream(toPath)

private def transferData(from: InputStream, target: OutputStream): Unit = from transferTo target

def ensureIsCreated(folder: File): Boolean =
  if (!folder.exists)
    folder.mkdirs()
  folder.exists

// IOTools class is static and is a collection of methods for working with Input/Output
object IOTools:
  val CACHED_FILES_LOCATION: String = System.getProperty("user.dir") + "\\src\\resources\\cache\\images\\"
  val SMALL_IMAGE_FILE_EXTENSION = "_small.png"
  val LARGE_IMAGE_FILE_EXTENSION = "_large.png"
  val IMAGE_FILE_EXTENSION = ".png"
  private val DECKS_LOCATION: String = System.getProperty("user.dir") + "\\src\\resources\\decks\\"
  private val DECK_FILE_EXTENSION = ".deck"
  private val cacheFolder: File = new File(CACHED_FILES_LOCATION)
  private val decksFolder: File = new File(DECKS_LOCATION)

  /**
   * This function fetches an image from a URL and downloads it to the desired destination file
   *
   * @param fromUrl the URL we want to download the image from
   * @param toPath  the absolute Path on the disk, we can get the Project's path using System.getProperty("user.dir")
   */
  def saveImage(fromUrl: URL, toPath: String): Unit =
    ensureIsCreated(cacheFolder)
    Try(Using.resources(stream(fromUrl), stream(toPath))(transferData)) recover {
      case e: Exception => throw new Exception("Error while saving image: " + e.getMessage)
    }

  def clearCache: Unit =
    println(s"Clearing cache folder... $directorySizeToString found in cache folder")
    cacheFolder.listFiles foreach (_.delete)
    println(s"$directorySizeToString found in cache folder now !")

  private def directorySize: Long = cacheFolder.listFiles map (_.length) sum

  private def directorySizeToString: String =
    val size = directorySize
    if size < 1024 then size + " bytes"
    else if size < 1024 * 1024 then size / 1024 + " KB"
    else if size < 1024 * 1024 * 1024 then size / 1024 / 1024 + " MB"
    else size / 1024 / 1024 / 1024 + " GB"

  def removeDeckExtension(deckName: String): String = deckName.replace(DECK_FILE_EXTENSION, "")

  private def addDeckExtension(deckName: String): String = deckName + DECK_FILE_EXTENSION

  /**
   * Used to write a Deck to the disk, generating a file with the .deck extension that we can read later
   *
   * @param name the name of the Deck and therefore the file. i.e. for "Lugia Vstar",
   *             the file will be called "Lugia_Vstar.deck"
   * @param deck the deck containing the cards we want to save
   */
  def saveDeck(name: String, deck: Deck): Unit =
    val deckFile = File(DECKS_LOCATION + addDeckExtension(name))
    Using.resource(PrintWriter(deckFile))(_.write(deck toString))

  def readDeckFolder: List[String] =
    ensureIsCreated(decksFolder)
    Try(new File(DECKS_LOCATION).listFiles) match
      case Failure(_) => List.empty
      case Success(files) => files.map(_.getName).toList

  def readDeck(name: String): Deck =
    val deckFile = File(DECKS_LOCATION + name)
    val reader = BufferedReader(FileReader(deckFile))
    Using.resource(reader) { reader =>
      val deckString = reader.lines.toArray mkString "\n"
      parseDeck(deckString)
    }

def parseDeck(deckString: String): Deck =
  def headerLineOrEmpty(line: String): Boolean =
    line.startsWith("Pokemon") || line.startsWith("Trainer") || line.startsWith("Energy") || line.isEmpty

  def parseCardLine(line: String): (Future[Card], Int) =
    val id = line.split(" ").last // some cards have spaces in their name
    val count = line.split(" ").head.toInt
    (fetchCard(id), count)

  val linesArray = deckString split "\r?\n"
  val cardLines = linesArray filterNot headerLineOrEmpty toSeq // filter out the header lines and empty lines

  val (cardFutures, cardCounts) = cardLines.par.map(parseCardLine).seq.unzip

  val cards = result(sequence(cardFutures), Duration.Inf)
  val cardsToCounts = cards zip cardCounts toMap

  new Deck(cardsToCounts)




