package chess.game;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Stores the location of {@link Piece} objects
 * 
 * @author jwalkenhorst
 */
public class Board implements Serializable{
	public static final int SIZE = 8;
	
	/**
	 * @return if loc is on this board.
	 */
	public static boolean onBoard(Location loc){
		if (loc == null) return false;
		if (loc.row < 0) return false;
		if (loc.column < 0) return false;
		if (loc.row >= SIZE) return false;
		if (loc.column >= SIZE) return false;
		return true;
	}
	
	/**
	 * Represents the result of moving a piece.
	 * 
	 * @author jwalkenhorst
	 */
	public class Move implements Serializable{
		private Piece captured;
		private Piece moving;
		private Location oldLocation, newLocation;
		private Move previous;
		private boolean promotion;
		
		protected Move(Location oldLocation, Location newLocation){
			if (!oldLocation.isOnBoard()) throw new OffBoardException(oldLocation);
			if (!newLocation.isOnBoard()) throw new OffBoardException(newLocation);
			this.oldLocation = oldLocation;
			this.newLocation = newLocation;
		}
		
		/**
		 * @return The piece captured by the result of this move, or null if no capture occurred.
		 */
		public Piece getCaptured(){
			return this.captured;
		}
		
		/**
		 * @return The piece that was moved, or null if this Move has not been executed.
		 */
		public Piece getMoving(){
			return this.moving;
		}
		
		public Location getNewLocation(){
			return this.newLocation;
		}
		
		public Location getOldLocation(){
			return this.oldLocation;
		}
		
		/**
		 * @return true if this Move has resulted in a promotion, false otherwise or if this Move has not been executed.
		 */
		public boolean isPromotion(){
			return this.promotion;
		}
		
		protected boolean checksPlayer(){
			Board.this.notifyListeners = false;
			execute();
			boolean check = Board.this.isCheck(this.moving.getPlayer());
			undo();
			Board.this.notifyListeners = true;
			return check;
		}
		
		protected void execute(){
			if (this.moving != null) throw new IllegalStateException("Move already executed");
			this.moving = Board.this.pieces.remove(this.oldLocation);
			if (this.moving == null) throw new EmptyLocationException(this.oldLocation);
			this.captured = Board.this.pieces.put(this.newLocation, this.moving);
			if (this.newLocation.row == 0 || this.newLocation.row == Board.SIZE - 1){
				if (this.moving.getType() == PieceType.PAWN) this.promotion = true;
			}
			this.moving.incrementMoves();
			this.previous = Board.this.last;
			Board.this.last = this;
			Board.this.fireBoardChanged(new Location[]{this.oldLocation, this.newLocation});
		}
		
		protected void undo(){
			if (this.moving == null) throw new IllegalStateException("Move not executed");
			this.moving.decrementMoves();
			this.promotion = false;
			if (this.captured == null) Board.this.pieces.remove(this.newLocation);
			else Board.this.pieces.put(this.newLocation, this.captured);
			Board.this.pieces.put(this.oldLocation, this.moving);
			this.moving = null;
			Board.this.last = this.previous;
			this.previous = null;
			Board.this.fireBoardChanged(new Location[]{this.oldLocation, this.newLocation});
		}
	}
	/**
	 * A snapshot of where a Piece is located when this object is created.
	 * 
	 * @author jwalkenhorst
	 */
	protected class PieceLocation implements Serializable{
		private final Location location;
		private final Piece piece;
		
		/**
		 * Creates a new PieceLocation based on the mapping specified by pieceLocation
		 * 
		 * @param pieceLocation
		 *            the <Location, Piece> mapping that defines this PieceLocation
		 */
		protected PieceLocation(Entry<Location, Piece> pieceLocation){
			this.location = pieceLocation.getKey();
			this.piece = pieceLocation.getValue();
		}
		
		@Override
		public boolean equals(Object obj){
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			PieceLocation other = (PieceLocation)obj;
			if (!getOuterType().equals(other.getOuterType())) return false;
			if (this.location == null){
				if (other.location != null) return false;
			} else if (!this.location.equals(other.location)) return false;
			if (this.piece == null){
				if (other.piece != null) return false;
			} else if (!this.piece.equals(other.piece)) return false;
			return true;
		}
		
		public Player getPlayer(){
			return this.piece.getPlayer();
		}
		
		@Override
		public int hashCode(){
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((this.location == null) ? 0 : this.location.hashCode());
			result = prime * result + ((this.piece == null) ? 0 : this.piece.hashCode());
			return result;
		}
		
		protected boolean hasMoveTo(Location end){
			return this.piece.hasMove(this.location, end);
		}
		
		private Board getOuterType(){
			return Board.this;
		}
	}
	private class DoubleMove extends Move{
		private Piece secondMoving, secondCapture;
		private Location secondOld, secondNew;
		
		protected DoubleMove(Location oldLocation, Location newLocation, Location secondOld, Location secondNew){
			super(oldLocation, newLocation);
			this.secondOld = secondOld;
			this.secondNew = secondNew;
		}
		
		@Override
		protected void execute(){
			super.execute();
			this.secondMoving = Board.this.pieces.remove(this.secondOld);
			if (this.secondMoving == null) throw new EmptyLocationException(this.secondOld);
			if (this.secondNew != null){
				this.secondCapture = Board.this.pieces.put(this.secondNew, this.secondMoving);
				this.secondMoving.incrementMoves();
				Board.this.fireBoardChanged(new Location[]{this.secondOld, this.secondNew});
			} else{
				Board.this.fireBoardChanged(new Location[]{this.secondOld});
			}
		}
		
		@Override
		protected void undo(){
			if (this.secondMoving == null) throw new IllegalStateException("Move not executed");
			Location[] changedLocations;
			if (this.secondNew != null){
				this.secondMoving.decrementMoves();
				if (this.secondCapture == null) Board.this.pieces.remove(this.secondNew);
				else Board.this.pieces.put(this.secondNew, this.secondCapture);
				changedLocations = new Location[]{this.secondOld, this.secondNew};
			} else{
				changedLocations = new Location[]{this.secondOld};
			}
			Board.this.pieces.put(this.secondOld, this.secondMoving);
			this.secondMoving = null;
			Board.this.fireBoardChanged(changedLocations);
			super.undo();
		}
	}
	
	protected Move last;
	protected Map<Location, Piece> pieces;
	protected boolean notifyListeners = true;
	private Piece blackKing, whiteKing;
	private transient List<BoardListener> listeners;
	
	public Board(){
		this.pieces = new HashMap<>(Board.SIZE*Board.SIZE, 1.0f);
	}
	
	public void addBoardListener(BoardListener listener){
		if (listener == null) return;
		if (this.listeners == null) this.listeners = new LinkedList<>();
		this.listeners.add(listener);
	}
	
	/**
	 * @return the location of piece on the board, null if piece is not present. Uses object identity to identify piece.
	 */
	public Location findPiece(Piece piece){
		if (piece == null) return null;
		if (piece.getBoard() != this) throw new IllegalArgumentException("Piece does not belong to this board");
		for (Entry<Location, Piece> pieceLocation : this.pieces.entrySet()){
			if (piece == pieceLocation.getValue()) return pieceLocation.getKey();
		}
		return null;
	}
	
	public Piece getBlackKing(){
		return this.blackKing;
	}
	
	public Move getLast(){
		return this.last;
	}
	
	/**
	 * @return the piece at location, or null if location is occupied.
	 */
	public Piece getPiece(Location location){
		return this.pieces.get(location);
	}
	
	public Set<Location> getPlayerLocations(Player player){
		Set<Location> locations = new HashSet<>();
		for (Entry<Location, Piece> entry : this.pieces.entrySet()){
			if (entry.getValue().getPlayer() == player) locations.add(entry.getKey());
		}
		return locations;
	}
	
	public Piece getWhiteKing(){
		return this.whiteKing;
	}
	
	public boolean isCheck(Player player){
		Location kingLoc;
		switch (player){
		case BLACK:
			kingLoc = this.findPiece(this.blackKing);
			break;
		case WHITE:
			kingLoc = this.findPiece(this.whiteKing);
			break;
		default:
			throw new IllegalArgumentException();
		}
		Player opponent = player.next();
		boolean check = playerHasMove(kingLoc, opponent);
		return check;
	}
	
	public boolean playerHasMove(Location loc, Player player){
		for (PieceLocation pl : this.getPieceLocations()){
			if (pl.getPlayer() == player && pl.hasMoveTo(loc)){
				return true;
			}
		}
		return false;
	}
	
	public void removeBoardListener(BoardListener listener){
		if (listener == null) return;
		if (this.listeners != null) this.listeners.remove(listener);
	}
	
	/**
	 * Causes all BoardListeners registered on this board to recieve a BoardChangedEvent for loc
	 */
	public void update(Location loc){
		this.fireBoardChanged(new Location[]{loc});
	}
	
	protected void fireBoardChanged(Location[] locations){
		if (!this.notifyListeners || this.listeners == null || this.listeners.size() == 0) return;
		BoardChangedEvent evt = new BoardChangedEvent(this, locations);
		for (BoardListener l : this.listeners)
			l.boardChanged(evt);
	}
	
	/**
	 * @return an unexecuted Move that moves the piece at oldLocation to newLocation
	 * @throws OffBoardException
	 *             if newLocation is not on the board
	 * @throws EmptyLocationException
	 *             if oldLocation is not occupied
	 */
	protected Move makeMove(Location oldLocation, Location newLocation){
		return new Move(oldLocation, newLocation);
	}
	
	protected Move makeMove(Location oldLocation, Location newLocation, Location secondOld, Location secondNew){
		return new DoubleMove(oldLocation, newLocation, secondOld, secondNew);
	}
	
	/**
	 * Puts piece on the board at location.
	 * 
	 * @throws OffBoardException
	 *             if location is not on this board.
	 * @return The Piece previously at location.
	 */
	protected Piece placePiece(Location location, PieceType type, Player player){
		if (!location.isOnBoard()){
			throw new OffBoardException(location);
		}
		Piece piece = new Piece(type, player, this);
		trackKings(piece);
		Piece removed = this.pieces.put(location, piece);
		fireBoardChanged(new Location[]{location});
		return removed;
	}
	
	/**
	 * @return A set of all PieceLocations mapped by this board.
	 */
	private Set<PieceLocation> getPieceLocations(){
		Set<PieceLocation> locations = new HashSet<>();
		for (Entry<Location, Piece> pieceLocation : this.pieces.entrySet()){
			locations.add(new PieceLocation(pieceLocation));
		}
		return locations;
	}
	
	private void trackKings(Piece piece){
		if (piece.getType() == PieceType.KING){
			switch (piece.getPlayer()){
			case WHITE:
				this.whiteKing = piece;
				break;
			case BLACK:
				this.blackKing = piece;
				break;
			default:
				throw new RuntimeException("Unknown Player: " + piece.getPlayer());
			}
		}
	}
}
