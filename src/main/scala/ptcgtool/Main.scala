package ptcgtool

import ptcgtool.api.CardFetcher.{generateSearchDirectoryV2, updateDB}
import ptcgtool.frontend.{DARK_GRAY, TAB_STYLE, TabLabel, backgroundIs, deckBuilderTabContent, statsTabContent}
import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.control.TabPane.TabClosingPolicy.Unavailable
import scalafx.scene.control.{Tab, TabPane}

import java.awt.Dimension
import java.awt.Toolkit.getDefaultToolkit
import scala.language.postfixOps

object Main extends JFXApp3:

  private final val SCREEN_SIZE = getDefaultToolkit getScreenSize
  private final val WINDOW_SIZE = Dimension((SCREEN_SIZE.width * 0.9) toInt, (SCREEN_SIZE.height * 0.9) toInt)

  private final val APP_NAME = "PTCG Tool"
  private final val DECK_BUILDING_TAB_NAME = "Deck Builder"
  private final val STATS_TAB_NAME = "Stats"

  override def start(): Unit =
    new PrimaryStage:
      title = APP_NAME
      scene = new Scene(WINDOW_SIZE.width, WINDOW_SIZE.height):
        val deckBuilderTab: Tab = new Tab:
          style = TAB_STYLE
          graphic = TabLabel(DECK_BUILDING_TAB_NAME)
          content = deckBuilderTabContent(WINDOW_SIZE)
        val statsTab: Tab = new Tab:
          style = TAB_STYLE
          graphic = TabLabel(STATS_TAB_NAME)
          content = statsTabContent(WINDOW_SIZE)
        root = new TabPane:
          tabClosingPolicy = Unavailable
          style = backgroundIs(DARK_GRAY)
          tabs = List(deckBuilderTab, statsTab)
