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

package free.jin.seek;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Hashtable;

import javax.swing.JComponent;

import free.chess.Chess;
import free.chess.Player;
import free.chess.WildVariant;
import free.jin.I18n;
import free.jin.Preferences;
import free.jin.Seek;
import free.jin.plugin.Plugin;
import free.jin.seek.event.SeekSelectionEvent;
import free.jin.seek.event.SeekSelectionListener;
import free.util.GraphicsUtilities;
import free.util.ImageUtilities;


/**
 * The sought graph component.
 */

public class SoughtGraph extends JComponent{



  /**
   * The amount of slots dedicated to bullet.
   */

  protected static final int BULLET_SLOTS = 7;




  /**
   * The amount of slots dedicated to blitz.
   */

  protected static final int BLITZ_SLOTS = 28;




  /**
   * The amount of slots dedicated to standard.
   */

  protected static final int STANDARD_SLOTS = 15;




  /**
   * The total amount of rating (vertical) slots.
   */

  protected static final int RATING_SLOTS = 32;




  /**
   * The percentage of the width dedicated to the graph itself.
   */

  protected static final double GRAPH_WIDTH_PERCENTAGE = 15.0/16.0;




  /**
   * The percentage of the height dedicated to the graph itself.
   */

  protected static final double GRAPH_HEIGHT_PERCENTAGE = 9.0/10.0;




  /**
   * The Plugin this SoughtGraph is used by.
   */

  protected final Plugin plugin;
  
  
  
  /**
   * The size of the smallest seek image. 
   */
  
  private final int minSeekImageSize;




  /**
   * Maps Seeks to Point objects indicating their locations on the graph.
   */

  protected final Hashtable seeksToLocations = new Hashtable();



  /**
   * A matrix of seeks which maps locations to seeks in those locations.
   */

  protected final Seek [][] seekMatrix = new Seek[BULLET_SLOTS+BLITZ_SLOTS+STANDARD_SLOTS][RATING_SLOTS];




  /**
   * The seek currently under the mouse cursor.
   */

  protected Seek curSeek = null;



  /**
   * The current location of the mouse.
   */

  protected Point curMouseLocation = null;




  /**
   * The background image.
   */

  private final Image bgImage;
  
  
  
  /**
   * The name of the fast games category. 
   */
  
  private final String fastCategoryName;
  
  
  
  /**
   * The name of the moderate speed games category.
   */
  
  private final String moderateCategoryName;
  
  
  
  /**
   * The name of the slow games category.
   */
  
  private final String slowCategoryName;



  /**
   * An array whose indices specify the size of the seek images and whose values
   * are Hashtables mapping seek type keys (Strings) to cached Images. The seek
   * type keys are in the following format:
   * "seek-image.rated/unrated.computer/human.variant"
   */

  private final Hashtable [] seekImageCache;




  /**
   * Creates a new SoughtGraph with the give user Plugin.
   */

  public SoughtGraph(Plugin plugin){
    this.plugin = plugin;

    Preferences prefs = plugin.getPrefs();
    I18n i18n = plugin.getI18n();

    setOpaque(true);
    setBackground(prefs.getColor("background-color", Color.white));
    setFont(new Font("SansSerif", Font.PLAIN, 10));

    String bgImageFilename = prefs.getString("background.image", null);
    Image bgImage = bgImageFilename == null ? null :
      getToolkit().getImage(SoughtGraph.class.getResource("background.gif"));
    if (bgImage != null){
      if (ImageUtilities.preload(bgImage) != ImageUtilities.COMPLETE)
        bgImage = null;
    }

    this.bgImage = bgImage;
    
    fastCategoryName = i18n.getString(prefs.getString("fastCategory.nameKey"));
    moderateCategoryName = i18n.getString(prefs.getString("moderateCategory.nameKey"));
    slowCategoryName = i18n.getString(prefs.getString("slowCategory.nameKey"));

    int [] seekImageSizes = prefs.getIntList("seek-image.sizes");
    int maxSize = 0, minSize = Integer.MAX_VALUE;
    for (int i = 0; i < seekImageSizes.length; i++){
      if (seekImageSizes[i] > maxSize)
        maxSize = seekImageSizes[i];
      if (seekImageSizes[i] < minSize)
        minSize = seekImageSizes[i];
    }
    
    minSeekImageSize = minSize;

    seekImageCache = new Hashtable[maxSize+1];
    for (int i = 0; i < seekImageSizes.length; i++)
      seekImageCache[seekImageSizes[i]] = new Hashtable(10);

    enableEvents(MouseEvent.MOUSE_MOTION_EVENT_MASK|MouseEvent.MOUSE_EVENT_MASK);
  }




  /**
   * This method calculates the desired location of the given seek. Note that the
   * seek might end up elsewhere because that location is already taken.
   */

  protected Point mapSeek(Seek seek){
    int etimeM3 = 3*seek.getTime()/(60*1000)+2*seek.getInc()/1000;
    int rating = seek.isSeekerRated() ? seek.getSeekerRating() : 0;

    int x,y;

    
    if (etimeM3<9) // Bullet
      x = (etimeM3-2)*BULLET_SLOTS/7; // 7 is the amount of possible etime values for bullet.
    else if (etimeM3<45) // Blitz
      x = BULLET_SLOTS+(etimeM3-7-2)*BLITZ_SLOTS/36; // 36 is the amount of possible etime values for blitz.
    else // Standard
      x = (int)(BULLET_SLOTS+BLITZ_SLOTS+STANDARD_SLOTS*(1-Math.pow(1.1,-((etimeM3-7-36-2)/10.0))));
        // The formula is just something exponential that looks good.

    if (rating==0)
      y = 0;
    else if (rating<1000)
      y = 1+rating/200;
    else if (rating<1500)
      y = 6+(rating-1000)/50;
    else if (rating<2000)
      y = 16+(rating-1500)/50;
    else if (rating<3000)
      y = 26+(rating-2000)/200;
    else
      y = 31;

    return new Point(x,y);
  }




  /**
   * This method returns the actual location where the given seek will be put
   * depending on the given desired location. This method always returns the
   * location of an empty slot.
   */

  protected Point fitSeek(Seek seek, Point desiredSlot){
    if (seekMatrix[desiredSlot.x][desiredSlot.y]==null)
      return desiredSlot;

    int etimeM3 = 3*seek.getTime()/(60*1000)+2*seek.getInc()/1000;
    boolean isBullet = etimeM3<9;
    boolean isBlitz = (!isBullet)&&(etimeM3<45);
    boolean isStandard = (etimeM3>=45);

    int x = desiredSlot.x;
    int y = desiredSlot.y;

    int direction = 0;
    int spiralLength = 1;
    while(true){
      for (int i=0;i<2;i++){
        for (int j=0;j<spiralLength;j++){
          switch(direction){
            case 0: x++; break; // Right
            case 1: y++; break; // Up
            case 2: x--; break; // Left
            case 3: y--; break; // Down 
          }

          if ((x<0)||(x>=seekMatrix.length)||(y<0)||(y>=seekMatrix[0].length))
            continue;
          
          if (isBullet&&(x>=BULLET_SLOTS)) // Don't put a bullet seek on a non-bullet slot.
            continue; 

          if (isBlitz&&((x<BULLET_SLOTS)||(x>=BULLET_SLOTS+BLITZ_SLOTS))) // Same for blitz.
            continue; 

          if (isStandard&&(x<BULLET_SLOTS+BLITZ_SLOTS)) // And same for standard.
            continue;

          if (seekMatrix[x][y]==null)
            return new Point(x,y);
        }
        direction++;
        if (direction==4)
          direction = 0;
      }
      spiralLength++;
    }
  }




  /**
   * Adds the given Seek to this SoughtGraph.
   */

  public void addSeek(Seek seek){
    Point desiredSlot = mapSeek(seek);
    Point actualSlot = fitSeek(seek, desiredSlot);

    seeksToLocations.put(seek, actualSlot);
    seekMatrix[actualSlot.x][actualSlot.y] = seek;

    Rectangle seekBounds = getSeekBounds(actualSlot.x, actualSlot.y, null);
    repaint(seekBounds.x - 2, seekBounds.y - 2, seekBounds.width + 4, seekBounds.height + 4);

    if (curMouseLocation!=null)
      updateCurrentSeek(curMouseLocation.x, curMouseLocation.y);
  }




  /**
   * Removes the given Seek from this SoughtGraph.
   */

  public void removeSeek(Seek seek){
    Point location = (Point)seeksToLocations.remove(seek);
    if (location == null)
      throw new IllegalArgumentException("Unknown seek: " + seek.getID());

    seekMatrix[location.x][location.y] = null;

    Rectangle seekBounds = getSeekBounds(location.x, location.y, null);
    repaint(seekBounds.x - 2, seekBounds.y - 2, seekBounds.width + 4, seekBounds.height + 4);

    if ((seek == curSeek) && (curMouseLocation != null)) // The !=null check is just in case.
      updateCurrentSeek(curMouseLocation.x, curMouseLocation.y);
  }





  /**
   * Removes all the seeks.
   */

  public void removeAllSeeks(){
    seeksToLocations.clear();

    for (int i = 0; i < seekMatrix.length; i++)
      for (int j = 0; j < seekMatrix[i].length; j++)
        seekMatrix[i][j] = null;
    
    repaint();
  }




  /**
   * Returns the bounding rectangle of the seek at the given location in the
   * seek matrix.
   */

  protected Rectangle getSeekBounds(int x, int y, Rectangle rect){
    if (rect == null)
      rect = new Rectangle();

    int width = getWidth();
    int height = getHeight();

    int graphX = (int)(width*(1-GRAPH_WIDTH_PERCENTAGE))+1;
    int graphY = 0;
    int graphWidth = (int)(width*GRAPH_WIDTH_PERCENTAGE);
    int graphHeight = (int)(height*GRAPH_HEIGHT_PERCENTAGE);

    double slotWidth = ((double)graphWidth)/(BULLET_SLOTS+BLITZ_SLOTS+STANDARD_SLOTS);
    double slotHeight = ((double)graphHeight)/RATING_SLOTS;

    rect.x = (int)(graphX+x*slotWidth);
    rect.y = (int)(graphY+graphHeight-(y+1)*slotHeight);
    rect.width = (int)slotWidth;
    rect.height = (int)slotHeight;

    return rect;
  }
  
  
  
  /**
   * Cached fonts.
   */
   
  private Font timeStringFont = null;
  private FontMetrics timeStringFontMetrics = null;
  private Font ratingStringFont = null;
  private Font seekFont = null;
  private FontMetrics seekFontMetrics = null;
  
  
  
  /**
   * The size at which the above sizes were cached, null if none.
   */
   
  private Dimension fontsCachedAtSize = null;
  
  

  /**
   * Paints this SoughtGraph on the given Graphics.
   */

  public void paintComponent(Graphics g){
    Rectangle clipRect = g.getClipBounds();

    int width = getWidth();
    int height = getHeight();
    
    if ((fontsCachedAtSize == null) || (width != fontsCachedAtSize.width) ||
        (height != fontsCachedAtSize.height)){
      timeStringFont = null;
      timeStringFontMetrics = null;
      ratingStringFont = null;
      seekFont = null;
      seekFontMetrics = null;
      fontsCachedAtSize = new Dimension(width, height);
    }
    

    Color bg = getBackground();
    Color fg = getForeground();
    Color lightFG = new Color((bg.getRed()+fg.getRed())/2, (bg.getGreen()+fg.getGreen())/2, (bg.getBlue()+fg.getBlue())/2);

    g.setColor(bg);
    g.fillRect(clipRect.x, clipRect.y, clipRect.width, clipRect.height);

    if (bgImage != null){
      Rectangle imageRect = new Rectangle(0, 0, bgImage.getWidth(null), bgImage.getHeight(null));
      while (imageRect.x < width){
        imageRect.y = 0;
        while (imageRect.y < height){
          if (imageRect.intersects(clipRect))
            g.drawImage(bgImage, imageRect.x, imageRect.y, this);
          imageRect.y += imageRect.height;
        }
        imageRect.x += imageRect.width;
      }
    }

    int graphX = (int)(width*(1-GRAPH_WIDTH_PERCENTAGE));
    int graphY = 0;
    int graphWidth = (int)(width*GRAPH_WIDTH_PERCENTAGE);
    int graphHeight = (int)(height*GRAPH_HEIGHT_PERCENTAGE);

    double slotWidth = ((double)graphWidth)/(BULLET_SLOTS+BLITZ_SLOTS+STANDARD_SLOTS);
    double slotHeight = ((double)graphHeight)/RATING_SLOTS;

    int bulletWidth = (int)(slotWidth*BULLET_SLOTS);
    int blitzWidth = (int)(slotWidth*BLITZ_SLOTS);
    int standardWidth = (int)(slotWidth*STANDARD_SLOTS);

    // The axises
    g.setColor(fg);
    g.drawLine(graphX,graphY+graphHeight,graphX+graphWidth,graphY+graphHeight);
    g.drawLine(graphX,graphY,graphX,graphY+graphHeight);
    // The axises' arrows
    g.fillPolygon(new int[]{graphX+5,graphX-5,graphX},new int[]{graphY+9,graphY+9,graphY},3);
    g.fillPolygon(new int[]{graphX+graphWidth,graphX+graphWidth-9,graphX+graphWidth-9},new int[]{graphY+graphHeight,graphY+graphHeight-5,graphY+graphHeight+5},3);


    g.setColor(lightFG);
    
    // The line separating bullet from blitz.
    int bbX = (int)(graphX+BULLET_SLOTS*slotWidth);
    g.drawLine(bbX, graphY, bbX, graphY+graphHeight);

    // The line separating blitz from standard.
    int bsX = (int)(graphX+(BULLET_SLOTS+BLITZ_SLOTS)*slotWidth);
    g.drawLine(bsX, graphY, bsX, graphY+graphHeight);


    g.setColor(fg);
    Font originalFont = g.getFont();

    // The speed category names    
    int timeStringHeight = (height-(graphY+graphHeight))/2;
    
    if (timeStringFont == null){
      int bulletFontSize = GraphicsUtilities.getMaxFittingFontSize(g, originalFont, fastCategoryName, bulletWidth, timeStringHeight);
      int blitzFontSize = GraphicsUtilities.getMaxFittingFontSize(g, originalFont, moderateCategoryName, blitzWidth, timeStringHeight);
      int standardFontSize = GraphicsUtilities.getMaxFittingFontSize(g, originalFont, slowCategoryName, standardWidth, timeStringHeight);
      int fontSize = Math.min(Math.min(bulletFontSize, blitzFontSize), standardFontSize);
      timeStringFont = new Font(originalFont.getName(), originalFont.getStyle(), fontSize);
      timeStringFontMetrics = g.getFontMetrics(timeStringFont);
    }
    
    int bulletStringWidth = timeStringFontMetrics.stringWidth(fastCategoryName);
    int blitzStringWidth = timeStringFontMetrics.stringWidth(moderateCategoryName);
    int standardStringWidth = timeStringFontMetrics.stringWidth(slowCategoryName);
    g.setFont(timeStringFont);
    int timeStringY = graphY+graphHeight+timeStringFontMetrics.getMaxAscent()+1;
    g.drawString(fastCategoryName, graphX+(bulletWidth-bulletStringWidth)/2, timeStringY);
    g.drawString(moderateCategoryName, graphX+bulletWidth+(blitzWidth-blitzStringWidth)/2, timeStringY);
    g.drawString(slowCategoryName, graphX+bulletWidth+blitzWidth+(standardWidth-standardStringWidth)/2, timeStringY);

    // The "1000", "1500" and "2000" strings.
    String tenString = "1000";
    String fifteenString = "1500";
    String twentyString = "2000";
    int ratingStringHeight = graphHeight/3; // This is obviously wrong, but we know for sure the height isn't going to limit us.
    if (ratingStringFont == null){
      int tenFontSize = GraphicsUtilities.getMaxFittingFontSize(g, originalFont, tenString, graphX-4, ratingStringHeight);
      int fifteenFontSize = GraphicsUtilities.getMaxFittingFontSize(g, originalFont, fifteenString, graphX-4, ratingStringHeight);
      int twentyFontSize = GraphicsUtilities.getMaxFittingFontSize(g, originalFont, twentyString, graphX-4, ratingStringHeight);
      int ratingStringFontSize = Math.min(Math.min(tenFontSize, fifteenFontSize), twentyFontSize);
      ratingStringFont = new Font(originalFont.getName(), originalFont.getStyle(), ratingStringFontSize);
    }
    g.setFont(ratingStringFont);
    int ratingStringX = 1;
    g.drawString(tenString, ratingStringX, (int)(graphY+graphHeight-6*slotHeight));
    g.drawString(fifteenString, ratingStringX, (int)(graphY+graphHeight-16*slotHeight));
    g.drawString(twentyString, ratingStringX, (int)(graphY+graphHeight-26*slotHeight));

    // The current seek description string.
    if (curSeek != null){
      String seekString = getSeekString(curSeek);
      int seekStringHeight = (height-(graphY+graphHeight))/2;
      
      // Ideally, when caching the font size here, we should also take into account
      // the string for which we're caching the size, but the limiting factor for
      // the font size is practically always the height, so the string doesn't really
      // matter.
      if (seekFont == null){
        int seekFontSize = GraphicsUtilities.getMaxFittingFontSize(g, originalFont, seekString, width, seekStringHeight);
        seekFont = new Font(originalFont.getName(), originalFont.getStyle(), seekFontSize);
        seekFontMetrics = g.getFontMetrics(seekFont);
      }
      
      g.setFont(seekFont);
      g.drawString(seekString, 1, height-seekStringHeight+seekFontMetrics.getMaxAscent());
    }


    // The seeks.    
    Rectangle seekBounds = new Rectangle();
    for (int i=0;i<seekMatrix.length;i++){
      for (int j=0;j<seekMatrix[i].length;j++){
        Seek seek = seekMatrix[i][j];
        if (seek==null)
          continue;

        seekBounds = getSeekBounds(i, j, seekBounds);
        if (seekBounds.intersects(clipRect)){
//          g.setClip(clipRect);
          drawSeek(g, seek, seekBounds);
        }
      }
    }
  }





  /**
   * Returns the Image that should be drawn for the specified Seek at the
   * specified size.
   */

  private Image getSeekImage(Rectangle seekBounds, Seek seek){
    int size = Math.min(seekBounds.width, seekBounds.height);
    if (size <= 0)
      throw new IllegalArgumentException("Seek bounds size must be positive");

    int index = -1;

    if (size >= seekImageCache.length)
      index = seekImageCache.length-1;
    else if (seekImageCache[size] != null)
      index = size;

    if (index == -1){
      for (int i = size; i > 0; i--)
        if (seekImageCache[i] != null){
          index = i;
          break;
        }
    }

    if (index == -1){
      for (int i = size+1; i < seekImageCache.length; i++)
        if (seekImageCache[i] != null){
          index = i;
          break;
        }
    }

    if (index == -1)
      throw new IllegalStateException("Couldn't find suitable seek images");

    Hashtable sizeImages = seekImageCache[index];

    String playerType = seek.isSeekerComputer() ? "computer" : "human";
    String ratedString = seek.isRated() ? "rated" : "unrated";
    String imageKey = "seek-image."+ratedString+"."+playerType+"."+seek.getVariant();

    Image image = (Image)sizeImages.get(imageKey);
    if (image == null){
      String imageName = (String)plugin.getPrefs().lookup(imageKey);
      if (imageName == null)
        return null;

      String imageFile = "images/"+index+"/"+imageName;
      image = getToolkit().getImage(SoughtGraph.class.getResource(imageFile));
      if (ImageUtilities.preload(image) != ImageUtilities.COMPLETE)
        return null;
      sizeImages.put(imageKey, image);
    }

    return image;
  }


  /**
   * Draws a single seek within the given bounds.
   */

  protected void drawSeek(Graphics g, Seek seek, Rectangle seekBounds){
    Image seekImage = getSeekImage(seekBounds, seek);

    if (seekImage == null){
      g.setColor(Color.black);
      g.drawOval(seekBounds.x, seekBounds.y, seekBounds.width-1, seekBounds.height-1);
      return;
    }

    g.drawImage(seekImage, seekBounds.x, seekBounds.y, null);
  }




  /**
   * Returns a string representing the given seek.
   */

  protected String getSeekString(Seek seek){
// <name><titles> <rating> <(provisional)> seeks <time> <inc> [isRated] [wild] [color] [minrating]-[maxrating] [manual] [formula]
    
    I18n i18n = I18n.get(SoughtGraph.class);
    String provisional = i18n.getString("provisional");
    String rated = i18n.getString("rated");
    String unrated = i18n.getString("unrated");
    String white = i18n.getString("white");
    String black = i18n.getString("black");
    String manualAcceptIndicator = i18n.getString("manualAcceptIndicator");
    String formulaIndicator = i18n.getString("formulaIndicator");

    String name = seek.getSeekerName();
    String title = seek.getSeekerTitle();
    int rating = seek.getSeekerRating();
    String ratingString = seek.isSeekerRated() ? (" "+rating) : "";
    boolean isProvisional = seek.isSeekerProvisional()&&seek.isSeekerRegistered();
    int time = seek.getTime()/(60*1000);
    int inc = seek.getInc()/1000;
    boolean isRated = seek.isRated();
    WildVariant variant = seek.getVariant();
    Player color = seek.getSoughtColor();
    boolean isRatingLimited = seek.isRatingLimited();
    boolean isManualAccept = seek.isManualAccept();
    boolean isFormula = seek.isFormula();
    
    String seekString = name + title + ratingString + (isProvisional ? " (" + provisional+") " : " ") +
      time + " " + inc + " " + (isRated ? rated : unrated) + " ";

    if (!(variant instanceof Chess))
      seekString = seekString + variant.getName() + " ";

    if (color!=null)
      seekString = seekString + (color.isWhite() ? white : black) + " ";

    if (isRatingLimited){
      int minRating = seek.getMinRating();
      int maxRating = seek.getMaxRating();
      seekString = seekString+minRating + "-" + maxRating + " ";
    }

    if (isManualAccept)
      seekString = seekString + manualAcceptIndicator + " ";

    if (isFormula)
      seekString = seekString + formulaIndicator + " ";

    return seekString;
  }




  /**
   * Returns the seek at the given pixel coordinates, or null if none.
   */

  protected Seek seekAtLocation(int x, int y){
    int width = getWidth();
    int height = getHeight();

    int graphX = (int)(width*(1-GRAPH_WIDTH_PERCENTAGE));
    int graphY = 0;
    int graphWidth = (int)(width*GRAPH_WIDTH_PERCENTAGE);
    int graphHeight = (int)(height*GRAPH_HEIGHT_PERCENTAGE);

    double slotWidth = ((double)graphWidth)/(BULLET_SLOTS+BLITZ_SLOTS+STANDARD_SLOTS);
    double slotHeight = ((double)graphHeight)/RATING_SLOTS;

    if (x < graphX)
      return null;
    if (graphY + graphHeight < y)
      return null;

    int i = (int)((x-graphX)/slotWidth);
    int j = (int)((graphY+graphHeight-y)/slotHeight);

    if ((i < 0) || (i >= seekMatrix.length) || (j < 0) || (j >= seekMatrix[i].length))
      return null;

    return seekMatrix[i][j];
  }





  /**
   * Possibly changes the current seek under mouse and repaints if necessary.
   * The given coordinates should be the coordinates of the mouse.
   */

  protected void updateCurrentSeek(int x, int y){
    Seek newSeek = seekAtLocation(x, y);
    if (newSeek != curSeek){
      curSeek = newSeek;
      repaint(0, (int)(getHeight()*GRAPH_HEIGHT_PERCENTAGE), getWidth(), (int)(getHeight()*(1-GRAPH_HEIGHT_PERCENTAGE)));
      
      if (newSeek != null)
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      else
        setCursor(Cursor.getDefaultCursor());
    }
  }





  
  /**
   * Changes the current seek under mouse and repaints if necessary.
   */

  protected void processMouseMotionEvent(MouseEvent evt){
    super.processMouseMotionEvent(evt);

    if (evt.getID() == MouseEvent.MOUSE_MOVED){
      updateCurrentSeek(evt.getX(), evt.getY());
      curMouseLocation = evt.getPoint();
    }
  }




  /**
   * If a seek is clicked, fires a SeekSelectionEvent.
   * Also, if the mouse leaves the component, nulls the current seek and repaints.
   */

  protected void processMouseEvent(MouseEvent evt){
    super.processMouseEvent(evt);

    if (evt.getID() == MouseEvent.MOUSE_EXITED){
      curSeek = null;
      curMouseLocation = null;
      repaint(0, (int)(getHeight()*GRAPH_HEIGHT_PERCENTAGE), getWidth(), (int)(getHeight()*(1-GRAPH_HEIGHT_PERCENTAGE)));
    }
    if (evt.getID() == MouseEvent.MOUSE_ENTERED){
      curMouseLocation = evt.getPoint();
      setCursor(Cursor.getDefaultCursor());
    }
    if ((evt.getID() == MouseEvent.MOUSE_CLICKED) && (evt.getModifiers() == MouseEvent.BUTTON1_MASK)){
      Seek pressedSeek = seekAtLocation(evt.getX(), evt.getY());
      if (pressedSeek!=null)
        fireSeekSelectionEvent(new SeekSelectionEvent(this, pressedSeek));
    }
  }





  /**
   * Adds the given SeekSelectionListener to the list of listeners receiving
   * notifications when the user selects seeks.
   */

  public void addSeekSelectionListener(SeekSelectionListener listener){
    listenerList.add(SeekSelectionListener.class, listener);
  }




  /**
   * Removes the given SeekSelectionListener from the list of listeners receiving
   * notifications when the user selects seeks.
   */

  public void removeSeekSelectionListener(SeekSelectionListener listener){
    listenerList.remove(SeekSelectionListener.class, listener);
  }



  
  /**
   * Fires the given SeekSelectionEvent to all interested SeekSelectionListeners.
   */

  protected void fireSeekSelectionEvent(SeekSelectionEvent evt){
    Object [] listenerList = this.listenerList.getListenerList();
    for (int i=0;i<listenerList.length;i+=2){
      if (listenerList[i]==SeekSelectionListener.class){
        SeekSelectionListener listener = (SeekSelectionListener)listenerList[i+1];
        listener.seekSelected(evt);
      }
    }
  }
  
  
  
  /**
   * Returns the minimum size of the sought graph.
   */
 
  public Dimension getMinimumSize(){
    int width = (int)(minSeekImageSize * (BULLET_SLOTS + BLITZ_SLOTS + STANDARD_SLOTS) / GRAPH_WIDTH_PERCENTAGE);
    int height = (int)(minSeekImageSize * RATING_SLOTS / GRAPH_HEIGHT_PERCENTAGE);
    return new Dimension(width, height);
  }
  
  

}
