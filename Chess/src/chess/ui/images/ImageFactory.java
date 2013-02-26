package chess.ui.images;

import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import chess.game.Piece;
import chess.game.PieceType;
import chess.game.Player;

public class ImageFactory{
	private static final String IMAGE_CLASSPATH = "/chess/ui/images/pieces/";
	private Map<Piece, Image> pieces;
	
	public ImageFactory(){
		this.pieces = new TreeMap<>();
		for (PieceType type : PieceType.values()){
			for (Player player : Player.getPlayers()){
				Piece p = new Piece(type, player, null);
				String imagePath = String.format(	"%s%s/%s.png",
													IMAGE_CLASSPATH,
													player.toString().toLowerCase(),
													type.toString().toLowerCase());
				URL imURL = this.getClass().getResource(imagePath);
				try{
					this.pieces.put(p, ImageIO.read(imURL));
				} catch (IOException e){
					e.printStackTrace();
				}
			}
		}
	}
	
	public Image getPiece(Piece piece){
		return piece != null ? this.pieces.get(piece) : null;
	}
}
