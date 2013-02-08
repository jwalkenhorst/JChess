package chess.game;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;

import chess.game.Board.Move;

public class Game implements Serializable{
	/**
	 * Represents an action in this Game
	 * 
	 * @author jwalkenhorst
	 */
	public interface GameCommand extends Serializable{
		/**
		 * Performs this action
		 */
		void execute();
		
		/**
		 * Unperforms this action
		 */
		void undo();
	}
	/**
	 * Represents moving a piece from one location to another.
	 * 
	 * @author jwalkenhorst
	 */
	public class MoveCommand implements GameCommand{
		private boolean executed;
		private Move move;
		
		protected MoveCommand(Move move){
			this.move = move;
			this.executed = false;
		}
		
		@Override
		public void execute(){
			if (this.executed) throw new IllegalStateException("Cannot execute move again.");
			if (isPromoting()) throw new IllegalStateException("No moves allowed during a promotion");
			this.executed = true;
			this.move.execute();
			if (this.move.isPromotion()){
				Game.this.promoting = this.move.getMoving();
			} else{
				nextTurn();
			}
		}
		
		/**
		 * @return the Move performed by this command or null if this command has not been executed.
		 */
		public Move getMove(){
			return this.move;
		}
		
		//TODO: toString
		@Override
		public void undo(){
			if (!this.executed) throw new IllegalStateException("Move not executed yet.");
			Player moved = this.move.getMoving().getPlayer();
			this.executed = false;
			this.move.undo();
			Game.this.promoting = null;
			Game.this.setTurn(moved);
		}
	}
	/**
	 * Represents promoting a Pawn
	 * 
	 * @author jwalkenhorst
	 */
	public class PawnPromotionCommand implements GameCommand{
		/**
		 * The Command that caused this promotion
		 */
		private GameCommand previous;
		/**
		 * The piece that got promoted, or null if this promotion has not been executed
		 */
		private Piece promoted;
		/**
		 * The type the Pawn was promoted to.
		 */
		private PieceType promotion;
		
		/**
		 * @param previous
		 *            The Command that caused this promotion
		 * @param promotion
		 *            The type the Pawn was promoted to.
		 */
		public PawnPromotionCommand(GameCommand previous, PieceType promotion){
			this.previous = previous;
			this.promotion = promotion;
		}
		
		@Override
		public void execute(){
			if (this.promoted != null) throw new IllegalStateException("Cannot promote again.");
			if (!isPromoting()) throw new IllegalStateException("No piece to promote");
			if (!PieceType.getPromotionTypes().contains(this.promotion)) throw new IllegalArgumentException("Cannot promote to "
					+ this.promotion);
			Game.this.promoting.setType(this.promotion);
			this.promoted = Game.this.promoting;
			Game.this.promoting = null;
			Game.this.nextTurn();
		}
		
		@Override
		public String toString(){
			String moveDesc = this.previous.toString();
			String result = moveDesc + " " + (this.promoted == null ? "?" : this.promoted.toString());
			return result;
		}
		
		@Override
		public void undo(){
			if (this.promoted == null) throw new IllegalStateException("Promotion not executed yet.");
			Game.this.promoting = this.promoted;
			this.promoted = null;
			Game.this.promoting.setType(PieceType.PAWN);
			this.previous.undo();
		}
	}
	
	protected Board board;
	protected Piece promoting;
	private boolean blackCheck;
	private Deque<GameCommand> commandHistory = new ArrayDeque<>();
	private transient PropertyChangeSupport propertyChange = new PropertyChangeSupport(this);
	private Player turn;
	private boolean whiteCheck;
	
	/**
	 * No-arg constructor for a standard chess game
	 */
	public Game(){
		this(true);
	}
	
	/**
	 * Constructor to create a standard game setup or empty game board.
	 * 
	 * @param standard
	 *            if true creates a standard game setup, creates an empty game board otherwise
	 */
	private Game(boolean standard){
		this.board = new Board();
		this.turn = Player.WHITE;
		if (standard) standardGameSetup();
	}
	
	public void addBoardListener(BoardListener listener){
		this.board.addBoardListener(listener);
	}
	
	public void addPropertyChangeListener(PropertyChangeListener listener){
		this.propertyChange.addPropertyChangeListener(listener);
	}
	
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener){
		this.propertyChange.addPropertyChangeListener(propertyName, listener);
	}
	
	public void executeMove(Move move){
		MoveCommand command = new MoveCommand(move);
		command.execute();
		this.commandHistory.addLast(command);
		this.propertyChange.firePropertyChange("history", null, getHistory());
	}
	
	public Piece getBlackKing(){
		return this.board.getBlackKing();
	}
	
	/**
	 * Returns a list of strings describing commands performed on this game.
	 */
	public List<String> getHistory(){
		List<String> commands = new ArrayList<>();
		for (GameCommand c : this.commandHistory){
			commands.add(c.toString());
		}
		return commands;
	}
	
	/**
	 * @see Board.getPiece;
	 */
	public Piece getPiece(Location location){
		return this.board.getPiece(location);
	}
	
	/**
	 * Bound property for the current player.
	 * 
	 * @return The player whose turn it is.
	 */
	public Player getTurn(){
		return this.turn;
	}
	
	/**
	 * @return an array of locations that the piece at location may move to, or an empty array if it may not move or there is no piece there.
	 */
	public List<Move> getMoves(Location start){
		Piece moving = this.board.getPiece(start);
		List<Move> moves;
		if (moving == null || moving.getPlayer()!= this.turn) moves = Collections.emptyList();
		else moves = moving.getMoves(start);
		for (Iterator<Move> iterator = moves.iterator(); iterator.hasNext();){
			Move move = iterator.next();
			if (move.checksPlayer()) iterator.remove();
			
		}
		return moves;
	}
	
	public Piece getWhiteKing(){
		return this.board.getWhiteKing();
	}
	
	public boolean isBlackCheck(){
		return this.blackCheck;
	}
	
	public boolean isCheck(Player player){
		switch (player){
		case BLACK:
			return this.blackCheck;
		case WHITE:
			return this.whiteCheck;
		default:
			throw new IllegalArgumentException();
		}
	}
	
	public boolean isPromoting(){
		return this.promoting != null;
	}
	
	public boolean isWhiteCheck(){
		return this.whiteCheck;
	}
	
	/**
	 * @throws IllegalStateException
	 *             if no piece is waiting for promotion
	 * @throws IllegalArgumentException
	 *             if promotion is not a valid promotion type
	 */
	public void promote(PieceType promotion){
		GameCommand lastCommand = this.commandHistory.pollLast();
		lastCommand = new PawnPromotionCommand(lastCommand, promotion);
		lastCommand.execute();
		this.commandHistory.addLast(lastCommand);
		this.propertyChange.firePropertyChange("history", null, getHistory());
	}
	
	public void removeBoardListener(BoardListener listener){
		this.board.removeBoardListener(listener);
	}
	
	public void removePropertyChangeListener(PropertyChangeListener listener){
		this.propertyChange.removePropertyChangeListener(listener);
	}
	
	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener){
		this.propertyChange.removePropertyChangeListener(propertyName, listener);
	}
	
	/**
	 * Unperforms the last action
	 */
	public void undo(){
		GameCommand lastCommand = this.commandHistory.pollLast();
		if (lastCommand != null){
			lastCommand.undo();
		}
	}
	
	protected void nextTurn(){
		this.setTurn(this.turn.next());
	}
	
	protected void setTurn(Player next){
		Player current = this.turn;
		if (current != Player.GAME_OVER){
			boolean currentPlayerCheck = this.board.isCheck(current);
			//TODO: check enforcement
			//if (currentPlayerCheck) throw new IllegalStateException("Player may not end turn in check.");
			setCheck(current, currentPlayerCheck);
		} else{
			for (Player p : Player.getPlayers()){
				setCheck(p, this.board.isCheck(p));
			}
		}
		boolean nextPlayerCheck = this.board.isCheck(next);
		setCheck(next, nextPlayerCheck);
		this.turn = next;
		boolean nextHasMoves = false;
		for (Location loc : this.board.getPlayerLocations(next)){
			if (getMoves(loc).size() > 0){
				nextHasMoves = true;
				break;
			}
		}
		if (!nextHasMoves){
			this.turn = Player.GAME_OVER;
			if (nextPlayerCheck){
				//TODO: checkmate
				JOptionPane.showMessageDialog(null, "Checkmate");
			} else{
				//TODO: stalemate
				JOptionPane.showMessageDialog(null, "Stalemate");
			}
		}
		this.propertyChange.firePropertyChange("turn", current, this.turn);
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException{
		in.defaultReadObject();
		this.propertyChange = new PropertyChangeSupport(this);
	}
	
	private void setCheck(Player player, boolean check){
		boolean old;
		String property;
		switch (player){
		case BLACK:
			old = this.blackCheck;
			property = "blackCheck";
			this.blackCheck = check;
			break;
		case WHITE:
			old = this.whiteCheck;
			property = "whiteCheck";
			this.whiteCheck = check;
			break;
		default:
			throw new IllegalArgumentException();
		}
		this.propertyChange.firePropertyChange(property, old, check);
	}
	
	private void standardGameSetup(){
		this.board.placePiece(new Location(7, 4), PieceType.KING, Player.WHITE);
		this.board.placePiece(new Location(0, 4), PieceType.KING, Player.BLACK);
		this.board.placePiece(new Location(7, 3), PieceType.QUEEN, Player.WHITE);
		this.board.placePiece(new Location(0, 3), PieceType.QUEEN, Player.BLACK);
		this.board.placePiece(new Location(7, 2), PieceType.BISHOP, Player.WHITE);
		this.board.placePiece(new Location(0, 2), PieceType.BISHOP, Player.BLACK);
		this.board.placePiece(new Location(7, 5), PieceType.BISHOP, Player.WHITE);
		this.board.placePiece(new Location(0, 5), PieceType.BISHOP, Player.BLACK);
		this.board.placePiece(new Location(7, 1), PieceType.KNIGHT, Player.WHITE);
		this.board.placePiece(new Location(0, 1), PieceType.KNIGHT, Player.BLACK);
		this.board.placePiece(new Location(7, 6), PieceType.KNIGHT, Player.WHITE);
		this.board.placePiece(new Location(0, 6), PieceType.KNIGHT, Player.BLACK);
		this.board.placePiece(new Location(7, 0), PieceType.ROOK, Player.WHITE);
		this.board.placePiece(new Location(0, 7), PieceType.ROOK, Player.BLACK);
		this.board.placePiece(new Location(7, 7), PieceType.ROOK, Player.WHITE);
		this.board.placePiece(new Location(0, 0), PieceType.ROOK, Player.BLACK);
		for (int j = 0; j < Board.SIZE; j++){
			this.board.placePiece(new Location(6, j), PieceType.PAWN, Player.WHITE);
			this.board.placePiece(new Location(1, j), PieceType.PAWN, Player.BLACK);
		}
	}
}
