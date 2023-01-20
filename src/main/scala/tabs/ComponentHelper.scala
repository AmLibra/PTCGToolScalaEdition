package ptcgtool
package tabs

import api.Card
import objects.Deck

import javafx.event.EventHandler
import javafx.scene.input.{MouseEvent, ScrollEvent}
import scalafx.Includes.jfxHBox2sfx
import scalafx.application.Platform.runLater
import scalafx.geometry.Pos.Center
import scalafx.geometry.{Insets, Orientation}
import scalafx.scene.Scene
import scalafx.scene.control.ScrollPane.ScrollBarPolicy.Never
import scalafx.scene.control.*
import scalafx.scene.image.ImageView
import scalafx.scene.input.ScrollEvent.Scroll
import scalafx.scene.layout.*
import scalafx.scene.layout.BorderStrokeStyle.Solid
import scalafx.scene.paint.Color.{Black, White}
import scalafx.stage.Modality.ApplicationModal
import scalafx.stage.Stage
import scalafx.stage.StageStyle.Undecorated

import java.awt.Dimension

private val SCROLL_SPEED = 1.5

def Separator(o: Orientation): Separator =
  new Separator:
    orientation = o

def SimpleButton(text: String, eventHandler: EventHandler[_ >: MouseEvent]): Button =
  new Button(text):
    onMouseClicked = eventHandler

def SimpleSearchBar(search: String => Unit): HBox =
  def SimpleSearchField(search: String => Unit): TextField =
    new TextField:
      promptText = "Search for a card..."
      onAction = _ => search(text.value)

  new HBox:
    val textField: TextField = SimpleSearchField(search)
    val searchButton: Button = SimpleButton("Search", _ => search(textField.text.value))
    children = Seq(textField, searchButton)

def horizontalImageBoxScrollPane(verticalRatio: Double, windowSize: Dimension): ScrollPane =
  new ScrollPane:
    val imageList: HBox = new HBox
    content = imageList
    prefViewportWidth = windowSize.width
    prefViewportHeight = windowSize.height * verticalRatio
    vbarPolicy = Never
    style = "-fx-background-color: transparent; -fx-border-color: transparent;" // let the background show through
    fitToWidth = true
    fitToHeight = true
    addEventFilter(Scroll, (event: ScrollEvent) => {
      val deltaY = -(event.getDeltaY * SCROLL_SPEED)
      val width = imageList.width.get - this.getViewportBounds.getWidth
      val hValue = this.getHvalue
      this.setHvalue(hValue - (deltaY / width))
    })


def deckManagerCardWindow(card: Card, deck: Deck, deckViewPane: ScrollPane, windowSize: Dimension): Unit =
  def popup(content: Scene): Unit =
    new Stage:
      initModality(ApplicationModal)
      initStyle(Undecorated)
      content.setOnMouseClicked(_ => close())
      scene = content
      centerOnScreen()
      show()

  val cardCount: Label = new Label("0"):
    style = "-fx-text-fill: white;"
  val cardCountUpdater: Runnable = () => cardCount.text = deck.countOf(card).toString

  def updateDeckView(): Unit =
    def toDeckCardView(card: Card): ImageView =
      cardImageView(card, 0.25, windowSize, _ => deckManagerCardWindow(card, deck, deckViewPane, windowSize))

    runLater {
      getContent(deckViewPane).children = deck.all map toDeckCardView
      cardCountUpdater
    }

  def deckAdd(card: Card): Unit =
    deck.add(card)
    updateDeckView()

  def deckRemove(card: Card): Unit =
    deck.remove(card)
    updateDeckView()

  popup(new Scene {
    content = new VBox:
      val image: ImageView = cardImageView(card, 0.8, windowSize, _ => ())
      val buttons: HBox = new HBox:
        val addButton: Button = SimpleButton("+", _ => deckAdd(card))
        val removeButton: Button = SimpleButton("-", _ => deckRemove(card))
        children = Seq(removeButton, cardCount, addButton)
      children = Seq(image, buttons)
      runLater(cardCountUpdater)
  })

// have to specify type because of type erasure
def getContent(scrollPane: ScrollPane): HBox = scrollPane.content.value.asInstanceOf[javafx.scene.layout.HBox]

def cardImageView(card: Card, scale: Double, windowSize: Dimension, eventHandler: EventHandler[_ >: MouseEvent]): ImageView =
  new ImageView(card.getImg):
    fitHeight = windowSize.height * scale
    preserveRatio = true
    onMouseClicked = eventHandler

