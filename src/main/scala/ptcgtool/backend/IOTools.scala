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
import scala.concurrent.duration.Duration.Inf
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.language.postfixOps
import scala.util.Using.resources
import scala.util.{Failure, Success, Try, Using}

// Bunch of syntax sugar for working with files
private def stream(fromURl: URL): InputStream = fromURl openStream

private def stream(toPath: String): OutputStream = FileOutputStream(toPath)

private def transferData(from: InputStream, target: OutputStream): Unit = from transferTo target

// IOTools class is static and is a collection of methods for working with Input/Output
object IOTools:
  val CACHED_FILES_LOCATION: String = System.getProperty("user.dir") + "\\src\\resources\\cache\\images\\"
  private val DECKS_LOCATION: String = System.getProperty("user.dir") + "\\src\\resources\\decks\\"
  private val DECK_FILE_EXTENSION = ".deck"
  private val cacheFolder: File = new File(CACHED_FILES_LOCATION)
  private val decksFolder: File = new File(DECKS_LOCATION)

  /**
   * This is used to clear the cache folder, deleting all the images in it and freeing up space
   */
  def clearCache(): Unit = cacheFolder.listFiles foreach (_.delete)

  /**
   * Ensures that the folder exists, if it doesn't, it creates it
   *
   * @param folder the folder we want to ensure exists
   * @return true if the folder exists, false if it doesn't
   */
  def ensureIsCreated(folder: File): Boolean = if !(folder exists) then folder mkdirs else folder exists

  /**
   * Used to read a deck from the disk, returning a Deck object
   *
   * @param name the name of the deck we want to read
   * @return a Deck object containing the cards in the deck
   */
  def readDeck(name: String): Deck =
    val deckFile = File(addDeckExtension(DECKS_LOCATION + name))
    val reader = BufferedReader(FileReader(deckFile))
    Using.resource(reader) { reader => parseDeck(reader.lines.iterator.asScala toList) }

  /**
   * Used to read all the decks from the disk, returning a list of the names of the decks
   * without the .deck extension
   * i.e. for "Lugia Vstar.deck", the name returned will be "Lugia Vstar"
   *
   * @return a list of the names of the decks without the .deck extension
   */
  def readDeckFolder: List[String] =
    ensureIsCreated(decksFolder)
    decksFolder.listFiles map (_.getName) filter (_.endsWith(DECK_FILE_EXTENSION)) map removeDeckExtension toList

  /**
   * This function removes the .deck extension from a deck name
   */
  def removeDeckExtension(deckName: String): String = deckName replace(DECK_FILE_EXTENSION, "")

  /**
   * This function fetches an image from a URL and downloads it to the desired destination file
   *
   * @param fromUrl the URL we want to download the image from
   * @param toPath  the absolute Path on the disk, we can get the Project's path using System.getProperty("user.dir")
   */
  def saveImage(fromUrl: URL, toPath: String): Unit =
    ensureIsCreated(cacheFolder)
    resources(stream(fromUrl), stream(toPath))(transferData)

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

  private def directorySize: Long = cacheFolder.listFiles map (_.length) sum

  private def directorySizeToString: String =
    val size = directorySize
    if size < 1024 then size + " bytes"
    else if size < 1024 * 1024 then size / 1024 + " KB"
    else if size < 1024 * 1024 * 1024 then size / 1024 / 1024 + " MB"
    else size / 1024 / 1024 / 1024 + " GB"

  private def addDeckExtension(deckName: String): String = deckName + DECK_FILE_EXTENSION

