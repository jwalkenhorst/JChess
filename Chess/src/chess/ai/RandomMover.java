package chess.ai;

import java.util.List;
import java.util.Random;

import chess.game.Board.Move;
import chess.game.Game;

public class RandomMover extends Mover{
	private Random rand = new Random();
	
	public RandomMover(Game game){
		super(game);
	}
	
	@Override
	public void move(){
		List<Move> moves = this.game.getAllCurrentMoves();
		this.game.executeMove(moves.get(rand.nextInt(moves.size())));
	}
}
