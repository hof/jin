/**
 * Jin - a chess client for internet chess servers.
 * More information is available at http://www.jinchess.com/.
 * Copyright (C) 2003 Alexander Maryanovsky.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package free.jin.ui;

import free.jin.Connection;
import free.jin.console.Console;
import java.awt.event.ActionEvent;
import javax.swing.JMenuItem;

  /**
   * A <code>JMenuItem</code> which, when activated, sends a command to the
   * server consisting of the current selection in the console appended to the
   * specified string. Useful for the console popup.
   */

public class IssueCommandMenuItem extends JMenuItem{

    private final Connection connection;

    /**
     * The server command to which the selection is appended.
     */

    private final String serverCommand;


    private final String parameter;

    /**
     * Creates a new <code>IssueCommandMenuItem</code> with the specified
     * label and server command.
     */

    public IssueCommandMenuItem(Connection connection, String label, String serverCommand, String parameter){
      super(label);

      this.connection = connection; 
      this.serverCommand = serverCommand;
      this.parameter = parameter; 
    }



    /**
     * Sends the command to the server.
     */

    protected void fireActionPerformed(ActionEvent evt){
      connection.sendCommand(new Command(serverCommand + " " + parameter, 0).getCommandString());
    }


  }
