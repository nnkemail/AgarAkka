package model

import akka.actor._
import model.Entities._
import scala.collection.mutable.HashSet
import model.Util.util.settings
import model.Util.util
import model.Util.Position
import java.awt.Color

object WorldActor {
  def props(server: ActorRef, worldGrid: WorldGrid) = Props(new WorldActor(server, worldGrid))
}

class WorldActor(server: ActorRef, worldGrid: WorldGrid) extends Actor {
  var movingEntities = HashSet.empty[MovableEntity]
  var stopedEntities = HashSet.empty[MovableEntity]
  var numberOfViruses = 0;
  
  spawnInitialFood()
  spawnInitialViruses()
  
  def receive = {
    case GameTick => {
      var entitiesToStop = HashSet.empty[MovableEntity]
      
      for (e <- movingEntities) {
        if (e._isMoveEngineActive)
          e.move()
        else
          entitiesToStop += e 
          stopedEntities += e
      }
      
      for (toStopEntity <- entitiesToStop)
        movingEntities -= toStopEntity
    }
    /*         
    case EjectedMass(m: Mass) => 
      var ejected = new Mass(startPos, worldGrid, worldActor) 
      ejected.moveAngle = angle
      ejected.speed = settings.ejectedMassSpeed
      ejected.color = this.color
      movingEntities += m
      m.setMoveEngine(settings.automaticMovementTicksForEject)   
    */
    
    case EjectedMass(startPosition: Position, angle: Double, color: Color) => 
      var ejected = new Mass(startPosition, worldGrid, this.context.self) 
      ejected.moveAngle = angle
      ejected.speed = settings.ejectedMassSpeed
      ejected.color = color
      movingEntities += ejected
      ejected.setMoveEngine(settings.automaticMovementTicksForEject)  
      
    case EatEntity(m: MovableEntity, eatingEntity: Entity) =>
      if (movingEntities.contains(m) || stopedEntities.contains(m)) {
        movingEntities -= m
        stopedEntities -= m
        m.onRemove()
        sender ! AddMass(m.mass, eatingEntity)
      }
      
    case AddMass(m: Int, eatingEntity: Entity) => {
      eatingEntity.addMass(m)
    }
    
    case FeedVirus(virus: Virus, mass: Mass) => {
      virus.feed(mass)
    }
      
    case ShootVirus(sourceVirus: Virus) => 
      var newVirus = new Virus(Position(sourceVirus.position.x, sourceVirus.position.y), worldGrid, this.context.self)
      newVirus.moveAngle = sourceVirus.moveAngle
      newVirus.speed = settings.ejectedVirusSpeed
      movingEntities += newVirus
      newVirus.setMoveEngine(settings.automaticMovementTicksForVirusEject)   
  }
  
  def spawnInitialFood() = {
    var initFoodCount = settings.initFoodCount;
    var i = 0;
    
    for (i <- 1 to initFoodCount)
       Food(util.getRandomPosition(), worldGrid);
    println("Spawn zakonczone")
  }
  
  def spawnInitialViruses() = {
    var initVirusCount = settings.virusMinAmount;
    var i = 0;
    
    for (i <- 1 to initVirusCount) {
       var newVirus = new Virus(util.getRandomPosition(), worldGrid, this.context.self);
       stopedEntities += newVirus
    }
    println("Spawn wirusow zakonczone")
  }
}