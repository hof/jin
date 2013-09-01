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

package free.jin.gamelogger;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import bsh.EvalError;
import bsh.Interpreter;
import free.chess.*;
import free.jin.Connection;
import free.jin.Game;
import free.jin.PGNConnection;
import free.jin.Preferences;
import free.jin.event.*;
import free.jin.plugin.Plugin;
import free.jin.plugin.PluginContext;
import free.jin.ui.OptionPanel;
import free.jin.ui.PreferencesPanel;



/**
 * A plugin which allows logging games.
 */

public class GameLogger extends Plugin implements GameListener, PropertyChangeListener{



  /**
   * The code for the logging mode when no games are logged.
   */

  public static final int LOG_NONE = 0;



  /**
   * The code for the logging mode when all games are logged into one file.
   */

  public static final int LOG_ALL = 1;



  /**
   * The code for the logging mode when logging rules apply.
   */

  public static final int USE_RULES = 2;



  /**
   * The DateFormat used for formatting the Date pgn tag.
   */

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd");




  /**
   * The DateFormat used for formatting the Time pgn tag.
   */

  private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");




  /**
   * Maps Game objects to GameInfo objects.
   */

  private final Hashtable gamesToGameInfo = new Hashtable();




  /**
   * The current logging mode.
   */

  private int loggingMode;




  /**
   * The filename of the file to log to when the logging setting is to log all
   * games.
   */

  private String allGamesLogFile;




  /**
   * A Vector of LoggingRules.
   */

  private Vector loggingRules;
  



  /**
   * Sets the plugin context - returns <code>false</code> if the connection is
   * not an instance of <code>PGNConnection</code>.
   */

  public boolean setContext(PluginContext context){
    if (!(context.getConnection() instanceof PGNConnection))
      return false;

    return super.setContext(context);
  }




  /**
   * Returns the current logging mode. Possible values are
   * <code>LOG_NONE</code>, <code>LOG_ALL</code> and <code>USE_RULES</code>.
   */

  public int getLoggingMode(){
    return loggingMode;
  }




  /**
   * Returns the name of the file into which the games are saved under the
   * <code>LOG_ALL</code> logging mode. This may return <code>null</code> if the
   * user never specified such a file.
   */

  public String getLogFileForAll(){
    return allGamesLogFile;
  }




  /**
   * Returns a deep copy of the <code>loggingRules</code> Vector.
   */

  public Vector getLoggingRules(){
    Vector rules = new Vector(loggingRules.size());
    for (int i = 0; i < loggingRules.size(); i++){
      LoggingRule rule = (LoggingRule)loggingRules.elementAt(i);
      rules.addElement(new LoggingRule(rule));
    }

    return rules;
  }




  /**
   * Initializes the plugin.
   */

  public void start(){
    registerListeners();
    loadLoggingConditions();
  }




  /**
   * Stops the plugin.
   */

  public void stop(){
    unregisterListeners();
  }




  
  /**
   * Registers all the necessary listeners.
   */

  protected void registerListeners(){
    Connection conn = getConn();
    ListenerManager listenerManager = conn.getListenerManager();

    listenerManager.addGameListener(this);
  }





  /**
   * Unregisters all the listeners registered by <code>registerListeners()</code>.
   */

  protected void unregisterListeners(){
    Connection conn = getConn();
    ListenerManager listenerManager = conn.getListenerManager();

    listenerManager.removeGameListener(this);
  }




  /**
   * Loads the logging mode and logging rules.
   */

  private void loadLoggingConditions(){
    Preferences prefs = getPrefs();

    String loggingModeString = prefs.getString("logging.mode", "none");
    if ("rules".equalsIgnoreCase(loggingModeString))
      loggingMode = USE_RULES;
    else if ("all".equalsIgnoreCase(loggingModeString))
      loggingMode = LOG_ALL;
    else
      loggingMode = LOG_NONE;

    allGamesLogFile = prefs.getString("logging.all.filename", null);
    if ((allGamesLogFile == null) && (loggingMode == LOG_ALL))
      loggingMode = LOG_NONE;
      

    int rulesCount = prefs.getInt("logging.rules.count", 0);
    loggingRules = new Vector(rulesCount);

    for (int i = 0; i < rulesCount; i++){
      String name = prefs.getString("logging.rule-"+(i+1)+".name");
      String condition = prefs.getString("logging.rule-"+(i+1)+".condition");
      String filename = prefs.getString("logging.rule-"+(i+1)+".filename");

      try{
        loggingRules.addElement(new LoggingRule(name, condition, filename));
      } catch (EvalError e){
          e.printStackTrace();
        }
    }
  }




  /**
   * Rereads all the user/plugin preferences. This method should be called when
   * the user changes his preferences.
   */

  public void refreshFromProperties(){
    loadLoggingConditions();
  }





  /**
   * Returns an array of all the variables available to be used in the logging
   * rule condition and their possible values. Each element in the returned
   * array is an array by itself, of length 2 where the first element is the
   * variable name and the 2nd is its possible value, as a string.
   */

  static String [][] getAvailableVars(){
    return new String[][]{
      {"category", "\"Blitz\""},
      {"rating", "1800"},
      {"time", "10"},
      {"inc", "2"},
      {"etime", "2.5"},
      {"rated", "true"},
      {"opponent", "\"AlexTheGreat\""},
      {"title", "\"gm\""},
      {"moves", "40"},
      {"userWhite", "true"},
      {"userBlack", "false"},
      {"result", "\"win\""},
      {"win", "true"},
      {"loss", "false"},
      {"draw", "false"},
      {"unknownResult", "false"},
      {"whiteWins", "true"},
      {"blackWins", "true"},
    };
  }




  /**
   * Returns an array of names of the files into which the specified game should
   * be logged. Returns <code>null</code> if the specified game should not be
   * logged at all.
   */

  private String [] getFilesToLogInto(Game game){
    if (loggingMode == LOG_NONE)
      return null;
    else if (loggingMode == LOG_ALL){
      return new String[]{allGamesLogFile};
    }
    else{
      GameInfo gameInfo = (GameInfo)gamesToGameInfo.get(game);

      Interpreter bsh = new Interpreter();
      boolean isUserWhite = game.getUserPlayer().isWhite();
      try{
        bsh.set("category", game.getRatingCategoryString());
        bsh.set("rating", isUserWhite ? game.getBlackRating() : game.getWhiteRating());
        bsh.set("time", (isUserWhite ? game.getWhiteTime() : game.getBlackTime())/(1000*60));
        bsh.set("inc", isUserWhite ? game.getWhiteInc() : game.getBlackInc());
        bsh.set("etime", isUserWhite ? (game.getWhiteTime() + 2*game.getWhiteInc()/3) : 
                                       (game.getBlackTime() + 2*game.getBlackInc()/3));
        bsh.set("rated", game.isRated());
        bsh.set("opponent", (isUserWhite ? game.getBlackName() : game.getWhiteName()));
        bsh.set("title", (isUserWhite ? game.getBlackTitles() : game.getWhiteTitles()));
        bsh.set("moves", gameInfo.movelist.size());
        
        bsh.set("userWhite", game.getUserPlayer() == Player.WHITE_PLAYER);
        bsh.set("userBlack", game.getUserPlayer() == Player.BLACK_PLAYER);
        
        String result = getResultString(isUserWhite, game.getResult());
        bsh.set("result", result);
        bsh.set("win", result.equals("win"));
        bsh.set("loss", result.equals("loss"));
        bsh.set("draw", result.equals("draw"));
        bsh.set("unknownResult", result.equals("unknownResult"));
        
        bsh.set("whiteWins", game.getResult() == Game.WHITE_WINS);
        bsh.set("blackWins", game.getResult() == Game.BLACK_WINS);
      } catch (EvalError e){
        e.printStackTrace();
        return new String[0];
      }

      Vector files = new Vector();
      for (int i = 0; i < loggingRules.size(); i++){
        LoggingRule rule = (LoggingRule)loggingRules.elementAt(i);
        String condition = rule.getCondition();
        try{
          boolean result = ((Boolean)bsh.eval(condition)).booleanValue();
          if (result)
            files.addElement(rule.getFilename());
        } catch (EvalError e){
          e.printStackTrace();
        }
      }

      if (files.size() == 0)
        return null;

      String [] filenames = new String[files.size()];
      for (int i = 0; i < filenames.length; i++)
        filenames[i] = (String)files.elementAt(i);

      return filenames;
    }
  }




  /**
   * Returns the result string for the specified result code and a boolean
   * value specifying whether the user is playing with the white pieces.
   */

  private static final String getResultString(boolean isUserWhite, int result){
    switch (result){
      case Game.UNKNOWN_RESULT:
        return "unknown";
      case Game.DRAW:
        return "draw";
      case Game.WHITE_WINS:
        if (isUserWhite)
          return "win";
        else
          return "loss";
      case Game.BLACK_WINS:
        if (isUserWhite)
          return "loss";
        else
          return "win";
      default:
        throw new IllegalArgumentException("Bad result value: "+result);
    }
  }




  /**
   * Returns <code>true</code> if the specified game can be logged, that
   * is, do we know how to log it.
   */

  protected boolean canLog(Game game){
    return (game.getGameType() == Game.MY_GAME) && 
           (game.getVariant() instanceof Chess);
  }



  /**
   * Logs the specified game to all the files it should be logged into.
   */
  
  private void log(Game game){
    String [] filenames = getFilesToLogInto(game);
    if (filenames != null){
      for (int i = 0; i < filenames.length; i++)
        log(game, filenames[i]);
    }
  }
  
  
    
  /**
   * Logs the specified game into the specified file.
   */

  private void log(Game game, String filename){
    GameInfo gameInfo = (GameInfo)gamesToGameInfo.get(game);

    try{
      String resultString;
      switch (game.getResult()){
        case Game.WHITE_WINS:
          resultString = "1-0";
          break;
        case Game.BLACK_WINS:
          resultString = "0-1";
          break;
        case Game.DRAW:
          resultString = "1/2-1/2";
          break;
        default:
          resultString = "*";
          break;
      }

      int whiteRating = game.getWhiteRating();
      int blackRating = game.getBlackRating();
      String whiteRatingString = (whiteRating < 0) ? "-" : String.valueOf(whiteRating);
      String blackRatingString = (blackRating < 0) ? "-" : String.valueOf(blackRating);

      DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename, true)));
      writeTag(out, "Event", (game.isRated() ? "rated " : "unrated ") + game.getRatingCategoryString() + " game");
      writeTag(out, "Site", getUser().getServer().getLongName());
      writeTag(out, "Date", DATE_FORMAT.format(gameInfo.gameStartDate));
      writeTag(out, "Round", "-");
      writeTag(out, "White", game.getWhiteName());
      writeTag(out, "Black", game.getBlackName());
      writeTag(out, "WhiteElo", whiteRatingString);
      writeTag(out, "BlackElo", blackRatingString);
      writeTag(out, "Result", resultString);
      writeTag(out, "Time", TIME_FORMAT.format(gameInfo.gameStartDate));
      if (!game.isTimeOdds())
        writeTag(out, "TimeControl", game.getWhiteTime()/1000+"+"+game.getWhiteInc());
      writeTag(out, "Mode", "ICS");
      if (!gameInfo.initPos.getFEN().equals(Chess.INITIAL_POSITION_FEN)){
        writeTag(out, "SetUp", "1");
        writeTag(out, "FEN", gameInfo.initPos.getFEN());
      }

      out.writeBytes("\n");

      Vector movelist = gameInfo.movelist;
      int moveCount = movelist.size();
      StringBuffer lineBuf = new StringBuffer();
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < moveCount; i++){
        buf.setLength(0);
        ChessMove move = (ChessMove)movelist.elementAt(i);
        Player movingPlayer = move.getPlayer();
        String san = move.getSAN();
        if ((i == 0) && movingPlayer.isBlack()){
          buf.append("1... ");
          buf.append(san);
        }
        else{
          if (movingPlayer.isWhite()){
            buf.append(String.valueOf(1+i/2));
            buf.append(". ");
          }
          buf.append(san);
        }
        if (lineBuf.length() + 1 + buf.length() > 80){ // +1 is for the space between them
          out.writeBytes(lineBuf.toString());
          out.writeBytes("\n");
          lineBuf.setLength(0);
          lineBuf.append(buf.toString());
        }
        else{
          if (lineBuf.length() != 0)
            lineBuf.append(" ");
          lineBuf.append(buf.toString());
        }

        buf.setLength(0);
      }

      if (lineBuf.length() + 1 + resultString.length() > 80){ // +1 is for the space between them
        out.writeBytes(lineBuf.toString());
        out.writeBytes("\n");
        out.writeBytes(resultString);
      }
      else{
        out.writeBytes(lineBuf.toString());
        out.writeBytes(" ");
        out.writeBytes(resultString);
      }

      out.writeBytes("\n\n");

      out.close();
    } catch (IOException e){
        e.printStackTrace();
        OptionPanel.error("I/O Error", "Unable to log game:\n" + e.getMessage());
      }
  }




  /**
   * Writes the specified pgn tag with the specified value to the specified
   * output stream.
   */

  private static void writeTag(DataOutputStream out, String tagName, String tagValue) throws IOException{
    out.writeBytes("["+tagName+" \""+tagValue+"\"]\n");
  }



  
  /**
   * Starts logging the moves.
   */

  public void gameStarted(GameStartEvent evt){
    Game game = evt.getGame();
    Position initPos = game.getInitialPosition();
    GameInfo gameInfo = new GameInfo(initPos);
    gamesToGameInfo.put(game, gameInfo);
    
    game.addPropertyChangeListener(this);
  }




  /**
   * Saves the ended game.
   */

  public void gameEnded(GameEndEvent evt){
    Game game = evt.getGame();
    if (canLog(game) && game.isPlayed())
      log(game);
    gamesToGameInfo.remove(game);
  }
  
  
  
  
  /**
   * Observes changes in the game which we care about.
   */
  
  public void propertyChange(PropertyChangeEvent evt){
    Game game = (Game)evt.getSource();
    if (canLog(game)){
      if (evt.getPropertyName().equals("played") && !game.isPlayed()) // The game ended and became examined
        log(game);
    }
  }

  
  

  /**
   * Saves the move.
   */

  public void moveMade(MoveMadeEvent evt){
    Game game = evt.getGame();
    Move move = evt.getMove();
    GameInfo gameInfo = (GameInfo)gamesToGameInfo.get(game);
    gameInfo.movelist.addElement(move);
  }

  


  /**
   * Clears the move list.
   */

  public void positionChanged(PositionChangedEvent evt){
    Game game = evt.getGame();
    GameInfo gameInfo = (GameInfo)gamesToGameInfo.get(game);
    gameInfo.initPos = evt.getPosition();
  }



  /**
   * Removes the undone moves from the move list.
   */

  public void takebackOccurred(TakebackEvent evt){
    Game game = evt.getGame();
    GameInfo gameInfo = (GameInfo)gamesToGameInfo.get(game);
    Vector movelist = gameInfo.movelist;
    int start = movelist.size() - 1;
    int stop = Math.max(0, movelist.size() - evt.getTakebackCount());
    for (int i = start; i >= stop; i--)
      movelist.removeElementAt(i);
  }




  /**
   * GameListener implementation.
   */

  public void illegalMoveAttempted(IllegalMoveEvent evt){}
  public void clockAdjusted(ClockAdjustmentEvent evt){}
  public void boardFlipped(BoardFlipEvent evt){}
  public void offerUpdated(OfferEvent evt){}




  /**
   * Return a PreferencesPanel for changing GameLogger's settings.
   */

  public PreferencesPanel getPreferencesUI(){
    return new GameLoggerPreferencesPanel(this);
  }




  /**
   * Returns <code>true</code> that the game logger does feature a preferences
   * panel.
   */

  public boolean hasPreferencesUI(){
    return true;
  }



  /**
   * Returns the string "gamelogger".
   */

  public String getId(){
    return "gamelogger";
  }



  /**
   * Returns the plugin name.
   */

  public String getName(){
    return getI18n().getString("pluginName");
  }





  /**
   * A small class bundling information about a game.
   */

  private static class GameInfo{


    /**
     * The initial position.
     */

    public Position initPos;



    /**
     * The move list.
     */

    public Vector movelist;




    /**
     * A Date object representing the time when the game started.
     */

    public final Date gameStartDate;



    /**
     * Creates a new GameInfo with the specified initial position.
     */

    public GameInfo(Position initPos){
      this.initPos = initPos;
      movelist = new Vector();
      gameStartDate = new Date();
    }

  }

}
