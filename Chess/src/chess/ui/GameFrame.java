package chess.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Event;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import chess.game.Game;
import chess.game.Location;
import chess.game.Piece;
import chess.game.Player;

public class GameFrame extends JFrame{
	public static final boolean MULTI_VIEW = false;
	
	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e){
			//Use default look and feel
		}
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run(){
				GameFrame white = new GameFrame();
				white.setVisible(true);
				white.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				white.pack();
				if (MULTI_VIEW){
					white.setStrictOrientation(true);
					GameFrame black = new GameFrame(white.getGame(), Player.BLACK);
					black.setVisible(true);
					black.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					black.setLocation(400, 0);
					black.setStrictOrientation(true);
				}
			}
		});
	}
	
	private JPanel aboutContentPane = null;
	private JDialog aboutDialog = null;
	private JMenuItem aboutMenuItem = null;
	private JLabel aboutVersionLabel = null;
	private BoardPanel boardPanel = null;
	protected GameController controller;
	private JMenuItem exitMenuItem = null;
	protected Game game;
	private JMenu gameMenu = null;
	private JMenu helpMenu = null;
	private JMenuBar menuBar = null;
	private JMenuItem newMenuItem = null;
	private Player orientation;
	private JMenuItem saveAsMenuItem = null;
	private JMenuItem saveMenuItem = null;
	
	public GameFrame(){
		this(null, Player.WHITE);
	}
	
	public GameFrame(Game game, Player orientation){
		super("Chess: " + orientation);
		this.orientation = orientation;
		this.game = game == null ? new Game() : game;
		this.controller = new GameController(this);
		this.setJMenuBar(getMainMenuBar());
		this.setContentPane(getBoardPanel());	
	}
	
	public Game getGame(){
		return this.game;
	}
	
	public Player getOrientation(){
		return this.orientation;
	}
	
	public Image getPieceImage(Piece piece){
		return this.boardPanel.getPieceImage(piece);
	}
	
	public boolean isStrictOrientation(){
		return this.controller.isStrictOrientation();
	}
	
	public void lower(Location loc){
		this.getBoardPanel().lower(loc);
	}
	
	public void raise(Location loc){
		this.getBoardPanel().raise(loc);
	}
	
	public void setMoveLocations(Location[] moves){
		this.getBoardPanel().setMoveLocations(moves);
	}
	
	public void setSelection(Location loc){
		this.getBoardPanel().setSelection(loc);
	}
	
	public void setStrictOrientation(boolean strictOrientation){
		this.controller.setStrictOrientation(strictOrientation);
	}
	
	/**
	 * This method initializes aboutDialog
	 * 
	 * @return javax.swing.JDialog
	 */
	protected JDialog getAboutDialog(){
		if (this.aboutDialog == null){
			this.aboutDialog = new JDialog(GameFrame.this, true);
			this.aboutDialog.setTitle("About");
			this.aboutDialog.setContentPane(getAboutContentPane());
		}
		return this.aboutDialog;
	}
	
	/**
	 * This method initializes aboutContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getAboutContentPane(){
		if (this.aboutContentPane == null){
			this.aboutContentPane = new JPanel();
			this.aboutContentPane.setLayout(new BorderLayout());
			this.aboutContentPane.add(getAboutVersionLabel(), BorderLayout.CENTER);
		}
		return this.aboutContentPane;
	}
	
	/**
	 * This method initializes jMenuItem
	 * 
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getAboutMenuItem(){
		if (this.aboutMenuItem == null){
			this.aboutMenuItem = new JMenuItem();
			this.aboutMenuItem.setText("About");
			this.aboutMenuItem.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					JDialog about = getAboutDialog();
					about.pack();
					Point loc = GameFrame.this.getLocation();
					loc.translate(20, 20);
					about.setLocation(loc);
					about.setVisible(true);
				}
			});
		}
		return this.aboutMenuItem;
	}
	
	/**
	 * This method initializes aboutVersionLabel
	 * 
	 * @return javax.swing.JLabel
	 */
	private JLabel getAboutVersionLabel(){
		if (this.aboutVersionLabel == null){
			this.aboutVersionLabel = new JLabel();
			this.aboutVersionLabel.setText("Version 1.0");
			this.aboutVersionLabel.setHorizontalAlignment(SwingConstants.CENTER);
		}
		return this.aboutVersionLabel;
	}
	
	/**
	 * This method initializes boardPanel
	 * 
	 * @return chess.ui.BoardPanel
	 */
	private BoardPanel getBoardPanel(){
		if (this.boardPanel == null){
			this.boardPanel = new BoardPanel(this.controller, this.orientation);
		}
		return this.boardPanel;
	}
	
	/**
	 * This method initializes jMenuItem
	 * 
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getExitMenuItem(){
		if (this.exitMenuItem == null){
			this.exitMenuItem = new JMenuItem();
			this.exitMenuItem.setText("Exit");
			this.exitMenuItem.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					System.exit(0);
				}
			});
		}
		return this.exitMenuItem;
	}
	
	/**
	 * This method initializes jMenu
	 * 
	 * @return javax.swing.JMenu
	 */
	private JMenu getGameMenu(){
		if (this.gameMenu == null){
			this.gameMenu = new JMenu();
			this.gameMenu.setText("Game");
			this.gameMenu.add(getUndoMenuItem());
			this.gameMenu.add(getNewMenuItem());
			this.gameMenu.add(getSaveMenuItem());
			this.gameMenu.add(getSaveAsMenuItem());
			this.gameMenu.addSeparator();
			this.gameMenu.add(getExitMenuItem());
		}
		return this.gameMenu;
	}
	
	
	/**
	 * This method initializes jMenu
	 * 
	 * @return javax.swing.JMenu
	 */
	private JMenu getHelpMenu(){
		if (this.helpMenu == null){
			this.helpMenu = new JMenu();
			this.helpMenu.setText("Help");
			this.helpMenu.add(getAboutMenuItem());
		}
		return this.helpMenu;
	}
	
	/**
	 * This method initializes menuBar
	 * 
	 * @return javax.swing.JMenuBar
	 */
	private JMenuBar getMainMenuBar(){
		if (this.menuBar == null){
			this.menuBar = new JMenuBar();
			this.menuBar.add(getGameMenu());
			this.menuBar.add(getHelpMenu());
		}
		return this.menuBar;
	}
	
	/**
	 * This method initializes newMenuItem
	 * 
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getNewMenuItem(){
		if (this.newMenuItem == null){
			this.newMenuItem = new JMenuItem();
			this.newMenuItem.setText("New");
			this.newMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK, true));
		}
		return this.newMenuItem;
	}

	private JMenuItem undoMenuItem;
	private JMenuItem getUndoMenuItem(){
		if (this.undoMenuItem== null){
			this.undoMenuItem = new JMenuItem("Undo Last Move");
			this.undoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK, true));
			this.undoMenuItem.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					GameFrame.this.game.undo();
				}
			});
		}
		return this.undoMenuItem;
	}
	
	/**
	 * This method initializes saveAsMenuItem
	 * 
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getSaveAsMenuItem(){
		if (this.saveAsMenuItem == null){
			this.saveAsMenuItem = new JMenuItem();
			this.saveAsMenuItem.setText("Save As...");
		}
		return this.saveAsMenuItem;
	}
	
	/**
	 * This method initializes jMenuItem
	 * 
	 * @return javax.swing.JMenuItem
	 */
	private JMenuItem getSaveMenuItem(){
		if (this.saveMenuItem == null){
			this.saveMenuItem = new JMenuItem();
			this.saveMenuItem.setText("Save");
			this.saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK, true));
		}
		return this.saveMenuItem;
	}
}
