package chess.ui.images;

import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import chess.game.Piece;
import chess.game.PieceType;
import chess.game.Player;

public class ImageFactory{
	private static final String IMAGE_CLASSPATH = "/chess/ui/images/pieces/";
	private Map<Piece, Image> pieces;
	
	public ImageFactory(){
		this.pieces = new HashMap<>();
		for (PieceType type : PieceType.values()){
			for (Player player : Player.getPlayers()){
				Piece p = new Piece(type, player, null);
				String imagePath = String.format("%s%s/%s.png", IMAGE_CLASSPATH, player.toString().toLowerCase(), type.toString().toLowerCase());
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
		return this.pieces.get(piece);
	}
}
