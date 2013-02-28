package chess.ui;

import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import chess.game.Board.Move;
import chess.game.Mover;

public class SwingMover extends SwingWorker<Move, Void>{
	Mover mover;
	
	public SwingMover(Mover mover){
		this.mover = mover;
	}

	@Override
	protected Move doInBackground() throws Exception{
		return mover.call();
	}
	
	@Override
	protected void done(){
		try{
			mover.getGame().executeMove(this.get());
		} catch (InterruptedException | ExecutionException e){
			e.printStackTrace();
		}
	}
}
