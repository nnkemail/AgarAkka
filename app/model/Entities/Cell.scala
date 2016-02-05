package model.Entities

import model.Util.Position
import model.Entities.EntityType._
import model.PlayerActor
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable.ListBuffer
import akka.actor._
import util.control.Breaks._
import model._
import model.Util.util.settings
import model.Util.util
import scala.collection.mutable.HashSet

case class Cell(owner: PlayerActor, ePosition: Position, worldGrid: WorldGrid, worldActor: ActorRef) 
extends MovableEntity(EntityType.CELL, ePosition, worldGrid, worldActor) {
  var _recombineTicks: Long = 0
  
  @Override
  def shouldUpdate = true
  def recombineTicks: Long = _recombineTicks
  def recombineTicks_= (p_recombineTicks: Long) = { _recombineTicks = p_recombineTicks }  

  def getSpeed(): Double = {
	  //return 30.0D * Math.pow(mass, -1.0D / 4.5D) * 50.0D / 40.0D;
	  return 60.0D * Math.pow(mass, -1.0D / 4.5D) * 50.0D / 40.0D;
    //return 30*Math.pow(mass, -1.0D / 4.5D) 
  }
  
  def move(): Unit = {
		  if (_isMoveEngineActive)
			  bounce(settings.splitCellSpeedDecay)
			else {
				val r = getPhysicalSize();
				var mouse = owner.mousePosition

				// Get angle
				var deltaX = mouse.x - position.x;
				var deltaY = mouse.y - position.y;
				var _moveAngle = Math.atan2(deltaX, deltaY);

				if (java.lang.Double.isNaN(_moveAngle)) {
					println("NaN");
					return
				}

				// Distance between mouse pointer and cell
			  var distance = position.distance(mouse.x, mouse.y);
				speed = Math.min(getSpeed(), distance);

				var x1 = position.x + (speed * math.sin(_moveAngle));
				var y1 = position.y + (speed * math.cos(_moveAngle));

				// TODO: Fire a move event here
				// Make sure we're not passing the world border
				if (x1 < settings.border.left) {
					x1 = settings.border.left
				}
				if (x1 > settings.border.right) {
					x1 = settings.border.right
				}
				if (y1 < settings.border.top) {
					y1 = settings.border.top
				}
				if (y1 > settings.border.bottom) {
					y1 = settings.border.bottom
				}
				/*
				for (other <- owner.cells) {
					breakable {
						if (other.id == this.id) {
							break
						}

						if ((this.recombineTicks > 0) || (other.recombineTicks > 0)) {

							var collisionDist = other.getPhysicalSize() + r; // Minimum distance between the 2 cells
							if (!simpleCollide(other, collisionDist)) {
								break;  
							}

							// Precise collision checking
							distance = position.distance(other.position);

							if (distance < collisionDist) {
								// Moving cell pushes collided cell
								var newDeltaX = other.position.x - x1;
								var newDeltaY = other.position.y - y1;
								var newAngle = math.atan2(newDeltaX, newDeltaY);

								var move = collisionDist - distance;
								//var move = collisionDist - distance;

								other.position = (other.position.add(move * math.sin(newAngle), move * math.cos(newAngle)));
								//this.position = this.position.add(-move * math.sin(newAngle), -move * math.cos(newAngle))
							}
						}
					}
				} */

				position.x = x1
			  position.y = y1
			}

		updatePositionInGrid()
  }
  
  def checkCollisionWithOwnCells() = {
      val r = getPhysicalSize();
    	for (other <- owner.cells) {
	    breakable {
		    if (other.id == this.id) {
			    break
		    }
		    if ((this.recombineTicks > 0) || (other.recombineTicks > 0)) {
		    var collisionDist = other.getPhysicalSize() + r; // Minimum distance between the 2 cells
		    if (!simpleCollide(other, collisionDist)) {
			     break;  
		    }

		    // Precise collision checking
		    var distance = position.distance(other.position);

		    if (distance < collisionDist) {
			    // Moving cell pushes collided cell
			    var newDeltaX = other.position.x - position.x;
			    var newDeltaY = other.position.y - position.y;
			    var newAngle = math.atan2(newDeltaX, newDeltaY);

			    var move = collisionDist - distance;
			    //var move = collisionDist - distance;

			    other.position = (other.position.add(move * math.sin(newAngle), move * math.cos(newAngle)));
			    //this.position = this.position.add(-move * math.sin(newAngle), -move * math.cos(newAngle))
			    
			    if (other.position.x < settings.border.left) {
			    	other.position.x = settings.border.left
			    }
			    if (other.position.x > settings.border.right) {
			    	other.position.x = settings.border.right
			    }
			    if (other.position.y < settings.border.top) {
			    	other.position.y = settings.border.top
			    }
			    if (other.position.y > settings.border.bottom) {
			    	other.position.y = settings.border.bottom
			    }
			    other.updatePositionInGrid()
		    }
		  }
	    }
	  }  
  }
  
  def eat(): Unit = {
    
      if (owner.eatenOwnCells.contains(this))
        return
      if (recombineTicks > 0) {
		    recombineTicks = recombineTicks - 1;   
      }
      
		  var edibles = HashSet.empty[Entity]
		  var r = getPhysicalSize();

		  var topY = position.y - r;
		  var bottomY = position.y + r;
		  var leftX = position.x - r;
		  var rightX = position.x + r;
		  
		  var entNearBy = worldGrid.getListOfEntities(_gridsTakenByEntity)
		  
		  
		  for (other <- entNearBy) {
			  breakable {
			    if (this.id == other.id) break
				  //if (!other.collisionCheck(bottomY, topY, rightX, leftX)) {
			    if (!collisionCheck(other)) {
					  break;
				  }

				  var multiplier: Double = 1.25D;
				  other match {
				    case f: Food => 
				      //owner.worldGrid.remove(f)
				      edibles += f
				      break
				      
				    case m: Mass => 
				      edibles += m
				      break
				      
				    case v: Virus =>
				      multiplier = 1.33;
				     

				    case c: Cell => {
				      if (c.id == this.id) break
				      if (owner.eatenOwnCells.contains(c)) break;
				      if (this.owner.playerID == c.owner.playerID) {
				        if ((this.recombineTicks > 0) || (c.recombineTicks > 0)) {
                        break;
                    }
                    multiplier = 1.0D;
              }  
				    } 
				  }
				  
				  if (other.mass * multiplier <= this.mass) {
			      var dist: Double = position.distance(other.position);
					  var eatingRange: Double = getPhysicalSize() - (other.getPhysicalSize() * 0.4D);
					  if (dist < eatingRange) 
					    edibles += other
			    }
			  }
		  }
		 
		  for (e <- edibles) {
		    println(this.id + " Edibles: "+ e.entityType + "" + e.id)
		    e match {
		      case f: Food => 
		        this.addMass(f.mass);
		        this.owner.score += f.mass
		        this.owner.sendNewScore(this.owner.score)
		        println("Jedzenie " + f.mass)
		        f.onRemove()
		      
		      case v: Virus => virusCollision(v)
		      
		      case c: Cell =>
		        //println(this.id + " id celi do zjedzenia: ", c.id);
		        //eatenCells += c.id
		        //owner.worldGrid.remove(c)
		        if (this.owner.playerID == c.owner.playerID) {// && !owner.eatenOwnCells.contains(c)) {
		          this.addMass(c.mass)
		          this.owner.eatenOwnCells += c
		          owner.log.info(this.id + " id celi do samozjedzenia: " + c.id);
		        } else {
		          c.owner.context.self.!(EatCell(c, this))(owner.context.self)
		          owner.log.info(this.id + " id celi do wyslania: " + c.id);
		        }
		        
		      case m: Mass => owner.worldActor.!(EatEntity(m, this))(owner.context.self)
		    }
		  }
    }
  
    def split(): Cell = {
	    // Get angle
			var deltaY = owner.mousePosition.y - position.y;
			var deltaX = owner.mousePosition.x - position.x;
			var angle = Math.atan2(deltaX,deltaY);

			// Get starting position
			var size = this.getPhysicalSize();
			var startPos = Position(position.x + (size * Math.sin(angle)), position.y + (size * Math.cos(angle))); 

			// Calculate mass and speed of splitting cell
			var splitSpeed = this.getSpeed() * 6;
			var newMass = this.mass/2;
			println("oldMass: ", this.mass)
		  this.mass = newMass;
			this.updatePositionInGrid()
			println("newMass: ",this.mass)
			// Create cell
			var newCell = Cell (owner, startPos, worldGrid, worldActor);
		  newCell.color = this.color
		  newCell.mass = newMass
		  newCell.recombineTicks = settings.playerRecombineTime
		  this.recombineTicks = settings.playerRecombineTime
		  newCell.updatePositionInGrid()
		  newCell.moveAngle = angle
		  newCell.speed = splitSpeed
		  newCell.setMoveEngine(settings.automaticMovementTicksForSplit)
		  newCell

			//split.calcMergeTime(this.config.playerRecombineTime);
			// Add to moving cells list
    }
    
    def ejectMass() = {
      var deltaY = owner.mousePosition.y - position.y
			var deltaX = owner.mousePosition.x - position.x
			var angle = Math.atan2(deltaX,deltaY)
			
			var size = this.getPhysicalSize() + 10;
			var startPos = Position(position.x + ((settings.ejectedMass + size) * Math.sin(angle)), 
			    position.y + ((settings.ejectedMass + size) * Math.cos(angle))); 
			
			this.mass = this.mass - settings.ejectedMass
			
			//var ejected = new Mass(startPos, worldGrid, worldActor)  //TODO PRZENIESC DO WORLDACTOR!
      //ejected.moveAngle = angle
      //ejected.speed = settings.ejectedMassSpeed
      //ejected.color = this.color
      //ejected
			worldActor.!(EjectedMass(Position(startPos.x, startPos.y), angle, color))(owner.context.self)
    }
    
    def virusCollision(virus: Virus): Unit = {
      var maxSplits: Int = (Math.floor((this.mass)/16.0) - 1).toInt; // Maximum amount of splits
      var numSplits: Int = settings.playerMaxCells - owner.cells.size; // Get number of splits
      numSplits = Math.min(numSplits,maxSplits);
      if (numSplits < 0) numSplits = 0
      var splitMass = Math.min(this.mass/(numSplits + 1), 36); // Maximum size of new splits

      // Cell consumes mass before splitting
      worldActor.!(EatEntity(virus, this))(this.owner.context.self)

      // Cell cannot split any further
      if (numSplits <= 0) {
        return;
      }

      // Big cells will split into cells larger than 36 mass (1/4 of their mass)
      var bigSplits = 0;
      var endMass = this.mass - (numSplits * splitMass);
      if ((endMass > 300) && (numSplits > 0)) {
        bigSplits = bigSplits + 1
        numSplits = numSplits - 1
      }
      if ((endMass > 1200) && (numSplits > 0)) {
        bigSplits = bigSplits + 1
        numSplits = numSplits - 1
      }
      if ((endMass > 3000) && (numSplits > 0)) {
        bigSplits = bigSplits + 1
        numSplits = numSplits - 1
      }

      // Splitting
      var angle = 0D; // Starting angle
      for (k <- 0 until numSplits) {
        angle = angle + 6.0D/numSplits.toDouble; // Get directions of splitting cells
        //gameServer.newCellVirused(client, consumer, angle, splitMass,150);
        owner.context.self ! SplitCellByVirusCollision(this, angle, splitMass, 150)
        this.mass = this.mass - splitMass;
      }

      for (k <- 0 until bigSplits) 
      {
        angle = util.random.nextDouble() * 6.28; // Random directions
        splitMass = this.mass / 4;
        //gameServer.newCellVirused(client, consumer, angle, splitMass,20);
        owner.context.self ! SplitCellByVirusCollision(this, angle, splitMass, 20)
        this.mass -= splitMass;
      }
	
    // Prevent consumer cell from merging with other cells
      this._recombineTicks = settings.playerRecombineTime
    }
    
    private def simpleCollide(other: Cell, collisionDist: Double): Boolean = {
        math.abs(this.position.x - other.position.x) < (2 * collisionDist) && 
        math.abs(this.position.y - other.position.y) < (2 * collisionDist);
    }
    
     @Override
     override def equals(other: Any): Boolean =
        other match {
  
          case that: Cell =>
            super.equals(that) &&
            (that canEqual this) &&
            id == that.id &&
            _typeofEntity == that._typeofEntity
  
          case _ => false
        }
  
      @Override
      override def canEqual(other: Any): Boolean =
        other.isInstanceOf[Cell]
}