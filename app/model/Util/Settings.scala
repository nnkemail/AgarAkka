package model.Util

import akka.actor.ActorSystem
import akka.actor.Extension
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import akka.actor.ExtendedActorSystem
import scala.concurrent.duration.Duration
import com.typesafe.config.{ConfigObject, ConfigValue, ConfigFactory, Config}
import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters._
import java.util.Map.Entry
import model.Util.util._

class SettingsImpl(config: Config) extends Extension {
  object border {
    val left = config.getInt("myApp.border.left") 
    val right = config.getInt("myApp.border.right") 
    val top = config.getInt("myApp.border.top") 
    val bottom = config.getInt("myApp.border.bottom") 
  }
  
  object view {
    val baseX = config.getInt("myApp.view.baseX")
    val baseY = config.getInt("myApp.view.baseY")
  }
  
  val initFoodCount = config.getInt("myApp.initFoodCount") 
  val gridSquareSize = config.getInt("myApp.gridSquareSize") 
  val playerRecombineTime = config.getInt("myApp.playerRecombineTime")
  val playerMinMassToSplit = config.getInt("myApp.playerMinMassToSplit")
  val playerMaxCells = config.getInt("myApp.playerMaxCells")
  val splitCellSpeedDecay = config.getDouble("myApp.splitCellSpeedDecay")
  val ejectedMassSpeedDecay = config.getDouble("myApp.ejectedMassSpeedDecay")
  val automaticMovementTicksForSplit = config.getInt("myApp.automaticMovementTicksForSplit")
  val ejectedMass = config.getInt("myApp.ejectedMass")
  val ejectedMassSpeed = config.getInt("myApp.ejectedMassSpeed")
  val minMassEject = config.getInt("myApp.minMassEject")
  val automaticMovementTicksForEject = config.getInt("myApp.automaticMovementTicksForEject")
  val virusStartMass = config.getInt("myApp.virusStartMass")
  val virusFeedAmount = config.getInt("myApp.virusFeedAmount")
  val virusMinAmount = config.getInt("myApp.virusMinAmount")
	val virusMaxAmount = config.getInt("myApp.virusMaxAmount")
	val ejectedVirusSpeed = config.getInt("myApp.ejectedVirusSpeed")
	val ejectedVirusSpeedDecay = config.getDouble("myApp.ejectedVirusSpeedDecay")
	val automaticMovementTicksForVirusEject = config.getInt("myApp.automaticMovementTicksForVirusEject")
	
  var defaultRooms: List[RoomDescription] = {
    val projs = config.getConfigList("myApp.defaultRooms").asScala map { p => 
      RoomDescription(p.getString("title"), p.getDouble("lat"), p.getDouble("lng"), null, p.getString("serverAddress")) }
    projs.toList }
}
 
object Settings extends ExtensionId[SettingsImpl] with ExtensionIdProvider {
 
  override def lookup = Settings
 
  override def createExtension(system: ExtendedActorSystem) =
    new SettingsImpl(system.settings.config)
}