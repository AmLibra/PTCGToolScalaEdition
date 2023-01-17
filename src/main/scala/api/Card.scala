package ptcgtool
package api

import api.*
import api.IOTools.{CACHED_FILES_LOCATION, IMAGE_FILE_EXTENSION, saveImage}

import org.json4s.*
import org.json4s.native.Serialization

import java.io.File
import java.net.URL
import scala.swing.Image

// fields need default values for json4s to work therefore they are all Option
case class Card(name: Option[String],
                id: Option[String],
                supertype: Option[String],
                subtypes: Option[List[String]],
                hp: Option[String],
                types: Option[List[String]],
                evolvesFrom: Option[String],
                abilities: Option[List[Ability]],
                attacks: Option[List[Attack]],
                weaknesses: Option[List[Weakness]],
                retreatCost: Option[List[String]],
                convertedRetreatCost: Option[Int],
                set: Option[Set],
                number: Option[String],
                artist: Option[String],
                rarity: Option[String],
                flavorText: Option[String],
                nationalPokedexNumbers: Option[List[Int]],
                legalities: Option[Legalities],
                images: Option[Images],
                tcgplayer: Option[Tcgplayer],
                cardmarket: Option[Cardmarket]):

  def isPokemon: Boolean = supertype.contains("Pokémon")

  def isTrainer: Boolean = supertype.contains("Trainer")

  def isEnergy: Boolean = supertype.contains("Energy")

  def isBasicPokemon: Boolean = isPokemon && types.contains(List("Basic"))

  def isStandardLegal: Boolean = legalities.exists(_.standard.get == "Legal")

  private def getLargeImageUrl: URL = URL(images.flatMap(_.large).getOrElse(throw new Exception("No large image found")))

  def getId: String = id.getOrElse(throw new Exception("No id found"))

  def getName: String = name.getOrElse(throw new Exception("No name found"))

  /**
   * Fetches lazily the image if it already is stored in the App cache, stores image on disk
   *
   * @return the string that can be used to load the image in ImagePanel
   */
  def getImg: String =
    val imgDir = CACHED_FILES_LOCATION + getId + IMAGE_FILE_EXTENSION
    if (!File(imgDir).isFile) { // checks if the file exists and is not corrupted
      println("Image for " + this + " not found in cache. Downloading...")
      println("Image URL: " + getLargeImageUrl)
      saveImage(getLargeImageUrl, imgDir)
      println("--> Done fetching: " + this + "!")
    }
    imgDir

  // override toString method to print the name of the card and its id
  override def toString: String = name.getOrElse("No name found") + " (" + id.getOrElse("No id found") + ")"


case class Ability(name: Option[String], text: Option[String], `type`: Option[String])

case class Attack(name: Option[String],
                  cost: Option[List[String]],
                  convertedEnergyCost: Option[Int],
                  damage: Option[String],
                  text: Option[String])

case class Weakness(`type`: Option[String], value: Option[String])

case class Set(id: Option[String],
               name: Option[String],
               series: Option[String],
               printedTotal: Option[Int],
               total: Option[Int],
               legalities: Option[Legalities],
               ptcgoCode: Option[String],
               releaseDate: Option[String],
               updatedAt: Option[String],
               images: Option[Images])

case class Legalities(unlimited: Option[String], standard: Option[String], expanded: Option[String])

case class Images(small: Option[String], large: Option[String])

case class Tcgplayer(url: Option[String], updatedAt: Option[String], prices: Option[Prices])

case class Prices(normal: Option[Price], reverseHolofoil: Option[Price])

case class Price(low: Option[Double], mid: Option[Double], high: Option[Double],
                 market: Option[Double], directLow: Option[Double])

case class Cardmarket(url: Option[String],
                      updatedAt: Option[String],
                      prices: Option[CardmarketPrices])

case class CardmarketPrices(averageSellPrice: Option[Double],
                            lowPrice: Option[Double],
                            trendPrice: Option[Double],
                            germanProLow: Option[Double],
                            suggestedPrice: Option[Double],
                            reverseHoloSell: Option[Double],
                            reverseHoloLow: Option[Double],
                            reverseHoloTrend: Option[Double],
                            lowPriceExPlus: Option[Double],
                            avg1: Option[Double],
                            avg7: Option[Double],
                            avg30: Option[Double],
                            reverseHoloAvg1: Option[Double],
                            reverseHoloAvg7: Option[Double],
                            reverseHoloAvg30: Option[Double])

object Card:
  def apply(json: JValue): Card =
    implicit val formats: Formats = Serialization.formats(NoTypeHints)
    val name = (json \ "name").extractOpt[String]
    val id = (json \ "id").extractOpt[String]
    val supertype = (json \ "supertype").extractOpt[String]
    val subtypes = (json \ "subtypes").extractOpt[List[String]]
    val hp = (json \ "hp").extractOpt[String]
    val types = (json \ "types").extractOpt[List[String]]
    val evolvesFrom = (json \ "evolvesFrom").extractOpt[String]
    val abilities = (json \ "abilities").extractOpt[List[Ability]]
    val attacks = (json \ "attacks").extractOpt[List[Attack]]
    val weaknesses = (json \ "weaknesses").extractOpt[List[Weakness]]
    val retreatCost = (json \ "retreatCost").extractOpt[List[String]]
    val convertedRetreatCost = (json \ "convertedRetreatCost").extractOpt[Int]
    val set = (json \ "set").extractOpt[Set]
    val number = (json \ "number").extractOpt[String]
    val artist = (json \ "artist").extractOpt[String]
    val rarity = (json \ "rarity").extractOpt[String]
    val flavorText = (json \ "flavorText").extractOpt[String]
    val nationalPokedexNumbers = (json \ "nationalPokedexNumbers").extractOpt[List[Int]]
    val legalities = (json \ "legalities").extractOpt[Legalities]
    val images = (json \ "images").extractOpt[Images]
    val tcgplayer = (json \ "tcgplayer").extractOpt[Tcgplayer]
    val cardmarket = (json \ "cardmarket").extractOpt[Cardmarket]
    Card(name, id, supertype, subtypes, hp, types, evolvesFrom, abilities, attacks, weaknesses, retreatCost,
      convertedRetreatCost, set, number, artist, rarity, flavorText, nationalPokedexNumbers, legalities,
      images, tcgplayer, cardmarket)
