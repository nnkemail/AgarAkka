package model.Entities

import java.awt.Color
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor._
import model.ServerMessage
import model.Util.util
import model.Util.util.settings
import model._

object EntityType extends Enumeration {
      type EntityType = Value
      val CELL, FOOD, VIRUS, MASS = Value
}

import EntityType._
import model.Util.Position
import java.util.Objects

abstract class Entity(eType: EntityType, ePosition: Position, worldGrid: WorldGrid) {
    private   val _id: Int = util.nextSysId()
    protected var _typeofEntity: EntityType = eType;
    protected var _color = util.getRandomColor();
    //protected var _consumer: ActorRef 
    protected var _mass: Int = 1000;
    protected var _spiked: Boolean = false;
    protected var _position: Position = ePosition;
    protected var _gridsTakenByEntity = List.empty[(Int, Int)]
    
    def id = _id;
    def entityType = _typeofEntity;
    def position = _position;
    def position_= (newPosition: Position) = { _position = newPosition }
    def color = _color;
    def color_=(newColor: Color) = { _color = newColor };
    def mass = _mass;
    def mass_= (newMass: Int) = { _mass = newMass }
    def addMass(addMass: Int) = { _mass = _mass + addMass}
    def isSpiked: Boolean = _spiked;
    def isSpiked_= (isSpikedArg: Boolean) = { _spiked = isSpikedArg }
    //def consumer = _consumer;
    
    addToWorldGrid()
    /*
    def collisionCheck(bottomY: Double, topY: Double, rightX: Double , leftX: Double): Boolean = {
    	if (this.position.x > rightX || this.position.x < leftX || this.position.y > bottomY || this.position.y < topY) {
    		return false;
    	}

    	true;
    }*/
    
    def collisionCheck(other: Entity): Boolean = {
      // IF (O1O2 + r <= R) THEN collided. (O1O2: distance b/w 2 centers of cells)
      // (O1O2 + r)^2 <= R^2
      // approximately, remove 2*O1O2*r because it requires sqrt(): O1O2^2 + r^2 <= R^2

      var dx = this.position.x - other.position.x;
      var dy = this.position.y - other.position.y;

      ((dx * dx + dy * dy)  <= other.getPhysicalSquareSize + this.getPhysicalSquareSize());
    };
    
    def shouldUpdate(): Boolean

    def getPhysicalSize(): Double = {
    		math.sqrt(100 * this.mass)
    } 
    
    def getPhysicalSquareSize(): Double = {
    		100 * this.mass
    } 
    
    def addToWorldGrid() = {
      if (position.x < settings.border.left) {
			    	position.x = settings.border.left
			}
			if (position.x > settings.border.right) {
			  position.x = settings.border.right
			}
			if (position.y < settings.border.top) {
			  position.y = settings.border.top
			}
			if (position.y > settings.border.bottom) {
			  position.y = settings.border.bottom
			}
      var newGrids: List[(Int, Int)] = worldGrid.getHash(this)
      worldGrid.insert(newGrids, this)
      _gridsTakenByEntity = newGrids
    }
    
    def onRemove() = {
      worldGrid.remove(_gridsTakenByEntity, this)
    }
    /*
    @Override
    override def hashCode():Int = {
        var hash: Int = 5;
        hash = 47 * hash + this.id;
        hash = 47 * hash + Objects.hashCode(entityType);
        return hash;
    }
    
    @Override
    override def equals(obj: Any): Boolean = {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        var other = obj.asInstanceOf[Entity];
        if (this.id != other.id) {
            return false;
        }
        return true;
    }*/
    
     @Override
     override def equals(other: Any): Boolean =
        other match {
  
          case that: Entity =>
            (that canEqual this) &&
            id == that.id &&
            _typeofEntity == that._typeofEntity
  
          case _ => false
        }
  
      def canEqual(other: Any): Boolean =
        other.isInstanceOf[Entity]
        
      @Override
      override def hashCode: Int =
        41 * (
          41 + id
        ) + _typeofEntity.hashCode()
}

