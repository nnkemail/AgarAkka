package model

import play.libs.Akka
import akka.actor._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashSet
import model.Util.util
import model.Util.Position
import model.Entities._
import model.Util.Settings
import model.Util.util.settings

class RoomActor(id: Int) extends Actor {
  val worldGrid = WorldGrid()
  val worldActor = context.actorOf(WorldActor.props(this.context.self, worldGrid))
  val leaderBoardActor = context.actorOf(LeaderBoardActor.props(this.context.self))
  var players = HashSet.empty[ActorRef]
  var workingPlayers: Int = 0;
  var playersData = ListBuffer.empty[Entity]
  var recievedMsg = 0;

  context.system.scheduler.schedule(Duration.Zero, Duration(60, MILLISECONDS))(sendMoveTicks)
  context.system.scheduler.schedule(Duration.Zero, Duration(800, MILLISECONDS))(sendUpdateLeaderBoardTick)
  println ("Po schedulerze")
  
  def sendMoveTicks() = {
    players.foreach(_ ! GameTick)
    worldActor ! GameTick
  }
  
  def sendUpdateLeaderBoardTick() = {
    leaderBoardActor ! LeaderBoardUpdateTick(players.toList)
  }
  
  def receive = {
    case Join(userIDOption: Option[String]) =>
      workingPlayers = workingPlayers + 1;
      players += sender;
      sender ! SpawnData(util.nextSysId(), util.getRandomPosition(), self, worldGrid, worldActor)
      userIDOption map {userID => context.parent ! UserJoinedGame(userID, id)}
      println("Przyszlo Join");

    case Leave(userIDOption: Option[String]) =>
      players = players - sender;
      workingPlayers = workingPlayers - 1;
      userIDOption map {userID => context.parent ! UserLeftGame(userID)}
      println(players);
      
    case RemoveFromLeaderBoard(playerID: Int) =>
      leaderBoardActor forward RemoveFromLeaderBoard(playerID)
     
    case RestartMyGame =>
      println("room actor restart");
      sender ! RestartGame(util.getRandomPosition())
      
    case SaveMyScore(score, uID) =>
      context.parent ! SaveMyScore(score, uID)
      
      
  }
}

object RoomActor {
  //val serv = Akka.system.actorOf(Props[Server])    
  //def getActorRef() = { serv }
  def props(id: Int) = Props(new RoomActor(id))
}

