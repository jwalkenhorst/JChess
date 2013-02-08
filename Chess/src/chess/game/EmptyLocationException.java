package chess.game;

public class EmptyLocationException extends RuntimeException{
	private Location location;
	
	public EmptyLocationException(Location location){
		super(String.format("No piece on %s", location));
		this.location = location;
	}
	
	public Location getLocation(){
		return this.location;
	}
}
