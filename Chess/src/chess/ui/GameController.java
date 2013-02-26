package chess.ui;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import chess.game.Board.Move;
import chess.game.Game;
import chess.game.Location;
import chess.game.Piece;
import chess.game.PieceType;

public class GameController{
	private static final int ICON_SIZE = 30;
	private final Game model;
	private Location previewing = null;
	private Location selLoc = null;
	private Map<Location, Move> selMoves = null;
	private boolean strictOrientation = false;
	private final GameFrame view;
	
	public GameController(GameFrame app){
		this.view = app;
		this.model = this.view.getGame();
	}
	
	public Game getGame(){
		return this.model;
	}
	
	public boolean isStrictOrientation(){
		return this.strictOrientation;
	}
	
	public void preview(Location loc){
		if (!acceptInput()) return;
		if (this.selLoc == null){
			if (this.previewing != null){
				this.view.raise(this.previewing);
				this.previewing = null;
			}
			List<Move> moves = this.model.getMoves(loc);
			Location[] locs = new Location[moves.size()];
			int i = 0;
			for (Move move : moves){
				locs[i++] = move.getNewLocation();
			}
			this.view.setMoveLocations(locs);
			if (locs.length > 0){
				this.view.lower(loc);
				this.previewing = loc;
			}
		}
	}
	
	public void select(Location loc){
		if (!acceptInput()) return;
		List<Move> validMoves = this.model.getMoves(loc);
		if (this.selLoc == null){
			if (validMoves.size() == 0) return;
			Piece selPiece = this.model.getPiece(loc);
			if (selPiece != null){
				this.preview(loc);
				setSelection(loc, validMoves);
			}
		} else{
			Move move = loc != null ? this.selMoves.get(loc) : null;
			if (move != null){
				this.model.executeMove(move);
				if (move.isPromotion()){
					ImageIcon icon = getPieceIcon(loc);
					EnumSet<PieceType> promotionSet = PieceType.getPromotionTypes();
					PieceType[] promotions = promotionSet.toArray(new PieceType[promotionSet.size()]);
					int selection;
					selection = JOptionPane.showOptionDialog(	this.view,
																"Choose a piece to promote to:",
																"Pawn Promotion",
																JOptionPane.DEFAULT_OPTION,
																JOptionPane.QUESTION_MESSAGE,
																icon,
																promotions,
																promotions[0]);
					if (selection == JOptionPane.CLOSED_OPTION) selection = 0;
					this.model.promote(promotions[selection]);
				}
			}
			this.setSelection(null, null);
			this.preview(loc);
		}
	}
	
	public void setStrictOrientation(boolean strictOrientation){
		this.strictOrientation = strictOrientation;
	}
	
	private boolean acceptInput(){
		return !this.strictOrientation || this.model.getTurn() == this.view.getOrientation();
	}
	
	private ImageIcon getPieceIcon(Location loc){
		Image pieceImage = this.view.getPieceImage(this.model.getPiece(loc));
		BufferedImage scaled = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = scaled.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
							RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(pieceImage, 0, 0, ICON_SIZE, ICON_SIZE, null);
		ImageIcon icon = new ImageIcon(scaled);
		return icon;
	}
	
	private void setSelection(Location loc, List<Move> moves){
		this.selLoc = loc;
		if (this.selLoc != null){
			this.selMoves = new TreeMap<>();
			for (Move move : moves){
				this.selMoves.put(move.getNewLocation(), move);
			}
		} else this.selMoves = null;
		this.view.setSelection(loc);
	}
}
