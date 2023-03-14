package ptcgtool
package backend

import ptcgtool.api.CardFetcher
import ptcgtool.api.CardFetcher.fetchCard

import scala.collection.parallel.CollectionConverters.{ImmutableIterableIsParallelizable, ImmutableSeqIsParallelizable, IterableIsParallelizable, seqIsParallelizable}
import scala.collection.parallel.ParSeq
import scala.concurrent.Await.result
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.sequence
import scala.concurrent.duration.Duration.Inf
import scala.language.postfixOps
import scala.util.{Failure, Success}


def parseDeck(deckStringLines: List[String]): Deck =
  def nonMeaningfulLines(line: String): Boolean =
    !((line isEmpty) || (line startsWith "Pokémon") || (line startsWith "Trainer") || (line startsWith "Energy"))

  def parseCardLine(line: String): (Future[Card], Int) =
    val splitLine = line.split(" ")
    (fetchCard(splitLine last), (splitLine head) toInt)

  val cardLines = deckStringLines filter nonMeaningfulLines
  val (cardFutures, cardCounts) = (cardLines.par map parseCardLine).seq unzip
  val cardsToCounts = result(sequence(cardFutures), Inf) zip cardCounts toMap

  Deck(cardsToCounts)

final class Deck:

  private var cards: List[Card] = Nil

  private def size: Int = cards size

  def this(cards: Seq[Card]) =
    this()
    this.cards = cards toList

  def this(cardsAndAmounts: Map[Card, Int]) =
    this()
    cardsAndAmounts foreach { case (card, amount) => add(card, amount) }

  def countOf(card: Card): Int = cards count (_ == card)

  def countOf(cardName: String): Int = cards count (_.name == cardName)

  def all: Seq[Card] =
    sort()
    cards

  def allWithAmounts: Map[Card, Int] =
    sort()
    cards.distinct map (card => (card, countOf(card))) toMap

  def pokemonCards: Seq[Card] =
    sort()
    cards filter (_.isPokemon)

  def trainerCards: Seq[Card] =
    sort()
    cards filter (_.isTrainer)

  def energyCards: Seq[Card] =
    sort()
    cards filter (_.isEnergy)

  def add(card: Card): Unit = cards = card :: cards

  def add(cards: Seq[Card]): Unit = cards foreach add

  def add(card: Card, amount: Int): Unit = (1 to amount) foreach (_ => add(card))

  def add(cardsAndAmounts: Map[Card, Int]): Unit = cardsAndAmounts foreach { case (card, amount) => add(card, amount) }

  def remove(card: Card): Unit = cards = cards diff List(card)

  private def sort(): Unit =
    def sortedByNameAndId(cards: List[Card]): List[Card] = cards sortBy (card => (card name, card id))

    def byLabelType(types: (List[Card], List[Card], List[Card]), card: Card): (List[Card], List[Card], List[Card]) =
      if card isPokemon then (card :: types._1, types._2, types._3)
      else if card isTrainer then (types._1, card :: types._2, types._3)
      else (types._1, types._2, card :: types._3)

    val cardsFold = cards.par foldLeft( (List.empty[Card], List.empty[Card], List.empty[Card]) )
    val (pokemonCards, trainerCards, energyCards) = cardsFold (byLabelType)

    cards = sortedByNameAndId(pokemonCards) ++ sortedByNameAndId(trainerCards) ++ sortedByNameAndId(energyCards)

  def contains(card: Card): Boolean = cards contains card

  def isStandardLegal: Boolean =
    val uniqueNameCardCounts = cards.map(_.name).distinct map countOf
    size == 60 && (uniqueNameCardCounts forall(_ <= 4)) && (cards exists (_.isBasic)) && (cards forall (_.isStandardLegal))

  override def toString: String =
    def linesOf(cards: Seq[Card]): String =
      val uniqueCardIds = cards map (_.id) distinct
      val cardCounts = cards.distinct map countOf
      val cardNames = uniqueCardIds map (id => cards.find(_.id == id).get name)
      val cardLines =
        cardCounts zip cardNames zip uniqueCardIds map { case ((count, name), id) => s"$count $name $id" }
      cardLines mkString "\n"

    def printSection(header: String, cards: Seq[Card]): String = s"$header (${cards size}):\n${linesOf(cards)}\n"

    val sections = ("Pokémon", pokemonCards) :: ("Trainer", trainerCards) :: ("Energy", energyCards) :: Nil
    sections map { case (header, cards) => printSection(header, cards) } mkString "\n"

