package ptcgtool
package objects

import java.awt.Panel
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import scala.language.postfixOps
import scala.swing.Graphics2D


class ImagePanel extends Panel:
  private var _imagePath = ""
  private var bufferedImage: BufferedImage = _

  def imagePath: String = _imagePath

  def imagePath_=(value: String): Unit =
    _imagePath = value
    bufferedImage = ImageIO.read(new File(_imagePath))

object ImagePanel:
  def apply(path: String): ImagePanel =
    new ImagePanel{ imagePath_=(path) }
