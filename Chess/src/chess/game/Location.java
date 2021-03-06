package chess.game;

import java.io.Serializable;

public class Location implements Serializable, Comparable<Location>{
	public static String getColumnLabel(int column){
		return String.valueOf((char)(column + 'a'));
	}
	
	public static String getRowLabel(int row){
		return String.valueOf(Board.SIZE - row);
	}
	
	public final int row, column;
	
	public Location(int row, int column){
		this.row = row;
		this.column = column;
	}
	
	@Override
	public boolean equals(Object obj){
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Location other = (Location)obj;
		if (this.row != other.row) return false;
		if (this.column != other.column) return false;
		return true;
	}
	
	public String getColumnLabel(){
		return String.valueOf((char)(this.column + 'a'));
	}
	
	public String getRowLabel(){
		return String.valueOf(Board.SIZE - this.row);
	}
	
	@Override
	public int hashCode(){
		return Board.SIZE * this.row + this.column;
	}
	
	public boolean isLight(){
		return this.row % 2 == this.column % 2;
	}
	
	public boolean isOnBoard(){
		return Board.onBoard(this);
	}
	
	public Location move(Direction... dirs){
		Location l = this;
		for (Direction d : dirs){
			l = d.translate(l);
		}
		return l;
	}
	
	@Override
	public String toString(){
		return String.format("%s%s", this.getRowLabel(), this.getColumnLabel());
	}
	
	@Override
	public int compareTo(Location l){
		int rowDif = this.row - l.row;
		return (rowDif != 0) ? rowDif : this.column - l.column;
	}
}
