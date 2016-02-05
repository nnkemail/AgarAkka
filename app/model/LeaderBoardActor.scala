package model

import akka.actor._
import scala.collection.mutable.SortedSet
import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer

object LeaderBoardActor {
  def props(server: ActorRef) = Props(new LeaderBoardActor(server))
}

class LeaderBoardActor(server: ActorRef) extends Actor {
  //var leaderBoard: SortedSet[LeaderBoardEntry] = SortedSet.empty;
  var leaderBoard: ListBuffer[LeaderBoardEntry] = ListBuffer.empty
  var waitedOneRound: Boolean = false 
  
  def addToLeaderBoard(newEntry: LeaderBoardEntry) = {
    leaderBoard = leaderBoard filter (x => x.id != newEntry.id)
    leaderBoard += newEntry 
    leaderBoard = leaderBoard.sortWith(_.totalMass < _.totalMass) 
    leaderBoard = leaderBoard.take(10)  
  }
  
  def receive = {
    case entry: LeaderBoardEntry =>
     addToLeaderBoard(entry)   
     
    case LeaderBoardUpdateTick(players: List[ActorRef]) =>
      if (players.length >= 10 && leaderBoard.size < 10 && waitedOneRound == false) {
         waitedOneRound = true 
      } else {
        for (player <- players)
          player ! NewLeaderBoard(leaderBoard.toList)
        waitedOneRound = false
      }
      leaderBoard.clear()
      
    case RemoveFromLeaderBoard(playerID: Int) =>
      leaderBoard -= LeaderBoardEntry(playerID, "", 0)
  }
}
  
