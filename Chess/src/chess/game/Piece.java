package chess.game;

import java.io.Serializable;
import java.util.List;

import chess.game.Board.Move;

public class Piece implements Serializable{
	private Board board;
	private int moveCount = 0;
	private final Player player;
	private PieceType type;
	
	public Piece(PieceType type, Player player, Board board){
		this.type = type;
		this.player = player;
		this.board = board;
	}
	
	public Piece(String label, Board board){
		this(PieceType.getPieceType(label.charAt(0)), Player.getPlayer(label.charAt(1)), board);
	}
	
	@Override
	public boolean equals(Object obj){
		if (this == obj){
			return true;
		}
		if (obj == null){
			return false;
		}
		if (getClass() != obj.getClass()){
			return false;
		}
		Piece other = (Piece)obj;
		if (this.player != other.player){
			return false;
		}
		if (this.type != other.type){
			return false;
		}
		return true;
	}
	
	public Direction forward(){
		return this.player.forward;
	}
	
	public Location getLocation(){
		return this.board == null? null : this.board.findPiece(this);
	}
	
	public Player getOpponent(){
		return this.player.next();
	}
	
	public Player getPlayer(){
		return this.player;
	}	
	
	public PieceType getType(){
		return this.type;
	}
	
	@Override
	public int hashCode(){
		return this.player.hashCode();
	}
	
	/**
	 * @return if this Piece has moved.
	 */
	public boolean isMoved(){
		return this.moveCount > 0;
	}
	
	@Override
	public String toString(){
		return String.format("%c%c", this.type.label, this.player.label);
	}
	
	/**
	 * Checks if toAttack is on the board and occupied by an opposing piece
	 */
	protected boolean canAttack(Location toAttack){
		boolean attack = false;
		if (toAttack.isOnBoard()){
			Piece opponent = this.board.getPiece(toAttack);
			if (opponent != null && opponent.getPlayer() != this.player) attack = true;
		}
		return attack;
	}
	
	/**
	 * Checks if toMove is on the board and not occupied
	 */
	protected boolean isAvailable(Location toMove){
		return toMove.isOnBoard() && this.board.getPiece(toMove) == null;
	}
	
	protected boolean hasMove(Location from, Location to){
		return this.type.hasMove(this, from, to);
	}
	
	/**
	 * Decrements the move count.
	 */
	protected void decrementMoves(){
		if (this.moveCount > 0) this.moveCount--;
	}
	
	protected Board getBoard(){
		return this.board;
	}
	
	protected List<Move> getMoves(Location loc){
		return this.type.getMoves(this, loc);
	}
	
	/**
	 * Increments the move count.
	 */
	protected void incrementMoves(){
		this.moveCount++;
	}
	
	protected boolean isPlayerCheck(){
		return this.board.isCheck(this.player);
	}
	
	protected void setType(PieceType type){
		this.type = type;
		this.board.update(this.getLocation());
	}
}
