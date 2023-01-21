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
import scalafx.scene.paint.Color.{Black, Transparent, White}
import scalafx.stage.Modality.ApplicationModal
import scalafx.stage.{Stage, StageStyle}
import scalafx.stage.StageStyle.{TRANSPARENT, Undecorated}

import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import java.awt.Dimension
import java.io.FileInputStream
import scala.util.Try

private val SCROLL_SPEED = 1.5

private val DARK_GRAY = "#2b2b2b"

def Separator(o: Orientation): Separator =
  new Separator:
    orientation = o

def SimpleButton(text: String, eventHandler: EventHandler[_ >: MouseEvent]): Button =
  new Button(text):
    onMouseClicked = eventHandler
    // no blue border on click
    style = "fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-insets: 0;"
    + s"-fx-font-size: 20; -fx-font-weight: bold; -fx-font-family: \"Arial\" "

def SimpleSearchBar(search: String => Unit, size: Double): HBox =
  def SimpleSearchField(search: String => Unit): TextField =
    new TextField:
      style = "fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-insets: 0; " +
        "-fx-background-radius: 0; -fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 20; " +
        "-fx-font-weight: bold; -fx-font-family: \"Arial\"; -fx-prompt-text-fill: white; -fx-prompt-text-font-size: 20; " +
        "-fx-prompt-text-font-weight: bold; -fx-prompt-text-font-family: \"Arial\";"
      promptText = "Search for a card..."
      onAction = _ => search(text.value)
      prefWidth = size

  new HBox:
    val textField: TextField = SimpleSearchField(search)
    val searchButton: Button = SimpleButton("Search", _ => search(textField.text.value))
    children = Seq(textField, searchButton)

def horizontalImageBoxScrollPane(verticalRatio: Double, windowSize: Dimension): ScrollPane =
  new ScrollPane:
    // make the scroll bar invisible
    style = s"-fx-background-color: $DARK_GRAY; -fx-background: $DARK_GRAY; -fx-control-inner-background: $DARK_GRAY; "
    val imageList: HBox = new HBox:
      decorate(this, 10, 10)
      style = s"-fx-background-color: $DARK_GRAY "
    content = imageList
    prefViewportWidth = windowSize.width
    prefViewportHeight = windowSize.height * verticalRatio
    hbarPolicy = Never
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
      initStyle(StageStyle.Transparent)
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
    // transparent background
    fill = Transparent
    val anchor = new AnchorPane:
      // transparent background
      style = "-fx-background-color: transparent"
      val cardImage: ImageView = cardImageView(card, 0.8 * windowSize.height, _ => ())
      val cardCount: Label = SimpleLabel(deck.countOf(card).toString, 50)
      cardCount.setStyle("-fx-font-size: 50; -fx-font-weight: bold; -fx-font-family: \"Arial\"; -fx-text-fill: white; " +
        // text has a black border
        "-fx-stroke: black; -fx-stroke-width: 1; -fx-stroke-type: outside; " )
      val cardAdd: Button = SimpleButton("+", _ => deckAdd(card))
      val cardRemove: Button = SimpleButton("-", _ => deckRemove(card))
      val cardCountBox: HBox = new HBox:
        decorate(this, 0, 10)
        style = "-fx-background-color: transparent"
        children = Seq(cardRemove, cardCount, cardAdd)
      children = Seq(cardImage, cardCountBox)
      AnchorPane.setTopAnchor(cardImage, 0.0)
      AnchorPane.setBottomAnchor(cardCountBox, -20.0)
      AnchorPane.setLeftAnchor(cardCountBox, 0.0)
      AnchorPane.setRightAnchor(cardCountBox, 0.0)
    content = anchor
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
  box.style = s"-fx-background-color: $DARK_GRAY ; -fx-background: transparent; -fx-control-inner-background: transparent; " +
    "-fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-insets: 0; -fx-background-radius: 0; " +
    "-fx-border-color: transparent; -fx-border-width: 0; -fx-border-radius: 0; -fx-border-insets: 0; -fx-border-style: solid; "
  box.padding = Insets(padding)
  box.spacing = spacing
  box.alignment = Center

def decorate(box: VBox, padding: Double, spacing: Double): Unit =
  box.style = s"-fx-background-color: $DARK_GRAY ; -fx-background: transparent; -fx-control-inner-background: transparent; " +
    "-fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-insets: 0; -fx-background-radius: 0; " +
    "-fx-border-color: transparent; -fx-border-width: 0; -fx-border-radius: 0; -fx-border-insets: 0; -fx-border-style: solid; "
  box.padding = Insets(padding)
  box.spacing = spacing
  box.alignment = Center

// label with a certain text and size
def SimpleLabel(text: String, size: Double): Label =
  new Label(text):
    style = s"-fx-font-size: ${size}px; -fx-font-size: 20; -fx-font-weight: bold; -fx-font-family: \"Arial\" "


