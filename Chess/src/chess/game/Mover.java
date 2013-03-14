package chess.game;

import java.util.concurrent.Callable;

import chess.game.Board.Move;

public abstract class Mover implements Callable<Move>{
	protected Game game;
	
	public Mover(Game game){
		this.game = game;
	}
	
	@SuppressWarnings("static-method")
	public boolean allowUndo(){
		return true;
	}
	
	@Override
	public final Move call(){
		Move move = this.getMove();
		boolean promotion = move.promotesPiece();
		if (!promotion && game.canDeclareStalemate() && checkStalemate()) game.declareStalemate();
		this.game.executeMove(move);
		if (promotion) game.promote(getPromotion());
		return move;
	}
	
	@SuppressWarnings("static-method")
	public boolean checkStalemate(){
		return false;
	}
	
	public Game getGame(){
		return this.game;
	}
	
	public abstract Move getMove();
	
	@SuppressWarnings("static-method")
	public PieceType getPromotion(){
		return PieceType.QUEEN;
	}
}