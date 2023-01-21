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
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.input.ScrollEvent.Scroll
import scalafx.scene.layout.*
import scalafx.scene.layout.BorderStrokeStyle.Solid
import scalafx.scene.paint.Color.{Black, White}
import scalafx.stage.Modality.ApplicationModal
import scalafx.stage.Stage
import scalafx.stage.StageStyle.Undecorated

import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import java.awt.Dimension
import java.io.FileInputStream
import scala.util.Try

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
    val imageList: HBox = new HBox:
      decorate(this, 10, 10)

    content = imageList
    prefViewportWidth = windowSize.width
    prefViewportHeight = windowSize.height * verticalRatio
    vbarPolicy = Never
    fitToWidth = true
    fitToHeight = true
    addEventFilter(Scroll, (event: ScrollEvent) => {
      val deltaY = -(event.getDeltaY * SCROLL_SPEED)
      val width = imageList.width.get - this.getViewportBounds.getWidth
      val hValue = this.getHvalue
      this.setHvalue(hValue - (deltaY / width))
    })


def toDeckCardView(card: Card, deck: Deck, deckViewPane: ScrollPane, windowSize: Dimension): VBox =
  new VBox:
    decorate(this, 10, 10)
    val image: ImageView = cardImageView(card, 0.25 * windowSize.height, _ => deckManagerCardWindow(card, deck, deckViewPane, windowSize))
    val count: Label = Label(deck.countOf(card).toString)
    children = Seq(image, count)

def deckManagerCardWindow(card: Card, deck: Deck, deckViewPane: ScrollPane, windowSize: Dimension): Unit =
  def popup(content: Scene): Unit =
    new Stage:
      initModality(ApplicationModal)
      initStyle(Undecorated)
      content.setOnMouseClicked(_ => close())
      scene = content
      centerOnScreen()
      show()

  val cardCount: Label = SimpleLabel("-1", 50)
  val updateCardCount: Runnable = () => cardCount.text = deck.countOf(card).toString
  def updateDeckView(): Unit =
    runLater {
      getContent(deckViewPane).children = deck.all.distinct map (toDeckCardView(_, deck, deckViewPane, windowSize))
      updateCardCount
    }

  def deckAdd(card: Card): Unit =
    deck.add(card)
    updateDeckView()

  def deckRemove(card: Card): Unit =
    deck.remove(card)
    updateDeckView()

  popup(new Scene {
    content = new VBox:
      decorate(this, 10, 10)
      val image: ImageView = cardImageView(card, 0.8 * windowSize.height, _ => ())
      val buttons: HBox = new HBox:
        decorate(this, 10, 10)
        val addButton: Button = SimpleButton("+", _ => deckAdd(card))
        val removeButton: Button = SimpleButton("-", _ => deckRemove(card))
        children = Seq(removeButton, cardCount, addButton)
      children = Seq(image, buttons)
      runLater(updateCardCount)
  })

// have to specify type because of type erasure
def getContent(scrollPane: ScrollPane): HBox = scrollPane.content.value.asInstanceOf[javafx.scene.layout.HBox]

def cardImageView(card: Card, size: Double, eventHandler: EventHandler[_ >: MouseEvent]): ImageView =
  val cardImage = Try(card.getImg).toOption match
    case Some(i) => i
    case None => Image(FileInputStream("C:\\Users\\usr\\IdeaProjects\\PTCGToolScalaEdition\\src\\resources\\emptyCard.png"))
  new ImageView(cardImage):
    fitHeight = size
    preserveRatio = true
    onMouseClicked = eventHandler


// add padding, spacing and center alignment to a VBox or HBox
def decorate(box: HBox, padding: Double, spacing: Double): Unit =
  box.padding = Insets(padding)
  box.spacing = spacing
  box.alignment = Center

def decorate(box: VBox, padding: Double, spacing: Double): Unit =
  box.padding = Insets(padding)
  box.spacing = spacing
  box.alignment = Center

// label with a certain text and size
def SimpleLabel(text: String, size: Double): Label =
  new Label(text):
    style = s"-fx-font-size: ${size}px"


