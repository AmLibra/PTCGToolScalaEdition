package ptcgtool
package backend

import api.CardFetcher
import api.CardFetcher.fetchCard

import scala.concurrent.Await.result
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.sequence
import scala.concurrent.duration.Duration.Inf
import scala.language.postfixOps
import scala.util.{Failure, Success}
import collection.parallel.CollectionConverters.ImmutableSeqIsParallelizable
import collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable
import collection.parallel.CollectionConverters.seqIsParallelizable
import collection.parallel.CollectionConverters.IterableIsParallelizable


def parseDeck(deckStringLines: List[String]): Deck =
  def headerLineOrEmpty(line: String): Boolean =
    (line isEmpty) || (line startsWith "Pokemon") || (line startsWith "Trainer") || (line startsWith "Energy")

  def parseCardLine(line: String): (Future[Card], Int) =
    val id = line.split(" ").last // some cards have spaces in their name
    val count = line.split(" ").head.toInt
    (fetchCard(id), count)

  val cardLines = deckStringLines filterNot headerLineOrEmpty // filter out the header lines and empty lines

  val (cardFutures, cardCounts) = cardLines.par.map(parseCardLine).seq unzip

  val cardsToCounts = result(sequence(cardFutures), Inf) zip cardCounts toMap

  Deck(cardsToCounts)

final class Deck:

  private var cards: List[Card] = List.empty[Card]
  private def size: Int = cards.size

  def this(cards: Seq[Card]) =
    this()
    this.cards = cards.toList

  def this(cardsAndAmounts: Map[Card, Int]) =
    this()
    cardsAndAmounts.foreach { case (card, amount) => add(card, amount) }

  def countOf(card: Card): Int = cards.count(_ == card)

  def countOf(cardName: String): Int = cards.count(_.name == cardName)

  def all: Seq[Card] =
    sort
    cards

  def allWithAmounts: Map[Card, Int] =
    sort
    val uniqueCards = cards.distinct
    uniqueCards.map(card => (card, countOf(card))).toMap

  def pokemonCards: Seq[Card] = cards.filter(_.isPokemon).sortBy(_.name)

  def trainerCards: Seq[Card] = cards.filter(_.isTrainer).sortBy(_.name)

  def energyCards: Seq[Card] = cards.filter(_.isEnergy).sortBy(_.name)

  // add a card to the deck
  def add(card: Card): Unit = cards = card :: cards

  // add multiple cards to the deck
  def add(cards: Seq[Card]): Unit = cards.foreach(add)

  // add a certain amount of cards to the deck
  def add(card: Card, amount: Int): Unit = (1 to amount).foreach(_ => add(card))

  def add(cardsAndAmounts: Map[Card, Int]): Unit =
    cardsAndAmounts.foreach { case (card, amount) => add(card, amount) }

  // remove a card from the deck
  def remove(card: Card): Unit = cards = cards.diff(List(card))

  // sort the deck: first by card type (Pokemon > Trainer > Energy), then by card name
  def sort: Unit =
    val (pokemon, trainersAndEnergies) = cards.partition(_.isPokemon)
    val (trainers, energies) = trainersAndEnergies.partition(_.isTrainer)
    cards = pokemon.sortBy(card => (card.name, card.id)) ++ trainers.sortBy(card => (card.name, card.id)) ++ energies.sortBy(card => (card.name, card.id))

  // check if the deck contains a card
  def contains(card: Card): Boolean = cards.contains(card)

  // checks if the deck is legal:
  // a deck is legal if it contains exactly 60 cards, a maximum of 4 copies of each card name that is not a basic energy card
  // and at least 1 basic pokemon card, ruleboxes should be checked separately
  def isLegalStandard: Boolean =
    val cardNames = cards.map(_.name)
    val uniqueNameCardCounts = cardNames.distinct map countOf
    val basicPokemonCount = cards.count(_.isBasicPokemon)
    val allCardsStandardLegal = cards.forall(_.isStandardLegal)
    size == 60 && uniqueNameCardCounts.forall(_ <= 4) && basicPokemonCount >= 1 && allCardsStandardLegal

  // override toString to print the deck in 3 sections: Pokemon, Trainers and Energy
  // header is the name of the deck and the header of each section should look like this:
  // Header (Count: 10)
  // each card with a different id should be printed on a new line
  // the format of a card line is "x count name id", every card should get a unique line
  override def toString: String =
    def print(cards: Seq[Card]): String =
      val uniqueCardIds = cards map (_.id) distinct
      val cardCounts = cards.distinct map countOf
      val cardNames = uniqueCardIds map (id => cards.find(_.id == id).get.name)
      val cardLines =
        cardCounts zip cardNames zip uniqueCardIds map { case ((count, name), id) => s"$count $name $id" }
      cardLines.mkString("\n")

    val pokemonLines = print(pokemonCards)
    val trainerLines = print(trainerCards)
    val energyLines = print(energyCards)

    val pokemonHeader = s"Pokemon (${pokemonCards.size}):\n"
    val trainerHeader = s"Trainers (${trainerCards.size}):\n"
    val energyHeader = s"Energy (${energyCards.size}):\n"

    pokemonHeader + s"$pokemonLines\n" +
      trainerHeader + s"$trainerLines\n" +
      energyHeader + s"$energyLines"

