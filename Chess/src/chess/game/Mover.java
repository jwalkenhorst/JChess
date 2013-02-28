package chess.game;

import java.util.concurrent.Callable;

import chess.game.Board.Move;

public abstract class Mover implements Callable<Move>{
	protected Game game;
	
	public Mover(Game game){
		this.game = game;
	}
	
	public Game getGame(){
		return this.game;
	}
}