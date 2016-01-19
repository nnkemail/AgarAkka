package model.Util

import play.libs.Akka
import akka.actor._
import scala.util.Random
import java.util.concurrent.atomic.AtomicInteger
import java.awt.Color


object util {
  case class sampleColor (r: Int, g: Int, b: Int);
  var colors = Array(
        sampleColor(235, 75, 0),
        sampleColor(225, 125, 255),
        sampleColor(180, 170, 240),
        sampleColor(180, 90, 135),
        sampleColor(195, 240, 0),
        sampleColor(150, 18, 255),
        sampleColor(80, 245, 0),
        sampleColor(165, 25, 0),
        sampleColor(80, 145, 0),
        sampleColor(80, 170, 240),
        sampleColor(55, 92, 255) )       

  val system = akka.actor.ActorSystem("system")   //TODO ?
  val settings = Settings(system)
  var random: Random = new Random();
 
//	def getRandomPosition(): Position = {
//			Position( (random.nextDouble() * (Math.abs( settings.border.left) + Math.abs( settings.border.right))) / 2.0D,
//					(random.nextDouble() * (Math.abs(settings.border.top) + Math.abs(settings.border.bottom))) / 2.0D);
//	}

  def getRandomPosition(): Position = {
			Position( (random.nextDouble() * (Math.abs( settings.border.left) + Math.abs( settings.border.right))),
					(random.nextDouble() * (Math.abs(settings.border.top) + Math.abs(settings.border.bottom))));
	}
	def getRandomColor(): Color = {
		//to get rainbow, pastel colors
	  
		//var hue: Float = random.nextFloat();
		//var saturation = 0.9f;//1.0 for brilliant, 0.0 for dull
		//var luminance = 1.0f; //1.0 for brighter, 0.0 for black
		//var color: Color = Color.getHSBColor(hue, saturation, luminance);
		//color
		
	  val ri = random.nextInt(colors.length);
    var color: Color = new Color(colors(ri).r, colors(ri).g, colors(ri).b)
    color
	}
	
	case class RoomDescription(title: String, lat: Double, lng: Double, var roomActor: ActorRef, serverAddress: String)
	
  private val sysId = new AtomicInteger()
  def nextSysId() = sysId.incrementAndGet()
}