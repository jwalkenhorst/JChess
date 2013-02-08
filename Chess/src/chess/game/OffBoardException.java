package chess.game;

public class OffBoardException extends RuntimeException{
	
	private Location location;
	
	public OffBoardException(Location location){
		super(String.format("%s is off the board", location));
		this.location = location;
	}

	public Location getLocation(){
		return this.location;
	}
}
