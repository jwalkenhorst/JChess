package chess.game;

import java.util.Arrays;
import java.util.EventObject;

public class BoardChangedEvent extends EventObject{
	@Override
	public String toString(){
		return this.getClass() + Arrays.toString(this.locations);
	}
	
	protected Location[] locations;
	
	public BoardChangedEvent(Object source, Location[] locations){
		super(source);
		this.locations = locations;
	}
	
	public Location[] getLocations(){
		return this.locations;
	}
}