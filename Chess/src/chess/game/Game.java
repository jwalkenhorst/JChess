package chess.game;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;

import chess.game.Board.Move;

public class Game implements Serializable{
	/**
	 * Represents an action in this Game
	 * 
	 * @author jwalkenhorst
	 */
	protected interface GameCommand extends Serializable{
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
	protected class MoveCommand implements GameCommand{
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
			if (this.move.promotesPiece()){
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
		
		@Override
		public String toString(){
			return this.move.toString() + (this.executed ? '+' : '-');
		}
		
		@Override
		public void undo(){
			if (!this.executed) throw new IllegalStateException("Move not executed yet.");
			Game.this.promoting = null;
			this.executed = false;
			Player movingPlayer = this.move.getMoving().getPlayer();
			this.move.undo();
			Game.this.setTurn(movingPlayer);
		}
	}
	/**
	 * Represents promoting a Pawn
	 * 
	 * @author jwalkenhorst
	 */
	protected class PawnPromotionCommand implements GameCommand{
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
			String result = moveDesc
					+ " "
					+ (this.promoted == null ? "?" : this.promoted.toString());
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
	private Future<Move> lastMovement;
	private ExecutorService moveExecutor = Executors.newSingleThreadExecutor();
	private Map<Player, Mover> movers = new EnumMap<>(Player.class);
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
	
	public boolean canDeclareStalemate(){
		//System.out.println(this.board.getNonCaptureMoves());
		return this.board.getNonCaptureMoves() >= 49;
	}
	
	protected Player playerStalemate = null;
	public void declareStalemate(){
		if (!canDeclareStalemate()) throw new IllegalStateException("Stalemate not available now");
		this.playerStalemate = this.turn;
	}
	
	public void executeMove(Move move){
		//TODO: strict turn enforcement
		GameCommand command = new MoveCommand(move);
		this.commandHistory.addLast(command);
		command.execute();
		this.propertyChange.firePropertyChange("history", null, getHistory());
		executeMover();
	}

	public List<Move> getAllCurrentMoves(){
		if (!Player.getPlayers().contains(this.turn)) return Collections.emptyList();
		List<Move> allMoves = new ArrayList<>();
		for (Location loc : this.board.getPlayerLocations(this.turn)){
			allMoves.addAll(getMoves(loc));
		}
		return allMoves;
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
	
	public Mover getMover(Player p){
		return movers.get(p);
	}
	
	/**
	 * @return a list of locations that the piece at location may move to, or an empty list if it may not move or there
	 *         is no piece there.
	 */
	public List<Move> getMoves(Location start){
		Piece moving = this.board.getPiece(start);
		List<Move> moves;
		if (moving == null || moving.getPlayer() != this.turn) moves = Collections.emptyList();
		else moves = moving.getMoves(start);
		for (Iterator<Move> iterator = moves.iterator(); iterator.hasNext();){
			Move move = iterator.next();
			if (move.checksPlayer()) iterator.remove();
		}
		return moves;
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
	
	public void setMover(final Player player, final Mover mover){
		if (player == Player.GAME_OVER){
			throw new IllegalArgumentException("May not assign mover to " + Player.GAME_OVER);
		}
		this.movers.put(player, mover);
		executeMover();
	}
	
	public boolean turnHasMover(){
		return getMover(this.getTurn()) != null;
	}
	
	/**
	 * Unperforms the last action
	 */
	public void undo(){
		GameCommand lastCommand;
		Mover mover;
		do{
			lastCommand = this.commandHistory.pollLast();
			if (lastCommand != null){
				lastCommand.undo();
				this.propertyChange.firePropertyChange("history", null, getHistory());
			}
			mover = this.movers.get(this.turn);
			
		}while(lastCommand != null && mover != null && mover.allowUndo());
		if (lastCommand == null) executeMover();
	}
	
	protected void nextTurn(){
		if (playerStalemate != null && canDeclareStalemate()){
			this.setTurn(Player.GAME_OVER);
		}else{	
			playerStalemate = null;
			this.setTurn(this.turn.next());
		}
	}
	
	protected void setTurn(Player next){
		Player current = this.turn;
		if (current != Player.GAME_OVER){
			boolean currentPlayerCheck = this.board.isCheck(current);
			//TODO: check enforcement at model level - currently only the UI strictly enforces check movement rules
			//if (currentPlayerCheck) throw new IllegalStateException("Player may not end turn in check.");
			setCheck(current, currentPlayerCheck);
		} else{
			for (Player p : Player.getPlayers()){
				setCheck(p, this.board.isCheck(p));
			}
		}
		this.turn = next;
		boolean nextPlayerCheck = false;
		if (Player.getPlayers().contains(next)){
			nextPlayerCheck = this.board.isCheck(next);
			setCheck(next, nextPlayerCheck);
		}
		List<Move> allMoves = getAllCurrentMoves();
		if (allMoves.isEmpty() && !isPromoting()){
			this.turn = Player.GAME_OVER;
			if (nextPlayerCheck){
				//TODO: checkmate - no model logic for handling checkmate, no end-of-game events aside from the player changing to GAME_OVER 
				JOptionPane.showMessageDialog(null, "Checkmate - "+current);
			} else{
				//TODO: stalemate  - no model logic for handling stalemate, no end-of-game events aside from the player changing to GAME_OVER
				String message ;
				if (playerStalemate== null) message = "Stalemate: no possible move for "+next;
				else message = "Stalemate - "+playerStalemate;
				JOptionPane.showMessageDialog(null, message);
			}
		}
		this.propertyChange.firePropertyChange("turn", current, this.turn);
	}
	
	private void executeMover(){
		Mover mover = this.movers.get(this.turn);
		if (mover != null){
			this.lastMovement = this.moveExecutor.submit(mover);
		}
	}
	
	private void readObject(java.io.ObjectInputStream in)
			throws IOException, ClassNotFoundException{
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
		return;
	}
}
