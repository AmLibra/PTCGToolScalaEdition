package ptcgtool
package tabs

import scalafx.scene.control.Label
import scalafx.scene.layout.VBox

import java.awt.Dimension


def statsTabContent(windowSize: Dimension): VBox =
  new VBox:
    children = List(new Label("Stats - WIP"))