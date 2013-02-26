package chess.ui;

import java.awt.LayoutManager;
import java.awt.Rectangle;

import javax.swing.JPanel;

public class SquarePanel extends JPanel{
	public SquarePanel(){
		super();
	}
	
	public SquarePanel(LayoutManager layout){
		super(layout);
	}
	
	public SquarePanel(boolean isDoubleBuffered){
		super(isDoubleBuffered);
	}
	
	public SquarePanel(LayoutManager layout, boolean isDoubleBuffered){
		super(layout, isDoubleBuffered);
	}

	@Deprecated
	@Override
	@SuppressWarnings("deprecation")
	public void reshape(int x, int y, int w, int h){
		int size = Math.min(w, h);
		super.reshape(x, y, size, size);
	}

	@Override
	public void setBounds(int x, int y, int width, int height){
		int size = Math.min(width, height);
		super.setBounds(x, y, size, size);
	}

	@Override
	public void setBounds(Rectangle r){
		this.setBounds(r.x, r.y, r.width, r.height);
	}
}