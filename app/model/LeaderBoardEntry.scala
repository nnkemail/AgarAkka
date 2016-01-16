package model

case class LeaderBoardEntry(id: Int, name: String, score: Int) extends Ordered[LeaderBoardEntry] {
	@Override
	override def equals(other: Any): Boolean =
	  other match { 
	  case that: LeaderBoardEntry =>
	    (that canEqual this) &&
	    id == that.id

	  case _ => false
	}

	def canEqual(other: Any): Boolean =
			other.isInstanceOf[LeaderBoardEntry]

	@Override
	override def hashCode: Int =
	41 * (
		41 + id
  ) + name.hashCode()

	def compare(that: LeaderBoardEntry) = that.score - this.score 
}