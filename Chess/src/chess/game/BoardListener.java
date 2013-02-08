package chess.game;

import java.util.EventListener;

public interface BoardListener extends EventListener{
	void boardChanged(BoardChangedEvent e);
}
