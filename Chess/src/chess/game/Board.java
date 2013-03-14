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
	
	public abstract class Move implements Serializable{
		public abstract boolean checksPlayer();
		
		/**
		 * @return The piece captured after executing this move. Null if no capture did/will occurr.
		 */
		public abstract Piece getCaptured();
		
		/**
		 * @return The piece that will be or has moved.
		 */
		public abstract Piece getMoving();
		
		public abstract Location getNewLocation();
		
		public abstract Location getOldLocation();
		
		/**
		 * @return if this Move requires a piece to need promotion.
		 */
		public abstract boolean promotesPiece();
		
		protected abstract void execute();
		
		protected abstract void undo();
	}
	/**
	 * A snapshot of where a Piece is located when this object is created.
	 * 
	 * @author jwalkenhorst
	 */
	protected static class PieceLocation implements Serializable{
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
			result = prime * result + ((this.location == null) ? 0 : this.location.hashCode());
			result = prime * result + ((this.piece == null) ? 0 : this.piece.hashCode());
			return result;
		}
		
		protected boolean hasMoveTo(Location end){
			return this.piece.hasMove(this.location, end);
		}
	}
	private class DoubleMove extends SingleMove{
		private Piece secondMoving, secondCapture;
		private Location secondOld, secondNew;
		
		private DoubleMove(Location oldLocation, Location newLocation, Location secondOld,
				Location secondNew){
			super(oldLocation, newLocation);
			this.secondOld = secondOld;
			this.secondNew = secondNew;
		}
		
		@Override
		public String toString(){
			return super.toString() + ", " + this.secondOld + "->" + this.secondNew;
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
	/**
	 * Represents the result of moving a piece.
	 * 
	 * @author jwalkenhorst
	 */
	private class SingleMove extends Move{
		int prevNonCaptureMoves;
		private Piece captured;
		private boolean executed = false;
		private Piece moving;
		private Location oldLocation, newLocation;
		private Move previous;
		
		private boolean promotion;
		
		private SingleMove(Location oldLocation, Location newLocation){
			if (!oldLocation.isOnBoard()) throw new OffBoardException(oldLocation);
			if (!newLocation.isOnBoard()) throw new OffBoardException(newLocation);
			Piece m = Board.this.pieces.get(oldLocation);
			if (m == null){
				throw new EmptyLocationException(oldLocation);
			}
			this.moving = m;
			this.oldLocation = oldLocation;
			this.newLocation = newLocation;
			this.promotion = PieceType.promotePieceAt(newLocation, m);
			this.captured = Board.this.pieces.get(this.newLocation);
		}
		
		public boolean checksPlayer(){
			Board.this.notifyListeners = false;
			execute();
			boolean check = Board.this.isCheck(this.moving.getPlayer());
			undo();
			Board.this.notifyListeners = true;
			return check;
		}
		
		/**
		 * @return The piece captured after executing this move. Null if no capture did/will occurr.
		 */
		public Piece getCaptured(){
			return this.captured;
		}
		
		/**
		 * @return The piece that will be or has moved.
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
		 * @return if this Move requires a piece to need promotion.
		 */
		public boolean promotesPiece(){
			return this.promotion;
		}
		
		@Override
		public String toString(){
			return this.oldLocation + "->" + this.newLocation;
		}
		
		protected void execute(){
			if (this.executed) throw new IllegalStateException("Move already executed");
			this.executed = true;
			this.moving = Board.this.pieces.remove(this.oldLocation);
			if (this.moving == null) throw new EmptyLocationException(this.oldLocation);
			this.captured = Board.this.pieces.put(this.newLocation, this.moving);
			prevNonCaptureMoves = nonCaptureMoves;
			if (this.captured == null) nonCaptureMoves++;
			else nonCaptureMoves = 0;
			this.moving.incrementMoves();
			this.previous = Board.this.last;
			Board.this.last = this;
			Board.this.fireBoardChanged(new Location[]{this.oldLocation, this.newLocation});
		}
		
		protected void undo(){
			if (!this.executed) throw new IllegalStateException("Move not executed");
			this.moving.decrementMoves();
			nonCaptureMoves = prevNonCaptureMoves;
			if (this.captured == null) Board.this.pieces.remove(this.newLocation);
			else Board.this.pieces.put(this.newLocation, this.captured);
			Board.this.pieces.put(this.oldLocation, this.moving);
			this.executed = false;
			Board.this.last = this.previous;
			Board.this.fireBoardChanged(new Location[]{this.oldLocation, this.newLocation});
		}
	}
	
	protected Move last;
	protected int nonCaptureMoves;
	protected boolean notifyListeners = true;
	protected Map<Location, Piece> pieces;
	private Piece blackKing, whiteKing;
	private transient List<BoardListener> listeners;
	public Board(){
		this.pieces = new HashMap<>(Board.SIZE * Board.SIZE, 1.0f);
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
	
	public int getNonCaptureMoves(){
		return this.nonCaptureMoves;
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
	
	protected Move getLast(){
		return this.last;
	}
	
	/**
	 * @return an unexecuted Move that moves the piece at oldLocation to newLocation
	 * @throws OffBoardException
	 *             if newLocation is not on the board
	 * @throws EmptyLocationException
	 *             if oldLocation is not occupied
	 */
	protected Move makeMove(Location oldLocation, Location newLocation){
		return new SingleMove(oldLocation, newLocation);
	}
	
	protected Move makeMove(Location oldLocation, Location newLocation, Location secondOld,
			Location secondNew){
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
