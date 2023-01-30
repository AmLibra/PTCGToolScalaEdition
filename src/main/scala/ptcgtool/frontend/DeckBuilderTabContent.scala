package ptcgtool
package frontend

import javafx.event.EventHandler
import javafx.scene.input.{MouseEvent, ScrollEvent}
import ptcgtool.api.CardFetcher.fetchCards
import ptcgtool.backend.IOTools.*
import ptcgtool.api.CardFetcher
import ptcgtool.backend.{Card, Deck}
import ptcgtool.frontend.deckBuilderTabContent
import scalafx.Includes.{jfxHBox2sfx, jfxImage2sfx, jfxImageView2sfx, jfxNode2sfx, jfxScrollEvent2sfx, jfxScrollPane2sfx, jfxVBox2sfx}
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.application.Platform.runLater
import scalafx.application.{JFXApp3, Platform}
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Orientation.Horizontal
import scalafx.geometry.Pos.Center
import scalafx.geometry.{Insets, Orientation, Pos}
import scalafx.scene.control.*
import scalafx.scene.control.ScrollPane.ScrollBarPolicy.Never
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.input.ScrollEvent.Scroll
import scalafx.scene.layout.*
import scalafx.scene.layout.BorderStrokeStyle.Solid
import scalafx.scene.paint.Color
import scalafx.scene.paint.Color.{Black, White}
import scalafx.scene.{Node, Scene}
import scalafx.stage.Modality.ApplicationModal
import scalafx.stage.StageStyle.Undecorated
import scalafx.stage.{Modality, Stage, StageStyle, Window}

import java.awt.Toolkit.getDefaultToolkit
import java.awt.{Dimension, Toolkit}
import java.io.{FileInputStream, InputStream}
import scala.collection.parallel.CollectionConverters.{ImmutableIterableIsParallelizable, ImmutableSeqIsParallelizable, IterableIsParallelizable, seqIsParallelizable}
import scala.collection.parallel.ParSeq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.util.{Failure, Success}

private val TOP_SCROLLPANE_HEIGHT = 0.5
private val BOTTOM_SCROLLPANE_HEIGHT = 0.5

val deck = new Deck
var standardOnly = false

def deckBuilderTabContent(windowSize: Dimension): VBox =
  val searchResultsPane: ScrollPane = horizontalImageBoxScrollPane(TOP_SCROLLPANE_HEIGHT, windowSize)
  val deckViewPane = horizontalImageBoxScrollPane(BOTTOM_SCROLLPANE_HEIGHT, windowSize)

  val searchView: VBox = new VBox:
    val searchBarBox: HBox = new HBox:
      decorate(this, 20, 30)
      val statusLabel: Label = SimpleLabel("", 20)

      def toggleStandardOnly(): Unit = standardOnly = !standardOnly

      def fetchCardsAndUpdate(query: String): Unit =
        def processIfTooMany(cards: Seq[Card]): ParSeq[ImageView] =
          val start = System.currentTimeMillis()
          val out = (if cards.size > 150 then processed(cards) else cards.par).map(toSearchResultsView(_, deck, deckViewPane, windowSize))
          println(s"Generating view took ${System.currentTimeMillis() - start} ms")
          out

        statusLabel.text = "Searching..."
        fetchCards(query, standardOnly) map processIfTooMany onComplete {
          case Success(imageViews) => runLater {
            getContent(searchResultsPane).children = imageViews seq;
            statusLabel.text = s"Found ${imageViews.size} cards"
          }
          case Failure(exception) => throw exception
        }

      children = SimpleCheckBox("Search only in Standard", _ => toggleStandardOnly()) ::
        SimpleSearchBar(fetchCardsAndUpdate, windowSize.getWidth * 0.5) ::
        statusLabel :: SimpleButton("Clear Cache", _ => clearCache) :: Nil
    children = Seq(searchBarBox, searchResultsPane)

  val deckView: VBox = new VBox:
    val deckFileName: String = readDeckFolder.head
    readDeck(deckFileName).all foreach deck.add
    updateDeckViewPane(deck, deckViewPane, windowSize)

    val deckStats: VBox = deckViewStatsBox(deck)

    val deckViewHeader: HBox = new HBox:
      decorate(this, 20, 30)
      children = SimpleLabel("Deck: ", 30) ::
        SimpleLabel(removeDeckExtension(deckFileName), 50) ::
        SimpleButton("Save Deck", _ => saveDeck("test", deck)) ::
        deckStats ::
        SimpleButton("Load Deck", _ => loadDecksSelector())
        :: Nil
    children = Seq(deckViewHeader, deckViewPane)

  new VBox:
    style = backgroundIs(DARK_GRAY)
    children = List(searchView, SimpleSeparator(Horizontal), deckView)
