package free.jin.board;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;

/**
 *
 */
public class PlayerLabel extends JLabel {

    private JPopupMenu menu;

    public PlayerLabel(JPopupMenu menu, String text) {
      super(text);
      this.menu = menu;
      addMouseListener(new PopupTriggerListener());
    }

    class PopupTriggerListener extends MouseAdapter {

        public void mousePressed(MouseEvent ev) {
            if (ev.isPopupTrigger()) {
                menu.show(ev.getComponent(), ev.getX(), ev.getY());
            }
        }

        public void mouseReleased(MouseEvent ev) {
            if (ev.isPopupTrigger()) {
                menu.show(ev.getComponent(), ev.getX(), ev.getY());
            }
        }

        public void mouseClicked(MouseEvent ev) {
        }
    }
}