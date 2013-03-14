package chess.game;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import chess.game.Board.Move;

public enum PieceType{
	KING{
		@Override
		List<Location> getLocations(Piece king, Location location){
			List<Location> locs = getBasicLocations(king, location);
			Move east = getCastle(king, location, Direction.EAST);
			if (east != null) locs.add(east.getNewLocation());
			Move west = getCastle(king, location, Direction.WEST);
			if (west != null) locs.add(west.getNewLocation());
			return locs;
		}
		
		@Override
		List<Move> getMoves(Piece king, Location location){
			List<Move> moves = new LinkedList<>();
			Board board = king.getBoard();
			for (Location l : getBasicLocations(king, location)){
				moves.add(board.makeMove(location, l));
			}
			Move east = getCastle(king, location, Direction.EAST);
			if (east != null) moves.add(east);
			Move west = getCastle(king, location, Direction.WEST);
			if (west != null) moves.add(west);
			return moves;
		}
		
		@Override
		boolean hasMove(Piece piece, Location from, Location to){
			if (Math.abs(from.row - to.row) > 1) return false;
			return super.hasMove(piece, from, to);
		}
		
		private List<Location> getBasicLocations(Piece king, Location location){
			List<Location> locs = new LinkedList<>();
			for (Direction d : Direction.values()){
				Location next = location.move(d);
				if (king.isAvailable(next) || king.canAttack(next)) locs.add(next);
			}
			return locs;
		}
		
		private Move getCastle(Piece king, Location location, Direction dir){
			if (king.isMoved() || king.isPlayerCheck()) return null;
			Board board = king.getBoard();
			Location next = location;
			Piece atNext;
			do{
				next = next.move(dir);
				atNext = board.getPiece(next);
			} while (next.isOnBoard() && atNext == null);
			Piece rook = new Piece(ROOK, king.getPlayer(), king.getBoard());
			if (rook.equals(atNext) && !atNext.isMoved()){
				Location passing = location.move(dir);
				Move passingMove = board.makeMove(location, passing);
				if (!passingMove.checksPlayer()) return board.makeMove(	location,
																		passing.move(dir),
																		next,
																		passing);
			}
			return null;
		}
	},
	QUEEN{
		@Override
		List<Location> getLocations(Piece queen, Location location){
			List<Location> locs = movesTowards(queen, location, EnumSet.allOf(Direction.class));
			return locs;
		}
	},
	BISHOP{
		@Override
		List<Location> getLocations(Piece bishop, Location location){
			Iterable<Direction> directions = EnumSet.of(Direction.NORTHWEST,
														Direction.NORTHEAST,
														Direction.SOUTHEAST,
														Direction.SOUTHWEST);
			List<Location> locs = movesTowards(bishop, location, directions);
			return locs;
		}
	},
	KNIGHT('N'){
		@Override
		List<Location> getLocations(Piece knight, Location location){
			List<Location> locations = new LinkedList<>();
			int rows = location.row + 2;
			int cols = location.column + 2;
			for (int row = location.row - 2; row <= rows; row++){
				if (row == location.row) continue;
				for (int col = location.column - 2; col <= cols; col++){
					if (col == location.column) continue;
					Location next = new Location(row, col);
					if (next.isLight() == location.isLight()) continue;
					if (knight.isAvailable(next) || knight.canAttack(next)) locations.add(next);
				}
			}
			return locations;
		}
	},
	ROOK{
		@Override
		List<Location> getLocations(Piece rook, Location location){
			Iterable<Direction> directions = EnumSet.of(Direction.NORTH,
														Direction.EAST,
														Direction.SOUTH,
														Direction.WEST);
			List<Location> moves = movesTowards(rook, location, directions);
			return moves;
		}
	},
	PAWN{
		@Override
		List<Location> getLocations(Piece pawn, Location location){
			List<Location> locs = getBasicLocations(pawn, location);
			Location passant = getPassantLocation(pawn, location);
			if (passant != null) locs.add(passant);
			return locs;
		}
		
		@Override
		List<Move> getMoves(Piece pawn, Location location){
			List<Location> locs = getBasicLocations(pawn, location);
			List<Move> moves = new LinkedList<>();
			Board board = pawn.getBoard();
			for (Location l : locs){
				moves.add(board.makeMove(location, l));
			}
			Location passant = getPassantLocation(pawn, location);
			if (passant != null){
				Move lastMove = pawn.getBoard().getLast();
				moves.add(board.makeMove(location, passant, lastMove.getNewLocation(), null));
			}
			return moves;
		}
		
		private List<Location> getBasicLocations(Piece pawn, Location location){
			List<Location> locs = new LinkedList<>();
			//single forward move
			Location forward = location.move(pawn.forward());
			if (pawn.isAvailable(forward)){
				locs.add(forward);
				if (!pawn.isMoved()){
					//double forward move
					Location again = forward.move(pawn.forward());
					if (pawn.isAvailable(again)){
						locs.add(again);
					}
				}
			}
			//attack left
			Location takeLeft = forward.move(Direction.WEST);
			if (pawn.canAttack(takeLeft)) locs.add(takeLeft);
			//attack right
			Location takeRight = forward.move(Direction.EAST);
			if (pawn.canAttack(takeRight)) locs.add(takeRight);
			return locs;
		}
		
		private Location getPassantLocation(Piece pawn, Location location){
			Move lastMove = pawn.getBoard().getLast();
			if (lastMove != null){
				if (lastMove.getMoving().getType() == PAWN){
					Location lastNewLocation = lastMove.getNewLocation();
					if (location.row == lastNewLocation.row
							&& Math.abs(location.column - lastNewLocation.column) == 1
							&& Math.abs(lastMove.getOldLocation().row - lastNewLocation.row) == 2){
						return lastNewLocation.move(pawn.forward());
					}
				}
			}
			return null;
		}
	};
	public static PieceType getPieceType(char label){
		for (PieceType p : PieceType.values()){
			if (Character.toUpperCase(label) == p.label) return p;
		}
		throw new IllegalArgumentException("No such label");
	}
	
	public static EnumSet<PieceType> getPromotionTypes(){
		return EnumSet.complementOf(EnumSet.of(PieceType.KING, PieceType.PAWN));
	}
	
	public static boolean promotePieceAt(Location loc, Piece piece){
		return (piece.getType() == PieceType.PAWN && (loc.row == 0 || loc.row == Board.SIZE - 1));
	}
	
	static List<Location> movesTowards(Piece piece, Location location,
			Iterable<Direction> directions){
		List<Location> moves = new LinkedList<>();
		for (Direction d : directions){
			Location next = location.move(d);
			while (piece.isAvailable(next)){
				moves.add(next);
				next = next.move(d);
			}
			if (piece.canAttack(next)){
				moves.add(next);
			}
		}
		return moves;
	}
	
	public final char label;
	
	private PieceType(){
		this.label = this.name().charAt(0);
	}
	
	private PieceType(char label){
		this.label = label;
	}
	
	abstract List<Location> getLocations(Piece piece, Location location);
	
	List<Move> getMoves(Piece piece, Location location){
		List<Location> locs = getLocations(piece, location);
		List<Move> moves = new LinkedList<>();
		Board board = piece.getBoard();
		for (Location l : locs){
			Move move = board.makeMove(location, l);
			moves.add(move);
		}
		return moves;
	}
	
	boolean hasMove(Piece piece, Location from, Location to){
		List<Move> moves = getMoves(piece, from);
		for (Move move : moves){
			if (move.getNewLocation().equals(to)) return true;
		}
		return false;
	}
}