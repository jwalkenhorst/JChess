package chess.game.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import chess.game.Board.Move;
import chess.game.Game;
import chess.game.Mover;
import chess.game.Piece;

public class RandomCapture extends Mover{
	protected Random random = new Random();
	
	public RandomCapture(Game game){
		super(game);
	}
	
	@Override
	public Move getMove(){
		List<Move> moves = this.game.getAllCurrentMoves();
		List<Move> captures = getCaptures(moves);
		if (captures.size() > 0) moves = captures;
		int size = moves.size();
		return size > 0 ? moves.get(random.nextInt(size)) : null;
	}
	
	protected List<Move> getCaptures(List<Move> moves){
		List<Move> captures = new ArrayList<>();
		for (Move move : moves){
			Piece capturing = this.game.getPiece(move.getNewLocation());
			if (capturing != null) captures.add(move);
		}
		return captures;
	}
	
	@Override
	public String toString(){
		return "Random Capturing";
	}
}
