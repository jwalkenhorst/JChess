package chess.game.ai;

import java.util.Comparator;
import java.util.List;

import chess.game.Board.Move;
import chess.game.Game;
import chess.game.Mover;

public class NextMove extends Mover{
	public NextMove(Game game){
		super(game);
	}

	@Override
	public Move getMove(){
		List<Move> moves = this.game.getAllCurrentMoves();
		return null;
	}
	
	class MoveRanker implements Comparator<Move>{

		@Override
		public int compare(Move o1, Move o2){
			
			return 0;
		}
		
	}
}
