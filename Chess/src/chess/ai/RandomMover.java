package chess.ai;

import java.util.List;
import java.util.Random;

import chess.game.Board.Move;
import chess.game.Game;

public class RandomMover{
	Game game;
	Random rand = new Random();
	
	public RandomMover(Game game){
		this.game = game;
	}
	
	public void move(){
		List<Move> moves = this.game.getAllCurrentMoves();
		this.game.executeMove(moves.get(rand.nextInt(moves.size())));
	}
	
	public boolean approveUndo(){
		return true;
	}
}
