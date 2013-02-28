package chess.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import chess.game.Board;
import chess.game.BoardChangedEvent;
import chess.game.BoardListener;
import chess.game.Game;
import chess.game.Location;
import chess.game.Piece;
import chess.game.Player;
import chess.ui.images.ImageFactory;

public class BoardPanel extends SquarePanel{
	private static final int DESIGN_SIZE = 400;
	
	private enum BoardColor{
		DARK(UIManager.getColor("controlDkShadow")),
		HIGHLIGHT(UIManager.getColor("textHighlight")),
		LIGHT(UIManager.getColor("controlHighlight")),
		MOVES(UIManager.getColor("controlShadow")),
		SELECTED(UIManager.getColor("control"));
		public final Color color;
		
		BoardColor(Color c){
			this.color = c;
		}
	}
	private class PiecePanel extends JPanel{
		public final Location boardLocation;
		private BoardColor curColor = null;
		
		protected PiecePanel(Location location){
			super(null, true);
			this.boardLocation = location;
			resetColor();
			setRaised(true);
		}
		
		public void resetColor(){
			if (isCheck()) this.setCurColor(BoardColor.HIGHLIGHT);
			else if (this.boardLocation.isLight()) this.setCurColor(BoardColor.LIGHT);
			else setCurColor(BoardColor.DARK);
		}
		
		public void setCurColor(BoardColor c){
			if (this.curColor == c) return;
			this.curColor = c;
			this.setBackground(c.color);
			this.repaint();
		}
		
		public void setRaised(boolean raised){
			if (raised) this.setBorder(BorderFactory.createRaisedBevelBorder());
			else this.setBorder(BorderFactory.createLoweredBevelBorder());
		}
		
		@Override
		protected void paintComponent(Graphics g){
			super.paintComponent(g);
			if (BoardPanel.this.game != null){
				Image image = BoardPanel.this.images.getPiece(BoardPanel.this.game.getPiece(this.boardLocation));
				if (image != null){
					g.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), null);
				}
			}
		}
		
		private boolean isCheck(){
			if (this.boardLocation.equals(BoardPanel.this.whiteKingLoc)
					&& BoardPanel.this.game.isWhiteCheck()) return true;
			if (this.boardLocation.equals(BoardPanel.this.blackKingLoc)
					&& BoardPanel.this.game.isBlackCheck()) return true;
			return false;
		}
	}
	
	protected GameController controller;
	protected Game game;
	protected ImageFactory images = new ImageFactory();
	protected Map<Location, PiecePanel> pieces;
	protected Location whiteKingLoc, blackKingLoc;
	private MouseListener controllerListener = new MouseAdapter(){
		@Override
		public void mouseEntered(MouseEvent e){
			if (BoardPanel.this.controller == null) return;
			BoardPanel.this.controller.preview(((PiecePanel)e.getSource()).boardLocation);
		}
		
		@Override
		public void mouseExited(MouseEvent e){
			if (BoardPanel.this.controller == null) return;
			BoardPanel.this.controller.preview(null);
		}
		
		@Override
		public void mousePressed(MouseEvent e){
			if (BoardPanel.this.controller == null) return;
			PiecePanel piecePanel = (PiecePanel)e.getSource();
			BoardPanel.this.controller.select(piecePanel.boardLocation);
			BoardPanel.this.requestFocusInWindow();
		}
	};
	private Collection<JLabel> labels = new ArrayList<>();
	private Location[] moveLocations;
	private Player orientation;
	private Location selection;
	
	public BoardPanel(){
		this(null, Player.WHITE);
	}
	
	public BoardPanel(GameController controller){
		this(controller, Player.WHITE);
	}
	
	public BoardPanel(GameController controller, Player orientation){
		super(new GridLayout(Board.SIZE + 1, Board.SIZE + 1));
		this.controller = controller;
		this.orientation = orientation;
		this.game = controller.getGame();
		this.blackKingLoc = this.game.getBlackKing().getLocation();
		this.whiteKingLoc = this.game.getWhiteKing().getLocation();
		addGameListeners();
		addComponentListener();
		addKeyListener();
		this.setBorder(BorderFactory.createRaisedBevelBorder());
		addPiecePanels();
		this.setPreferredSize(new Dimension(DESIGN_SIZE, DESIGN_SIZE));
	}
	
	public Image getPieceImage(Piece piece){
		return this.images.getPiece(piece);
	}
	
	protected void lower(Location loc){
		this.pieces.get(loc).setRaised(false);
	}
	
	protected void raise(Location loc){
		this.pieces.get(loc).setRaised(true);
	}
	
	protected void setBlackKingLoc(Location blackKingLoc){
		Location old = this.blackKingLoc;
		this.blackKingLoc = blackKingLoc;
		this.pieces.get(old).resetColor();
		this.pieces.get(this.whiteKingLoc).resetColor();
	}
	
	protected void setLabelSize(int dim){
		Font f = new Font(Font.SANS_SERIF, Font.PLAIN, dim / 16);
		for (JLabel label : this.labels){
			label.setFont(f);
		}
	}
	
	protected void setMoveLocations(Location[] newMoves){
		if (this.moveLocations != null) for (Location loc : this.moveLocations){
			this.pieces.get(loc).resetColor();
		}
		this.moveLocations = newMoves;
		if (this.moveLocations != null) for (Location loc : this.moveLocations){
			this.pieces.get(loc).setCurColor(BoardColor.MOVES);
		}
	}
	
	protected void setSelection(Location loc){
		if (this.selection != null){
			PiecePanel panel = this.pieces.get(this.selection);
			panel.resetColor();
		}
		this.selection = loc;
		if (this.selection != null){
			this.pieces.get(loc).setCurColor(BoardColor.SELECTED);
		}
	}
	
	protected void setWhiteKingLoc(Location whiteKingLoc){
		Location old = this.whiteKingLoc;
		this.whiteKingLoc = whiteKingLoc;
		this.pieces.get(old).resetColor();
		this.pieces.get(this.whiteKingLoc).resetColor();
	}
	
	private void addComponentListener(){
		this.addComponentListener(new ComponentAdapter(){
			@Override
			public void componentResized(java.awt.event.ComponentEvent e){
				Insets border = getInsets();
				int size = Math.min(getWidth() - border.left - border.right, getHeight()
						- border.top
						- border.bottom);
				setLabelSize(size);
				BoardPanel.this.requestFocusInWindow();
			}
			
			@Override
			public void componentShown(ComponentEvent e){
				BoardPanel.this.requestFocusInWindow();
			}
		});
	}
	
	private void addGameListeners(){
		this.game.addPropertyChangeListener("whiteCheck", new PropertyChangeListener(){
			@Override
			public void propertyChange(PropertyChangeEvent evt){
				BoardPanel.this.resetColor(BoardPanel.this.whiteKingLoc);
			}
		});
		this.game.addPropertyChangeListener("blackCheck", new PropertyChangeListener(){
			@Override
			public void propertyChange(PropertyChangeEvent evt){
				BoardPanel.this.resetColor(BoardPanel.this.blackKingLoc);
			}
		});
		this.game.addBoardListener(new BoardListener(){
			@Override
			public void boardChanged(BoardChangedEvent e){
				final Location[] changes = e.getLocations();
				if (SwingUtilities.isEventDispatchThread()){
					updateBoard(changes);
				} else{
					try{
						SwingUtilities.invokeAndWait(new Runnable(){
							@Override
							public void run(){
								updateBoard(changes);
							}
						});
					} catch (InvocationTargetException | InterruptedException e1){
						e1.printStackTrace();
					}
				}
			}
		});
	}
	
	private void addKeyListener(){
		addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(KeyEvent e){
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE){
					BoardPanel.this.controller.select(null);
				}
			}
		});
	}
	
	/**
	 * This method initializes piecesPanel
	 * 
	 * @return javax.swing.JPanel
	 */
	private void addPiecePanels(){
		int min, max, inc;
		if (this.orientation == Player.WHITE){
			min = 0;
			max = Board.SIZE;
			inc = 1;
		} else{
			min = Board.SIZE - 1;
			max = -1;
			inc = -1;
		}
		this.pieces = new HashMap<>(Board.SIZE * Board.SIZE, 1.0f);
		for (int i = min; i != max; i += inc){
			String row = Location.getRowLabel(i);
			JLabel rowLabel = new JLabel(row, SwingConstants.CENTER);
			this.labels.add(rowLabel);
			this.add(rowLabel);
			for (int j = min; j != max; j += inc){
				Location loc = new Location(i, j);
				PiecePanel panel = new PiecePanel(loc);
				panel.addMouseListener(this.controllerListener);
				this.pieces.put(loc, panel);
				this.add(panel);
			}
		}
		this.add(new JLabel());
		for (int j = min; j != max; j += inc){
			String col = Location.getColumnLabel(j);
			JLabel colLabel = new JLabel(col, SwingConstants.CENTER);
			colLabel.setVerticalAlignment(SwingConstants.TOP);
			this.labels.add(colLabel);
			this.add(colLabel);
		}
	}
	
	private void resetColor(final Location location){
		if (SwingUtilities.isEventDispatchThread()){
			this.pieces.get(location).resetColor();
		} else{
			try{
				SwingUtilities.invokeAndWait(new Runnable(){
					@Override
					public void run(){
						BoardPanel.this.pieces.get(location).resetColor();
					}
				});
			} catch (InvocationTargetException | InterruptedException e){
				e.printStackTrace();
			}
		}
	}
	
	private void updateBoard(Location[] changes){
		if (changes == null) BoardPanel.this.repaint();
		else for (Location loc : changes){
			Piece piece = BoardPanel.this.game.getPiece(loc);
			if (BoardPanel.this.game.getBlackKing().equals(piece)){
				setBlackKingLoc(loc);
			} else if (BoardPanel.this.game.getWhiteKing().equals(piece)){
				setWhiteKingLoc(loc);
			}
			BoardPanel.this.pieces.get(loc).repaint();
		}
		BoardPanel.this.controller.select(null);
	}
}
