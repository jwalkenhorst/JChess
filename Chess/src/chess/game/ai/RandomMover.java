package chess.game.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import chess.game.Board.Move;
import chess.game.Game;
import chess.game.Mover;
import chess.game.Piece;

public class RandomMover extends Mover{
	boolean preferCapture;
	
	public RandomMover(Game game, boolean preferCapture){
		super(game);
		this.preferCapture = preferCapture;
	}
	
	public RandomMover(Game game){
		this(game, false);
	}
	
	public Random rand = new Random();
	
	@Override
	public Move call(){
		List<Move> moves = this.game.getAllCurrentMoves();
		if (preferCapture){
			List<Move> captures = new ArrayList<>();
			for (Move move : moves){
				Piece capturing = game.getPiece(move.getNewLocation());
				if (capturing != null) captures.add(move);
			}
			if (captures.size() > 0) moves = captures;
		}
		return moves.get(rand.nextInt(moves.size()));
	}
	
	@Override
	public String toString(){
		String s = "Random";
		if (preferCapture) s += " (Capturing)";
		return s;
	}
}