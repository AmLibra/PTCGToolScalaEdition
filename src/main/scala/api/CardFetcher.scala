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

/**
 * Sample response:
 * {
  "id": "swsh4-25",
  "name": "Charizard",
  "supertype": "Pokémon",
  "subtypes": [
    "Stage 2"
  ],
  "hp": "170",
  "types": [
    "Fire"
  ],
  "evolvesFrom": "Charmeleon",
  "abilities": [
    {
      "name": "Battle Sense",
      "text": "Once during your turn, you may look at the top 3 cards of your deck and put 1 of them into your hand. Discard the other cards.",
      "type": "Ability"
    }
  ],
  "attacks": [
    {
      "name": "Royal Blaze",
      "cost": [
        "Fire",
        "Fire"
      ],
      "convertedEnergyCost": 2,
      "damage": "100+",
      "text": "This attack does 50 more damage for each Leon card in your discard pile."
    }
  ],
  "weaknesses": [
    {
      "type": "Water",
      "value": "×2"
    }
  ],
  "retreatCost": [
    "Colorless",
    "Colorless",
    "Colorless"
  ],
  "convertedRetreatCost": 3,
  "set": {
    "id": "swsh4",
    "name": "Vivid Voltage",
    "series": "Sword & Shield",
    "printedTotal": 185,
    "total": 203,
    "legalities": {
      "unlimited": "Legal",
      "standard": "Legal",
      "expanded": "Legal"
    },
    "ptcgoCode": "VIV",
    "releaseDate": "2020/11/13",
    "updatedAt": "2020/11/13 16:20:00",
    "images": {
      "symbol": "https://images.pokemontcg.io/swsh4/symbol.png",
      "logo": "https://images.pokemontcg.io/swsh4/logo.png"
    }
  },
  "number": "25",
  "artist": "Ryuta Fuse",
  "rarity": "Rare",
  "flavorText": "It spits fire that is hot enough to melt boulders. It may cause forest fires by blowing flames.",
  "nationalPokedexNumbers": [
    6
  ],
  "legalities": {
    "unlimited": "Legal",
    "standard": "Legal",
    "expanded": "Legal"
  },
  "images": {
    "small": "https://images.pokemontcg.io/swsh4/25.png",
    "large": "https://images.pokemontcg.io/swsh4/25_hires.png"
  },
  "tcgplayer": {
    "url": "https://prices.pokemontcg.io/tcgplayer/swsh4-25",
    "updatedAt": "2021/08/04",
    "prices": {
      "normal": {
        "low": 1.73,
        "mid": 3.54,
        "high": 12.99,
        "market": 2.82,
        "directLow": 3.93
      },
      "reverseHolofoil": {
        "low": 3,
        "mid": 8.99,
        "high": 100,
        "market": 3.89,
        "directLow": 4.46
      }
    }
  },
  "cardmarket": {
    "url": "https://prices.pokemontcg.io/cardmarket/swsh4-25",
    "updatedAt": "2021/08/04",
    "prices": {
      "averageSellPrice": 9.38,
      "lowPrice": 8.95,
      "trendPrice": 10.29,
      "germanProLow": null,
      "suggestedPrice": null,
      "reverseHoloSell": null,
      "reverseHoloLow": null,
      "reverseHoloTrend": null,
      "lowPriceExPlus": 8.95,
      "avg1": 9.95,
      "avg7": 9.35,
      "avg30": 11.31,
      "reverseHoloAvg1": null,
      "reverseHoloAvg7": null,
      "reverseHoloAvg30": null
    }
  }
}
 */


class CardFetcher {
  // fields need default values for json4s to work therefore they are all Option
  case class CardJSON(name: Option[String],
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
                      cardmarket: Option[Cardmarket])

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

  private object CardJSON {
    def apply(json: JValue): CardJSON = {
      implicit val formats: Formats = Serialization.formats(NoTypeHints)
      val name = (json \ "name").extractOpt[String]
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
      CardJSON(name, supertype, subtypes, hp, types, evolvesFrom, abilities, attacks, weaknesses, retreatCost,
        convertedRetreatCost, set, number, artist, rarity, flavorText, nationalPokedexNumbers, legalities,
        images, tcgplayer, cardmarket)
    }
  }


  // https://api.pokemontcg.io/v2/cards?q=name:charizard&x-api-key=2e04bfdb-5eb1-4eaf-9c3d-316a1abb6c70
  // is the url to get the card data of charizard cards
  // this returns a json object with a data field that contains a list of cards
  // the cards have an id, name, imageUrl, and types field

  private val apiKey = "2e04bfdb-5eb1-4eaf-9c3d-316a1abb6c70"
  private val baseUrl = "https://api.pokemontcg.io/v2/cards"

  // fetchCards but asynchonously
  private def fetchCardsAsync(name: String): Future[List[CardJSON]] =
    implicit val formats: Formats = Serialization.formats(NoTypeHints)
    val url = s"$baseUrl?q=name:$name&x-api-key=$apiKey"
    Future {
      val response = Source.fromInputStream(new URL(url).openStream()).mkString
      val json = parse(response)
      // extract the data field from the json object, and get a list o JSON cards
      val data = (json \ "data").extract[List[JValue]]
      // convert the list of JSON cards to a list of CardJSON objects
      data.map(CardJSON(_))
    }

  def testAPI(): Unit =
    val cardsFuture = fetchCardsAsync("charizard")
    cardsFuture.onComplete {
      case scala.util.Success(cards) =>
        cards.foreach(card => println(card.name.get + " " + card.images.get.small.get))
      case scala.util.Failure(exception) =>
        println(exception)
    }
}