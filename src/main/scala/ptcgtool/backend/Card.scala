package ptcgtool.backend

import org.json4s.*
import org.json4s.native.Serialization
import ptcgtool.api.*
import IOTools.{CACHED_FILES_LOCATION, saveImage}
import scalafx.scene.image.Image

import java.io.{File, FileInputStream}
import java.net.URL
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

val EMPTY_CARD = Image(FileInputStream("C:\\Users\\usr\\IdeaProjects\\PTCGToolScalaEdition\\src\\resources\\emptyCard.png"))
val SMALL_IMAGE_FILE_EXTENSION = "_small.png"
val LARGE_IMAGE_FILE_EXTENSION = "_large.png"

// fields need default values for json4s to work therefore they are all Option
case class Card(name: String, id: String, supertype: String, subtypes: Option[List[String]],
                hp: Option[String], types: Option[List[String]], evolvesFrom: Option[String], abilities: Option[List[Ability]],
                attacks: Option[List[Attack]], set: Option[Set], artist: Option[String], legalities: Option[Legalities],
                images: Option[Images]):

  def isPokemon: Boolean = supertype == "Pokémon"

  def isTrainer: Boolean = supertype == "Trainer"

  def isEnergy: Boolean = supertype == "Energy"

  def isBasicPokemon: Boolean = isPokemon && subtypes.contains(List("Basic"))

  def isStandardLegal: Boolean = legalities.exists(_.standard.get == "Legal")

  private def largeImageUrl: URL = URL(images flatMap(_.large) getOrElse(throw Exception("No large image found")))

  private def smallImageUrl: URL = URL(images flatMap(_.small) getOrElse(throw Exception("No small image found")))

  private val largeImageLocation = CACHED_FILES_LOCATION + id + LARGE_IMAGE_FILE_EXTENSION

  private val smallImageLocation = CACHED_FILES_LOCATION + id + SMALL_IMAGE_FILE_EXTENSION

  private val largeImageFile = File(largeImageLocation)

  private val smallImageFile = File(smallImageLocation)

  private def hasImageDownloaded = largeImageFile.isFile || smallImageFile.isFile

  private def readImage(location: String): Image = Try(Image(FileInputStream(location))) match
    case Success(image) => image
    case Failure(_) => EMPTY_CARD

  /**
   * Fetches lazily the image if it already is stored in the App cache, stores image on disk
   *
   * @return the string that can be used to load the image in ImagePanel
   */
  def getLargeImage: Image =
    if !largeImageFile.isFile then Try(saveImage(largeImageUrl, largeImageLocation))
    readImage(largeImageLocation)

  def getSmallImage: Image =
    if largeImageFile.isFile then readImage(largeImageLocation)
    if !smallImageFile.isFile then Try(saveImage(smallImageUrl, smallImageLocation))
    readImage(smallImageLocation)

  def imageIfExists: Image =
    if largeImageFile.isFile then readImage(largeImageLocation)
    else if smallImageFile.isFile then readImage(smallImageLocation)
    else EMPTY_CARD

  override def toString: String = name + " (" + id + ")"


case class Ability(name: Option[String], text: Option[String], `type`: Option[String])

case class Attack(name: Option[String], cost: Option[List[String]], convertedEnergyCost: Option[Int],
                  damage: Option[String], text: Option[String])

case class Set(id: Option[String], name: Option[String], series: Option[String], printedTotal: Option[Int],
               total: Option[Int], legalities: Option[Legalities], ptcgoCode: Option[String], releaseDate: Option[String],
               updatedAt: Option[String], images: Option[Images])

case class Legalities(unlimited: Option[String], standard: Option[String], expanded: Option[String])

case class Images(small: Option[String], large: Option[String])

object Card:
  def apply(json: JValue): Card =
    implicit val formats: Formats = Serialization.formats(NoTypeHints)
    val name = (json \ "name").extract[String]
    val id = (json \ "id").extract[String]
    val supertype = (json \ "supertype").extract[String]
    val subtypes = (json \ "subtypes").extractOpt[List[String]]
    val hp = (json \ "hp").extractOpt[String]
    val types = (json \ "types").extractOpt[List[String]]
    val evolvesFrom = (json \ "evolvesFrom").extractOpt[String]
    val abilities = (json \ "abilities").extractOpt[List[Ability]]
    val attacks = (json \ "attacks").extractOpt[List[Attack]]
    val set = (json \ "set").extractOpt[Set]
    val artist = (json \ "artist").extractOpt[String]
    val legalities = (json \ "legalities").extractOpt[Legalities]
    val images = (json \ "images").extractOpt[Images]
    Card(name, id, supertype, subtypes, hp, types, evolvesFrom, abilities, attacks, set, artist,legalities, images)
