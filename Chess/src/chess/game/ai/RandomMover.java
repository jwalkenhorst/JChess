package chess.game.ai;

import java.util.List;
import java.util.Random;

import chess.game.Board.Move;
import chess.game.Game;
import chess.game.Mover;

public class RandomMover extends Mover{
	
	public RandomMover(Game game){
		super(game);
	}
	
	public Random rand = new Random();
	
	@Override
	public Move getMove(){
		List<Move> moves = this.game.getAllCurrentMoves();
		return moves.get(this.rand.nextInt(moves.size()));
	}
	
	@Override
	public String toString(){
		return "Random";
	}
	
}