package chess.ai;

import chess.game.Game;

public abstract class Mover extends Thread{
	protected Game game;
	
	public Mover(Game game){
		this.game = game;
	}
	
	public abstract void move();
}