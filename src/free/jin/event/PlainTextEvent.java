/**
 * Jin - a chess client for internet chess servers.
 * More information is available at http://www.jinchess.com/.
 * Copyright (C) 2002 Alexander Maryanovsky.
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

package free.jin.event;

import free.jin.event.JinEvent;
import free.jin.Connection;

/**
 * The event sent when text that couldn't be identified as some known type
 * arrives from the server.
 */

public class PlainTextEvent extends JinEvent{

  
  /**
   * The text that arrived from the server.
   */

  private final String text;



  /**
   * Creates a new PlainTextEvent with the given text.
   */

  public PlainTextEvent(Connection conn, String text){
    super(conn);
    this.text = text;
  }


  
  /**
   * Returns the text.
   */

  public String getText(){
    return text;
  }

}
