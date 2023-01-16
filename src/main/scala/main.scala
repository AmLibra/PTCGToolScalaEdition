package ptcgtool

import ptcgtool.api.CardFetcher

import scala.swing.*

//get current screen size
val screenSize = java.awt.Toolkit.getDefaultToolkit.getScreenSize
// 80% of screen size
val windowSize = new Dimension((screenSize.width * 0.8).toInt, (screenSize.height * 0.8).toInt)

// the Tab from which we can build a deck
val deckBuilderTab = TabbedPane.Page("Deck Builder", new BoxPanel(Orientation.Vertical) {
  //deck name
  contents += new Label("Deck Name:")
  contents += Swing.VStrut(5)
  val deckName = new Label("Deck Name")
  contents += deckName
  // Horizontal list of cards, displays the cards in the Deck with their images
  val cardList = new ListView[Object]()
  contents += new ScrollPane(cardList)
  //separator
  contents += Swing.VStrut(10)
  //search Cards
  contents += new Label("Search Cards:")
  contents += Swing.VStrut(5)
  val searchCards = new TextField(20)
  contents += searchCards
})

val statsTab = new TabbedPane.Page("Stats", new BoxPanel(Orientation.Vertical) {
  contents += new Label("Stats")
})

//create main window
val frame: MainFrame = new MainFrame {
  title = "PTCG Tool"
  contents = new TabbedPane {
    pages += deckBuilderTab
    pages += statsTab
  }
  //set window size
  minimumSize = windowSize
  preferredSize = windowSize
  centerOnScreen()
}
@main
def main(): Unit =
  println("Hello world!")
  val cardFetcher = new CardFetcher
  cardFetcher.testAPI()
  //frame.visible = true
