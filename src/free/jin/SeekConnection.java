/**
 * Jin - a chess client for internet chess servers.
 * More information is available at http://www.jinchess.com/.
 * Copyright (C) 2004 Alexander Maryanovsky.
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

package free.jin;


import free.jin.event.SeekListenerManager;
import free.jin.Seek;


/**
 * An extension of the <code>Connection</code> interface which adds support for
 * seeking functionality.
 */

public interface SeekConnection extends Connection{



  /**
   * Returns the SeekListenerManager via which you can register and
   * unregister SeekListeners.
   */

  SeekListenerManager getSeekListenerManager();


  /**
   * Accepts the given seek. Note that the given seek must be an instance
   * generated by this SeekConnection and it must be in the current sought
   * list.
   */

  void acceptSeek(Seek seek);
  
  
  
  /**
   * Issues the specified UserSeek.
   */
   
  void issueSeek(UserSeek seek);
  
  

}