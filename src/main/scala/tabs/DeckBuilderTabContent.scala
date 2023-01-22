package ptcgtool
package tabs

import api.CardFetcher.fetchCards
import api.IOTools.*
import api.{Card, CardFetcher}
import objects.Deck
import tabs.deckBuilderTabContent

import javafx.event.EventHandler
import javafx.scene.input.{MouseEvent, ScrollEvent}
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
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.util.{Failure, Success}


private val TOP_SCROLLPANE_HEIGHT = 0.60
private val BOTTOM_SCROLLPANE_HEIGHT = 0.5

val deck = new Deck

def deckBuilderTabContent(windowSize: Dimension): VBox =
  val searchResultsPane: ScrollPane = horizontalImageBoxScrollPane(TOP_SCROLLPANE_HEIGHT, windowSize)
  val deckViewPane = horizontalImageBoxScrollPane(BOTTOM_SCROLLPANE_HEIGHT, windowSize)

  val searchView: VBox = new VBox:
    val searchBarBox: HBox = new HBox:
      decorate(this, 20, 30)

      def fetchCardsAndUpdate(query: String): Unit =
        def updateSearchResultsPane(imageViews: Seq[ImageView]): Unit =
          runLater {
            // limit the number of search results displayed to 100
            val maxCurrentDisplayed = 200

            getContent(searchResultsPane).children = imageViews.take(maxCurrentDisplayed)
            // update the scrollpane's chidren as you scroll:
            // if the imageViews total is lower than the currently displayed (max 200), then don't update
            // if we scroll more than the first 80% of images then remove the first 50 images and add the next 50
            // if we scroll less than the first 20% of images then remove the last 50 images and add the previous 50
            // we need to update the scrollpane's children to avoid memory leaks and set the removed images to null
            // to avoid memory leaks
            // this is all done in scalaFx's runLater to avoid concurrency issues with the UI
            searchResultsPane.onScroll = (event: ScrollEvent) =>
              if (imageViews.length > maxCurrentDisplayed) {
                val scrollPercent = event.getDeltaY / searchResultsPane.getWidth
                println(s"scrollPercent: ${event.getDeltaY}")
                val currentDisplayed = getContent(searchResultsPane).children.length
                if (scrollPercent > 0.8 && currentDisplayed < imageViews.length) {
                  getContent(searchResultsPane).children = imageViews.slice(currentDisplayed, currentDisplayed + 50)
                    ++ imageViews.slice(currentDisplayed - 50, currentDisplayed)
                } else if (scrollPercent < 0.2 && currentDisplayed > maxCurrentDisplayed) {
                  getContent(searchResultsPane).children = imageViews.slice(currentDisplayed - 100, currentDisplayed - 50)
                    ++ imageViews.slice(currentDisplayed - 50, currentDisplayed)

                }
              }
          }

        def toSearchResultsView(card: Card): ImageView =
          val imageView = cardImageView(card, 0.4 * windowSize.height, _ => deckManagerCardWindow(card, deck, deckViewPane, windowSize))
          zoomOnHover(imageView)
          imageView

        fetchCards(query).map(cards => cards.par map toSearchResultsView) onComplete {
          case Success(imageViews) => updateSearchResultsPane(imageViews.seq)
          case Failure(exception) => println(s"Failed to fetch cards: $exception")
        }

      children = SimpleSearchBar(fetchCardsAndUpdate, windowSize.getWidth * 0.5) ::
        SimpleButton("Clear Cache", _ => clearCache) :: Nil
    children = Seq(searchBarBox, searchResultsPane)

  val deckView: VBox = new VBox:
    val deckFileName: String = readDeckFolder.head
    readDeck(deckFileName).all foreach deck.add
    updateDeckViewPane(deck, deckViewPane, windowSize, () => ())

    val deckViewHeader: HBox = new HBox:
      decorate(this, 20, 30)
      children = SimpleLabel("Deck: ", 50) ::
        SimpleLabel(removeDeckExtension(deckFileName), 50) ::
        SimpleButton("Save Deck", _ => saveDeck("test", deck)) :: Nil
    children = Seq(deckViewHeader, deckViewPane)

  new VBox:
    style = backgroundIs(DARK_GRAY)
    children = List(searchView, SimpleSeparator(Horizontal), deckView)
