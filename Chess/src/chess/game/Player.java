package chess.game;

import java.util.EnumSet;

public enum Player{
	WHITE('l', Direction.NORTH), BLACK('d', Direction.SOUTH), GAME_OVER('\0', null){
		@Override
		public Player next(){
			return this;
		}
	};
	public static Player getPlayer(char label){
		for (Player p : Player.values()){
			if (Character.toLowerCase(label) == p.label) return p;
		}
		throw new IllegalArgumentException("No such label");
	}
	public static EnumSet<Player> getPlayers(){
		return EnumSet.range(Player.WHITE, Player.BLACK);
	}
	
	public final char label;
	
	public final Direction forward;
	
	private Player(char label, Direction forward){
		this.label = label;
		this.forward = forward;
	}
	
	public Player next(){
		Player[] players = Player.values();
		int position = (this.ordinal() + 1) % (players.length - 1);
		return players[position];
	}
}