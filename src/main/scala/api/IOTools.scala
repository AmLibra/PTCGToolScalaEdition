package ptcgtool
package api

import objects.Deck

import java.io.*
import java.net.URL
import java.nio.file.Path
import scala.compiletime.ops.boolean.||
import scala.language.postfixOps
import scala.util.{Failure, Success, Try, Using}

// Bunch of syntax sugar for working with files
private def stream(fromURl: URL): InputStream = fromURl openStream

private def stream(toPath: String): OutputStream = FileOutputStream(toPath)

private def transferData(from: InputStream, target: OutputStream): Unit = from transferTo target

private def isCreated(folder: File): Boolean =
  if (!folder.exists)
    folder.mkdirs()
  folder.exists


// IOTools class is static and is a collection of methods for working with Input/Output
object IOTools:
  val CACHED_FILES_LOCATION: String = System.getProperty("user.dir") + "\\src\\resources\\cache\\images" + "\\"
  val IMAGE_FILE_EXTENSION = ".png"
  private val DECKS_LOCATION: String = System.getProperty("user.dir") + "\\src\\resources\\decks\\"
  private val DECK_FILE_EXTENSION = ".deck"
  private val cacheFolder: File = new File(CACHED_FILES_LOCATION)

  /**
   * This function fetches an image from a URL and downloads it to the desired destination file
   *
   * @param fromUrl the URL we want to download the image from
   * @param toPath  the absolute Path on the disk, we can get the Project's path using System.getProperty("user.dir")
   */
  def saveImage(fromUrl: URL, toPath: String): Unit =
    Try(Using.resources(fromUrl.openStream, FileOutputStream(toPath)) { (is, os) =>
      is.transferTo(os)
    }).recover {
      case _: FileNotFoundException =>
        File(CACHED_FILES_LOCATION).mkdirs()
        saveImage(fromUrl, toPath)
    }

  def clearCache: Unit = cacheFolder.listFiles foreach (_.delete)

  def directorySize: Long = cacheFolder.listFiles map (_.length) sum

  def removeDeckExtension(deckName: String): String = deckName.replace(DECK_FILE_EXTENSION, "")

  def addDeckExtension(deckName: String): String = deckName + DECK_FILE_EXTENSION

  /**
   * Used to write a Deck to the disk, generating a file with the .deck extension that we can read later
   *
   * @param name the name of the Deck and therefore the file. i.e. for "Lugia Vstar",
   *             the file will be called "Lugia_Vstar.deck"
   * @param deck the deck containing the cards we want to save
   */
  def saveDeck(name: String, deck: Deck): Unit =
    val deckFile = new File(DECKS_LOCATION + addDeckExtension(name))
    val writer = new PrintWriter(deckFile)
    deck.all foreach (card => writer.println(card.name))
    writer.close()
