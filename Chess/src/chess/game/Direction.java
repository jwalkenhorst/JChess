package chess.game;

public enum Direction{
	NORTH{
		@Override
		public Location translate(Location location){
			return new Location(location.row - 1, location.column);
		}
	},
	SOUTH{
		@Override
		public Location translate(Location location){
			return new Location(location.row + 1, location.column);
		}
	},
	WEST{
		@Override
		public Location translate(Location location){
			return new Location(location.row, location.column - 1);
		}
	},
	EAST{
		@Override
		public Location translate(Location location){
			return new Location(location.row, location.column + 1);
		}
	},
	NORTHWEST{
		@Override
		public Location translate(Location location){
			return new Location(location.row - 1, location.column - 1);
		}
	},
	NORTHEAST{
		@Override
		public Location translate(Location location){
			return new Location(location.row - 1, location.column + 1);
		}
	},
	SOUTHWEST{
		@Override
		public Location translate(Location location){
			return new Location(location.row + 1, location.column - 1);
		}
	},
	SOUTHEAST{
		@Override
		public Location translate(Location location){
			return new Location(location.row + 1, location.column + 1);
		}
	};
	/**
	 * @return a new Location which is the result of translating location in the direction specified by this object.
	 */
	public abstract Location translate(Location location);
}
