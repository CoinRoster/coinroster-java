package com.coinroster;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.bots.BaseballBot;
import com.coinroster.bots.BasketballBot;
import com.coinroster.bots.GolfBot;
import com.coinroster.bots.HockeyBot;
import com.coinroster.bots.CrowdSettleBot;
import com.coinroster.internal.BackoutContest;
import com.coinroster.internal.EnterAutoplayRosters;
import com.coinroster.internal.UpdateContestStatus;

/**
 * Methods that create new contests or check the status of existing ones to perform operations.
 * 
 * @see com.coinroster.CronWorker Most, if not all, of these are called by Cron jobs
 *
 */
public class ContestMethods extends Utils {

	//------------------------------------------------------------------------------------
	
	/**
	 * Creates a new Basketball prop contest. Uses the `CONTEST_TEMPLATES` table from the database 
	 * as a format reference.
	 * 
	 * @see com.coinroster.api.CreateContest
	 * @throws Exception Can throw an error if new class instance cannot spawn
	 */
	public static void createBasketballContests() {
	
		Connection sql_connection = null;
		try {
			sql_connection = Server.sql_connection();
			DB db = new DB(sql_connection);
			BasketballBot basketball_bot = new BasketballBot(sql_connection);
			String gameID_array = basketball_bot.scrapeGameIDs();
			if(basketball_bot.getGameIDs() == null)
				return;
			log("setting up basketball contests...");
			basketball_bot.setup();
			basketball_bot.savePlayers();
	
			Long deadline = basketball_bot.getEarliestGame();
			LocalDate date = Instant.ofEpochMilli(deadline).atZone(ZoneId.systemDefault()).toLocalDate();
	
			JSONArray prop_contests = db.getRosterTemplates("BASKETBALLPROPS");
			for(int i = 0; i < prop_contests.length(); i++){
				JSONObject contest = prop_contests.getJSONObject(i);
				String title = date.toString() + " | " + contest.getString("title");
				contest.put("title", title);
				contest.put("odds_source", "n/a");
				contest.put("gameIDs", gameID_array);
				contest.put("registration_deadline", deadline);
	
				JSONObject prop_data = new JSONObject(contest.getString("prop_data"));
				JSONObject pari_mutuel_data;
				if(prop_data.getString("prop_type").equals("TEAM_SNAKE"))
					pari_mutuel_data = basketball_bot.createTeamsPariMutuel(contest, prop_data);
				else
					pari_mutuel_data = basketball_bot.createPariMutuel(deadline, date.toString(), contest);
	
				MethodInstance pari_method = new MethodInstance();
				JSONObject pari_output = new JSONObject("{\"status\":\"0\"}");
				pari_method.input = pari_mutuel_data;
				pari_method.output = pari_output;
				pari_method.session = null;
				pari_method.sql_connection = sql_connection;
				try{
					Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
					c.newInstance(pari_method);
				}
				catch(Exception e){
					log(pari_method.output.toString());
					Server.exception(e);
				}
			}
	
			//read templates from `CONTEST_TEMPLATES` table
			JSONArray roster_contests = db.getRosterTemplates("BASKETBALL");
			for(int index = 0; index < roster_contests.length(); index++){
				JSONObject contest = roster_contests.getJSONObject(index);
				String title = date.toString() + " | " + contest.getString("title");
				contest.put("title", title);
				contest.put("odds_source", "n/a");
				contest.put("gameIDs", gameID_array);
				contest.put("registration_deadline", deadline);
				ResultSet options;
				if(contest.getInt("filter") == 0){
					options = db.getOptionTable(basketball_bot.sport, false, 0, basketball_bot.getGameIDs());
				}
				else{
					options = db.getOptionTable(basketball_bot.sport, true, contest.getInt("filter"), basketball_bot.getGameIDs());
				}
	
				JSONObject prop_data = new JSONObject(contest.getString("prop_data"));
				double weight = basketball_bot.getNormalizationWeight(prop_data.getDouble("top_player_salary"), basketball_bot.getGameIDs(), contest.getInt("salary_cap"));
	
				JSONArray option_table = new JSONArray();
				while(options.next()){
					JSONObject player = new JSONObject();
					player.put("name", options.getString(2) + " " + options.getString(3));
					player.put("price", (int) Math.round(options.getDouble(4) * weight));
					player.put("count", 0);
					player.put("id", options.getString(1));
					option_table.put(player);
				}
				contest.put("option_table", option_table);
				MethodInstance method = new MethodInstance();
				JSONObject output = new JSONObject("{\"status\":\"0\"}");
				method.input = contest;
				method.output = output;
				method.session = null;
				method.sql_connection = sql_connection;
				try{
					Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
					c.newInstance(method);
					int contest_id = method.output.getInt("contest_id");
					new EnterAutoplayRosters(sql_connection, contest.getInt("contest_template_id"), contest_id);
				}
				catch(Exception e){
					e.printStackTrace();
				}	
			}	
		} catch (Exception e) {
			Server.exception(e);
		} finally {
			if (sql_connection != null) {
				try {
					sql_connection.close();
				} 
				catch (SQLException ignore) {
					// ignore
				}
			}
		}
	}	
	
	/**
	 * Check to see if contests are in play, settle if necessary.
	 * 
	 * @see com.coinroster.api.SettleContest Spawns instance of SettleContest if a contest has concluded
	 * @throws Exception Can throw an error if new class instance cannot spawn
	 * @throws SQLException
	 */
	public static void checkBasketballContests() {
		Connection sql_connection = null;
		try {
			sql_connection = Server.sql_connection();
			DB db_connection = new DB(sql_connection);
			JSONObject roster_contests = db_connection.check_if_in_play("FANTASYSPORTS", "BASKETBALL", "ROSTER");
			JSONObject pari_contests = db_connection.get_active_pari_mutuels("BASKETBALLPROPS", "PARI-MUTUEL");
	
			if(!(roster_contests.length() == 0) || !(pari_contests.length() == 0)){
				BasketballBot basketball_bot = new BasketballBot(sql_connection);
				log("Basketball games are in play and minute is multiple of 20");
				basketball_bot.scrapeGameIDs();
				ArrayList<String> gameIDs = basketball_bot.getGameIDs();
				boolean games_ended;
				games_ended = basketball_bot.scrape(gameIDs);
	
				Iterator<?> roster_contest_ids = roster_contests.keys();
				while(roster_contest_ids.hasNext()){
	
					String c_id = (String) roster_contest_ids.next();
					log("basketball contest: " + c_id);
					String scoring_rules_string = roster_contests.getString(c_id);
					JSONObject scoring_rules = new JSONObject(scoring_rules_string);
					JSONArray player_scores = basketball_bot.updateScores(scoring_rules, gameIDs);
					JSONObject fields = new JSONObject();
					fields.put("contest_id", Integer.parseInt(c_id));
					fields.put("normalization_scheme", "INTEGER");
					fields.put("player_scores", player_scores);
	
					MethodInstance method = new MethodInstance();
					JSONObject output = new JSONObject("{\"status\":\"0\"}");
					method.input = fields;
					method.output = output;
					method.session = null;
					method.sql_connection = sql_connection;
					try{
						Constructor<?> c = Class.forName("com.coinroster.api." + "UpdateScores").getConstructor(MethodInstance.class);
						c.newInstance(method);
					}
					catch(Exception e){
						e.printStackTrace();
					}
	
				}
				if(games_ended){
					log("basketball games have ended. Settling now...");
					roster_contests = db_connection.check_if_in_play("FANTASYSPORTS", "BASKETBALL", "ROSTER");
					roster_contest_ids = roster_contests.keys();
					while(roster_contest_ids.hasNext()){
						String c_id = (String) roster_contest_ids.next();
						String scoring_rules_string = roster_contests.getString(c_id);
						JSONObject scoring_rules = new JSONObject(scoring_rules_string);
						JSONArray player_scores = basketball_bot.updateScores(scoring_rules, gameIDs);
	
						JSONObject fields = new JSONObject();
						fields.put("contest_id", Integer.parseInt(c_id));
						fields.put("normalization_scheme", "INTEGER");
						fields.put("player_scores", player_scores);
	
						MethodInstance method = new MethodInstance();
						JSONObject output = new JSONObject("{\"status\":\"0\"}");
						method.input = fields;
						method.output = output;
						method.session = null;
						method.sql_connection = sql_connection;
						try{
							Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
							c.newInstance(method);
						}
						catch(Exception e){
							Server.exception(e);
						}
					}
	
					//SETTLE PARIMUTUELS FROM NIGHT'S GAMES
					pari_contests = db_connection.get_active_pari_mutuels("BASKETBALLPROPS", "PARI-MUTUEL");
					Iterator<?> pari_contest_ids = pari_contests.keys();	
					while(pari_contest_ids.hasNext()){
						String c_id = (String) pari_contest_ids.next();
	
						JSONObject scoring_rules = new JSONObject(pari_contests.getJSONObject(c_id).getString("scoring_rules"));
						JSONObject prop_data = new JSONObject(pari_contests.getJSONObject(c_id).getString("prop_data"));
						JSONArray option_table = new JSONArray(pari_contests.getJSONObject(c_id).getString("option_table"));
	
						JSONObject pari_fields = basketball_bot.settlePariMutuel(Integer.parseInt(c_id), scoring_rules, prop_data, option_table, gameIDs);
						MethodInstance pari_method = new MethodInstance();
						JSONObject pari_output = new JSONObject("{\"status\":\"0\"}");
						pari_method.input = pari_fields;
						pari_method.output = pari_output;
						pari_method.session = null;
						pari_method.sql_connection = sql_connection;
						try{
							Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
							c.newInstance(pari_method);
						}
						catch(Exception e){
							Server.exception(e);
						}		
					}
				}
			}
		} catch (Exception e) {
			Server.exception(e);
		} finally {
			if (sql_connection != null) {
				try {
					sql_connection.close();
				} 
				catch (SQLException ignore) {
					// ignore
				}
			}
		}
	}
	
	//------------------------------------------------------------------------------------
	
	/**
	 * Creates a new Hockey prop contest. Uses the `CONTEST_TEMPLATES` table from the database 
	 * as a format reference.
	 * 
	 * @see com.coinroster.api.CreateContest
	 * @throws Exception Can throw an error if new class instance cannot spawn
	 */
	public static void createHockeyContests() {
	
		Connection sql_connection = null;
		try {
			sql_connection = Server.sql_connection();
			DB db = new DB(sql_connection);
			HockeyBot hockey_bot = new HockeyBot(sql_connection);
			String gameID_array = hockey_bot.scrapeGameIDs();
			if(hockey_bot.getGameIDs() == null)
				return;
			log("setting up hockey contests...");
			hockey_bot.setup();
			hockey_bot.savePlayers();
	
			Long deadline = hockey_bot.getEarliestGame();
			LocalDate date = Instant.ofEpochMilli(deadline).atZone(ZoneId.systemDefault()).toLocalDate();
	
			JSONArray prop_contests = db.getRosterTemplates("HOCKEYPROPS");
			for(int i = 0; i < prop_contests.length(); i++){
				JSONObject contest = prop_contests.getJSONObject(i);
				String title = date.toString() + " | " + contest.getString("title");
				contest.put("title", title);
				contest.put("odds_source", "n/a");
				contest.put("gameIDs", gameID_array);
				contest.put("registration_deadline", deadline);
	
				JSONObject prop_data = new JSONObject(contest.getString("prop_data"));
				JSONObject pari_mutuel_data;
				if(prop_data.getString("prop_type").equals("TEAM_SNAKE"))
					pari_mutuel_data = hockey_bot.createTeamsPariMutuel(contest, prop_data);
				else
					pari_mutuel_data = hockey_bot.createPariMutuel(deadline, date.toString(), contest);
	
				MethodInstance pari_method = new MethodInstance();
				JSONObject pari_output = new JSONObject("{\"status\":\"0\"}");
				pari_method.input = pari_mutuel_data;
				pari_method.output = pari_output;
				pari_method.session = null;
				pari_method.sql_connection = sql_connection;
				try{
					Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
					c.newInstance(pari_method);
				}
				catch(Exception e){
					log(pari_method.output.toString());
					Server.exception(e);
				}
			}
	
			//read templates from `CONTEST_TEMPLATES` table
			JSONArray roster_contests = db.getRosterTemplates("HOCKEY");
			for(int index = 0; index < roster_contests.length(); index++){
				JSONObject contest = roster_contests.getJSONObject(index);
				String title = date.toString() + " | " + contest.getString("title");
				contest.put("title", title);
				contest.put("odds_source", "n/a");
				contest.put("gameIDs", gameID_array);
				contest.put("registration_deadline", deadline);
				ResultSet options;
				if(contest.getInt("filter") == 0){
					options = db.getOptionTable(hockey_bot.sport, false, 0, hockey_bot.getGameIDs());
				}
				else{
					options = db.getOptionTable(hockey_bot.sport, true, contest.getInt("filter"), hockey_bot.getGameIDs());
				}
	
				JSONObject prop_data = new JSONObject(contest.getString("prop_data"));
				double weight = hockey_bot.getNormalizationWeight(prop_data.getDouble("top_player_salary"), hockey_bot.getGameIDs(), contest.getInt("salary_cap"));
	
				JSONArray option_table = new JSONArray();
				while(options.next()){
					JSONObject player = new JSONObject();
					player.put("name", options.getString(2) + " " + options.getString(3));
					player.put("price", (int) Math.round(options.getDouble(4) * weight));
					player.put("count", 0);
					player.put("id", options.getString(1));
					option_table.put(player);
				}
				contest.put("option_table", option_table);
				MethodInstance method = new MethodInstance();
				JSONObject output = new JSONObject("{\"status\":\"0\"}");
				method.input = contest;
				method.output = output;
				method.session = null;
				method.sql_connection = sql_connection;
				try{
					Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
					c.newInstance(method);
					int contest_id = method.output.getInt("contest_id");
					new EnterAutoplayRosters(sql_connection, contest.getInt("contest_template_id"), contest_id);
				}
				catch(Exception e){
					e.printStackTrace();
				}	
			}	
		} catch (Exception e) {
			Server.exception(e);
		} finally {
			if (sql_connection != null) {
				try {
					sql_connection.close();
				} 
				catch (SQLException ignore) {
					// ignore
				}
			}
		}
	}	
	
	/**
	 * Check to see if contests are in play, settle if necessary.
	 * 
	 * @see com.coinroster.api.SettleContest Spawns instance of SettleContest if a contest has concluded
	 * @throws Exception Can throw an error if new class instance cannot spawn
	 * @throws SQLException
	 */
	public static void checkHockeyContests() {
		Connection sql_connection = null;
		try {
			sql_connection = Server.sql_connection();
			DB db_connection = new DB(sql_connection);
			JSONObject roster_contests = db_connection.check_if_in_play("FANTASYSPORTS", "HOCKEY", "ROSTER");
			JSONObject pari_contests = db_connection.get_active_pari_mutuels("HOCKEYPROPS", "PARI-MUTUEL");
	
			if(!(roster_contests.length() == 0) || !(pari_contests.length() == 0)){
				HockeyBot hockey_bot = new HockeyBot(sql_connection);
				log("Hockey games are in play and minute is multiple of 20");
				hockey_bot.scrapeGameIDs();
				ArrayList<String> gameIDs = hockey_bot.getGameIDs();
				boolean games_ended;
				games_ended = hockey_bot.scrape(gameIDs);
	
				Iterator<?> roster_contest_ids = roster_contests.keys();
				while(roster_contest_ids.hasNext()){
	
					String c_id = (String) roster_contest_ids.next();
					log("hockey contest: " + c_id);
					String scoring_rules_string = roster_contests.getString(c_id);
					JSONObject scoring_rules = new JSONObject(scoring_rules_string);
					JSONArray player_scores = hockey_bot.updateScores(scoring_rules, gameIDs);
					JSONObject fields = new JSONObject();
					fields.put("contest_id", Integer.parseInt(c_id));
					fields.put("normalization_scheme", "INTEGER");
					fields.put("player_scores", player_scores);
	
					MethodInstance method = new MethodInstance();
					JSONObject output = new JSONObject("{\"status\":\"0\"}");
					method.input = fields;
					method.output = output;
					method.session = null;
					method.sql_connection = sql_connection;
					try{
						Constructor<?> c = Class.forName("com.coinroster.api." + "UpdateScores").getConstructor(MethodInstance.class);
						c.newInstance(method);
					}
					catch(Exception e){
						e.printStackTrace();
					}
	
				}
				if(games_ended){
					log("hockey games have ended. Settling now...");
					roster_contests = db_connection.check_if_in_play("FANTASYSPORTS", "HOCKEY", "ROSTER");
					roster_contest_ids = roster_contests.keys();
					while(roster_contest_ids.hasNext()){
						String c_id = (String) roster_contest_ids.next();
						String scoring_rules_string = roster_contests.getString(c_id);
						JSONObject scoring_rules = new JSONObject(scoring_rules_string);
						JSONArray player_scores = hockey_bot.updateScores(scoring_rules, gameIDs);
	
						JSONObject fields = new JSONObject();
						fields.put("contest_id", Integer.parseInt(c_id));
						fields.put("normalization_scheme", "INTEGER");
						fields.put("player_scores", player_scores);
	
						MethodInstance method = new MethodInstance();
						JSONObject output = new JSONObject("{\"status\":\"0\"}");
						method.input = fields;
						method.output = output;
						method.session = null;
						method.sql_connection = sql_connection;
						try{
							Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
							c.newInstance(method);
						}
						catch(Exception e){
							Server.exception(e);
						}
					}
	
					//SETTLE PARIMUTUELS FROM NIGHT'S GAMES
					pari_contests = db_connection.get_active_pari_mutuels("HOCKEYPROPS", "PARI-MUTUEL");
					Iterator<?> pari_contest_ids = pari_contests.keys();	
					while(pari_contest_ids.hasNext()){
						String c_id = (String) pari_contest_ids.next();
	
						JSONObject scoring_rules = new JSONObject(pari_contests.getJSONObject(c_id).getString("scoring_rules"));
						JSONObject prop_data = new JSONObject(pari_contests.getJSONObject(c_id).getString("prop_data"));
						JSONArray option_table = new JSONArray(pari_contests.getJSONObject(c_id).getString("option_table"));
	
						JSONObject pari_fields = hockey_bot.settlePariMutuel(Integer.parseInt(c_id), scoring_rules, prop_data, option_table, gameIDs);
						MethodInstance pari_method = new MethodInstance();
						JSONObject pari_output = new JSONObject("{\"status\":\"0\"}");
						pari_method.input = pari_fields;
						pari_method.output = pari_output;
						pari_method.session = null;
						pari_method.sql_connection = sql_connection;
						try{
							Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
							c.newInstance(pari_method);
						}
						catch(Exception e){
							Server.exception(e);
						}		
					}
				}
			}
		} catch (Exception e) {
			Server.exception(e);
		} finally {
			if (sql_connection != null) {
				try {
					sql_connection.close();
				} 
				catch (SQLException ignore) {
					// ignore
				}
			}
		}
	}
	
	//------------------------------------------------------------------------------------
	
	/**
	 * Creates a new Golf prop contest. Uses the `CONTEST_TEMPLATES` table from the database 
	 * as a format reference.
	 * 
	 * @see com.coinroster.api.CreateContest
	 * @throws Exception Can throw an error if new class instance cannot spawn
	 */
	public static void createGolfContests() {
	
		Connection sql_connection = null;
	
		try {
			sql_connection = Server.sql_connection();
			GolfBot golfBot = new GolfBot(sql_connection);
			DB db = new DB(sql_connection);
			JSONArray roster_contests = db.getRosterTemplates("GOLF");
			JSONArray prop_contests = db.getRosterTemplates("GOLFPROPS");
			int today = getToday();
	
			golfBot.scrapeTourneyID(today);
			if(golfBot.getTourneyID() == null)
				return;
	
			switch(today){
	
			// MONDAY
			case 2: 
				// initialize and scrape
				golfBot.setup();
				golfBot.savePlayers();
	
				//generate tournament ROSTER contests
				for(int index = 0; index < roster_contests.length(); index++){
					// check if the contest is a round 1 contest or tournament contest:
					JSONObject contest = roster_contests.getJSONObject(index);
					golfBot.createGolfRosterContest(contest, "tournament");
					golfBot.createGolfRosterContest(contest, "1");
				}
	
				for(int index = 0; index < prop_contests.length(); index++){
					// check if the contest is a round 1 contest or tournament contest:
					JSONObject contest = prop_contests.getJSONObject(index);						
					golfBot.createGolfPropBet(contest, "tournament");
					golfBot.createGolfPropBet(contest, "1");
				}
				break;
	
				// THURSDAY
			case 5:
	
				//generate tournament ROSTER contests
				for(int index = 0; index < roster_contests.length(); index++){
					// check if the contest is a round 2 contest:
					JSONObject contest = roster_contests.getJSONObject(index);
					golfBot.createGolfRosterContest(contest, "2");
				}
				for(int index = 0; index < prop_contests.length(); index++){
					// check if the contest is a round 1 contest or tournament contest:
					JSONObject contest = prop_contests.getJSONObject(index);
					golfBot.createGolfPropBet(contest, "2");
				}
				break;
	
				// FRIDAY
			case 6:
	
				//generate tournament ROSTER contests
				for(int index = 0; index < roster_contests.length(); index++){
					// check if the contest is a round 2 contest:
					JSONObject contest = roster_contests.getJSONObject(index);
					golfBot.createGolfRosterContest(contest, "3");
				}		
				for(int index = 0; index < prop_contests.length(); index++){
					// check if the contest is a round 1 contest or tournament contest:
					JSONObject contest = prop_contests.getJSONObject(index);
					golfBot.createGolfPropBet(contest, "3");
				}
				break;
	
				// SATURDAY
			case 7:
	
				//generate tournament ROSTER contests
				for(int index = 0; index < roster_contests.length(); index++){
					// check if the contest is a round 2 contest:
					JSONObject contest = roster_contests.getJSONObject(index);
					golfBot.createGolfRosterContest(contest, "4");
				}	
				for(int index = 0; index < prop_contests.length(); index++){
					// check if the contest is a round 1 contest or tournament contest:
					JSONObject contest = prop_contests.getJSONObject(index);
					golfBot.createGolfPropBet(contest, "4");
				}
				break;
	
			default:
				break;
			}
	
		} catch (Exception e) {
			Server.exception(e);
		} finally {
			if (sql_connection != null) {
				try {
					sql_connection.close();
				} 
				catch (SQLException ignore) {
					// ignore
				}
			}
		}
	}
	
	//------------------------------------------------------------------------------------
	
	/**
	 * Calls GolfBot to scrape any additional players depending on current time.
	 * 
	 * @param hour Hour that CronJob is called
	 * @see com.coinroster.CronWorker
	 * @see com.coinroster.bots.GolfBot
	 */
	public static void updateGolfContestField(int hour) {
		Connection sql_connection = null;
		int today = getToday();
		if(today == 2 || today == 3 || today == 4 || (today == 5 && hour < 7 )){
			try {
				sql_connection = Server.sql_connection();
				GolfBot golfBot = new GolfBot(sql_connection);
				golfBot.scrapeTourneyID(today);
				boolean new_players = golfBot.appendLateAdditions();
				if(!new_players)
					log("no new golfers added to field");
			}
			catch (Exception e) {
				Server.exception(e);
			} 
			finally {
				if (sql_connection != null) {
					try {
						sql_connection.close();
					} 
					catch (SQLException ignore) {
						// ignore
					}
				}
			}
		}
	}
	
	//------------------------------------------------------------------------------------
	
	/**	 
	 * Check to see if Golf contests are in play, settle if necessary.
	 * 
	 * Needs to settle both voting round and betting round if settlement time has elapsed.
	 * 
	 * @see com.coinroster.api.SettleContest Spawns instance of SettleContest if a contest has concluded
	 * @throws Exception Can throw an error if new class instance cannot spawn
	 * @throws SQLException
	 */
	public static void checkGolfContests() {
		Connection sql_connection = null;
		try {
			sql_connection = Server.sql_connection();
			DB db_connection = new DB(sql_connection);
			JSONObject roster_contests = db_connection.checkGolfRosterInPlay("FANTASYSPORTS", "GOLF", "ROSTER");
			JSONObject pari_contests = db_connection.checkGolfPropInPlay("FANTASYSPORTS", "GOLFPROPS", "PARI-MUTUEL");
			int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
	
			if(!(roster_contests.length() == 0) || !(pari_contests.length() == 0)){
				log("Golf tournament is in play and minute is multiple of 20");
				String tourney_id = null;
				if(roster_contests.length() != 0)
					tourney_id = roster_contests.getJSONObject((String) roster_contests.keys().next()).getString("gameID");
				else if(pari_contests.length() != 0)
					tourney_id = pari_contests.getJSONObject((String) pari_contests.keys().next()).getString("gameID");
	
				GolfBot golfBot = new GolfBot(sql_connection, tourney_id);
	
				int today = getToday();
				//golfBot.scrapeTourneyID(today);
				JSONObject tournament_status = golfBot.scrapeScores(golfBot.getTourneyID());
	
				Iterator<?> roster_contest_ids = roster_contests.keys();
				while(roster_contest_ids.hasNext()){
					String c_id = (String) roster_contest_ids.next();
	
					// if its Thurs morning, replace INACTIVE players
					if(today == 5 && hour < 9)
						golfBot.checkForInactives(Integer.parseInt(c_id), 0);
	
					// if its Sat or Sun morning or Mon (delayed), replace CUT players
					else if((today == 7 && hour <= 8 && hour >= 7) || (today == 1 && hour <= 8 && hour >= 7) || (today == 2 && hour <= 8 && hour >= 7))
						golfBot.checkForInactives(Integer.parseInt(c_id), 3);
	
					String when = roster_contests.getJSONObject(c_id).getString("when");
					JSONObject scoring_rules = roster_contests.getJSONObject(c_id).getJSONObject("scoring_rules");
	
					JSONArray player_scores = golfBot.updateScores(scoring_rules, when);
					JSONObject fields = new JSONObject();
					fields.put("contest_id", Integer.parseInt(c_id));
					fields.put("normalization_scheme", "INTEGER");
					fields.put("player_scores", player_scores);
	
					MethodInstance method = new MethodInstance();
					JSONObject output = new JSONObject("{\"status\":\"0\"}");
					method.input = fields;
					method.output = output;
					method.session = null;
					method.sql_connection = sql_connection;
					try{
						Constructor<?> c = Class.forName("com.coinroster.api." + "UpdateScores").getConstructor(MethodInstance.class);
						c.newInstance(method);
					}
					catch(Exception e){
						e.printStackTrace();
					}
	
				}
				if(tournament_status.getBoolean("tournament")){
	
					log("golf tournament has ended. Settling tournament contests now...");
					roster_contests = db_connection.checkGolfRosterInPlay("FANTASYSPORTS", "GOLF", "ROSTER");
					pari_contests = db_connection.checkGolfPropInPlay("FANTASYSPORTS", "GOLFPROPS", "PARI-MUTUEL");					
	
					// settle ROSTER contests 
					roster_contest_ids = roster_contests.keys();
					while(roster_contest_ids.hasNext()){
						String c_id = (String) roster_contest_ids.next();
						JSONObject scoring_rules = roster_contests.getJSONObject(c_id).getJSONObject("scoring_rules");
						String when = roster_contests.getJSONObject(c_id).getString("when");
						if(when.equals("tournament")){
							JSONArray player_scores = golfBot.updateScores(scoring_rules, when);
							JSONObject fields = new JSONObject();
							fields.put("contest_id", Integer.parseInt(c_id));
							fields.put("normalization_scheme", "INTEGER");
							fields.put("player_scores", player_scores);
	
							MethodInstance method = new MethodInstance();
							JSONObject output = new JSONObject("{\"status\":\"0\"}");
							method.input = fields;
							method.output = output;
							method.session = null;
							method.sql_connection = sql_connection;
							try{
								Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
								c.newInstance(method);
							}
							catch(Exception e){
								e.printStackTrace();
							}
						}
					}
	
					// settle PROP contests 
					Iterator<?> pari_contest_ids = pari_contests.keys();
					while(pari_contest_ids.hasNext()){
						String c_id = (String) pari_contest_ids.next();
						String when = pari_contests.getJSONObject(c_id).getJSONObject("prop_data").getString("when");
						if(when.equals("tournament")){
							int winning_outcome = golfBot.settlePropBet(pari_contests.getJSONObject(c_id), c_id);
							JSONObject fields = new JSONObject();
							fields.put("contest_id", Integer.parseInt(c_id));
							fields.put("normalization_scheme", "INTEGER");
							fields.put("winning_outcome", winning_outcome);
	
							MethodInstance method = new MethodInstance();
							JSONObject output = new JSONObject("{\"status\":\"0\"}");
							method.input = fields;
							method.output = output;
							method.session = null;
							method.sql_connection = sql_connection;
							try{
								Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
								c.newInstance(method);
							}
							catch(Exception e){
								e.printStackTrace();
							}
						}
					}
				}
	
				for(int i = 1; i <= 4; i++){
					if(tournament_status.getBoolean(String.valueOf(i))){
						log("golf round " + String.valueOf(i) + " has ended. Settling round contests now...");
	
						// Settle ROSTER contests for round
						roster_contests = db_connection.checkGolfRosterInPlay("FANTASYSPORTS", "GOLF", "ROSTER");
						roster_contest_ids = roster_contests.keys();
						while(roster_contest_ids.hasNext()){
							String c_id = (String) roster_contest_ids.next();
							JSONObject scoring_rules = roster_contests.getJSONObject(c_id).getJSONObject("scoring_rules");
							String when = roster_contests.getJSONObject(c_id).getString("when");
							if(when.equals(String.valueOf(i))){
								JSONArray player_scores = golfBot.updateScores(scoring_rules, when);
								JSONObject fields = new JSONObject();
								fields.put("contest_id", Integer.parseInt(c_id));
								fields.put("normalization_scheme", "INTEGER");
								fields.put("player_scores", player_scores);
								MethodInstance method = new MethodInstance();
								JSONObject output = new JSONObject("{\"status\":\"0\"}");
								method.input = fields;
								method.output = output;
								method.session = null;
								method.internal_caller = true;
								method.sql_connection = sql_connection;
								try{
									Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
									c.newInstance(method);
								}
								catch(Exception e){
									Server.exception(e);
								}
							}
						}
	
						// Settle PROP contests for round
						pari_contests = db_connection.checkGolfPropInPlay("FANTASYSPORTS", "GOLFPROPS", "PARI-MUTUEL");					
						Iterator<?> pari_contest_ids = pari_contests.keys();
						while(pari_contest_ids.hasNext()){
							String c_id = (String) pari_contest_ids.next();
							String when = pari_contests.getJSONObject(c_id).getJSONObject("prop_data").getString("when");
							if(when.equals(String.valueOf(i))){
								int winning_outcome = golfBot.settlePropBet(pari_contests.getJSONObject(c_id), c_id);
								JSONObject fields = new JSONObject();
								fields.put("contest_id", Integer.parseInt(c_id));
								fields.put("normalization_scheme", "INTEGER");
								fields.put("winning_outcome", winning_outcome);
	
								MethodInstance method = new MethodInstance();
								JSONObject output = new JSONObject("{\"status\":\"0\"}");
								method.input = fields;
								method.internal_caller = true;
								method.output = output;
								method.session = null;
								method.sql_connection = sql_connection;
								try{
									Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
									c.newInstance(method);
								}
								catch(Exception e){
									e.printStackTrace();
								}
							}
						}
					}
				}
			}
			else{
				int today = getToday();
				if((today == 1 || today == 5 || today == 6 || today == 7) && ((hour % 3) == 0)){
					GolfBot golfBot = new GolfBot(sql_connection);
					log("No current CoinRoster contests but Golf tournament is in play and hour is multiple of 3");
					golfBot.scrapeTourneyID(today);
					if(golfBot.getTourneyID() != null)
						golfBot.scrapeScores(golfBot.getTourneyID());
				}
			}
	
		} catch (Exception e) {
			Server.exception(e);
		} finally {
			if (sql_connection != null) {
				try {
					sql_connection.close();
				} 
				catch (SQLException ignore) {
					// ignore
				}
			}
		}
	}	
	
	//------------------------------------------------------------------------------------
	
	/**	 
	 * Check to see if crowd contests are in play, settle if necessary.
	 * 
	 * Needs to settle both voting round and betting round if settlement time has elapsed.
	 * 
	 * @see com.coinroster.api.SettleContest Spawns instance of SettleContest if a contest has concluded
	 * @throws Exception Can throw an error if new class instance cannot spawn
	 * @throws SQLException
	 */
	public static void checkCrowdContests() {
	
		Connection sql_connection = null;
		try {
			sql_connection = Server.sql_connection();
			DB db = new DB(sql_connection);
	
			// get voting round contests that have status 1
			ArrayList<Integer> voting_contest_ids = db.check_if_in_play();
	
			if(!voting_contest_ids.isEmpty()){
				CrowdSettleBot crowd_bot = new CrowdSettleBot(sql_connection);
	
				// iterate through list of ids and check for contests whose registration deadline expired
				for(Integer contest_id : voting_contest_ids){
	
					JSONObject contest = db.select_contest(contest_id);
					if(new Date().getTime() > contest.getLong("registration_deadline")) {
	
						// Settle Voting Round
						log("Voting round for contest: " + contest_id + " has ended, settling");
						JSONObject input = crowd_bot.settlePariMutuel(contest_id);
						input.put("contest_id", contest_id);
						log(input.toString());
	
						// no bets on voting round; back
						if(input.getInt("winning_outcome") == 0 || input.has("multiple_winning_outcomes")) {
							new BackoutContest(sql_connection, contest_id);
							new BackoutContest(sql_connection, db.get_original_contest(contest_id));
	
							return;
						}
	
						// multiple bets placed, notify admin
						if(input.has("multiple_bets")) {
							JSONObject cash_register = db.select_user("username", "internal_cash_register");
	
							String
	
							cash_register_email_address = cash_register.getString("email_address"),
							cash_register_admin = "Cash Register Admin",
	
							subject_admin = "Crowd Settled Contest Has Multiple Entries",
							message_body_admin = "";
	
							message_body_admin += "<br/>";
							message_body_admin += "<br/>";
							message_body_admin += "A crowd settled contest has entries that users have placed on multiple options. Please settle the contest below:";
							message_body_admin += "<br/>";
							message_body_admin += "<br/>";
							message_body_admin += "Contest ID: <b>" + contest_id + "</b>";
							message_body_admin += "<br/>";
							message_body_admin += "<br/>";
							message_body_admin += "Please settle the contest from the admin panel.";
							message_body_admin += "<br/>";
	
							Server.send_mail(cash_register_email_address, cash_register_admin, subject_admin, message_body_admin);
							new UpdateContestStatus(sql_connection, contest_id, 5);
							return;
						}
	
						MethodInstance method = new MethodInstance();
						JSONObject output = new JSONObject("{\"status\":\"0\"}");
						method.input = input;
						method.output = output;
						method.session = null;
						method.sql_connection = sql_connection;
						try{
							Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
							c.newInstance(method);
						}
						catch(Exception e){
							Server.exception(e);
						}
						input.remove("contest_id");
	
						// get contest ID of contest that created voting round
	
						int original_contest_id = db.get_original_contest(contest_id);
	
						// settle original contest
						input.put("contest_id", original_contest_id);							
	
						MethodInstance original_method = new MethodInstance();
						original_method.input = input;
						original_method.output = output;
						original_method.session = null;
						original_method.sql_connection = sql_connection;
						try{
							Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
							c.newInstance(method);
						}
						catch(Exception e){
							e.printStackTrace();
						}
					}
				}
			}
		}catch (Exception e) {
			Server.exception(e);
		} 
		finally {
			if (sql_connection != null) {
				try {
					sql_connection.close();
				} 
				catch (SQLException ignore) {
					// ignore
				}
			}
		}
	}
	
	//------------------------------------------------------------------------------------
	
	
	/**
	 * Creates a new Baseball prop contest. Uses the `CONTEST_TEMPLATES` table from the database 
	 * as a format reference.
	 * 
	 * @see com.coinroster.api.CreateContest
	 * @throws Exception Can throw an error if new class instance cannot spawn
	 */
	public static void createBaseballContests() {
	
		Connection sql_connection = null;
	
		try {
			sql_connection = Server.sql_connection();
			DB db = new DB(sql_connection);
			BaseballBot baseball_bot = new BaseballBot(sql_connection);
			String gameID_array = baseball_bot.scrapeGameIDs();
			if(baseball_bot.getGameIDs() == null)
				return;
			log("setting up baseball contests...");
			baseball_bot.setup();
			baseball_bot.savePlayers();
	
			Long deadline = baseball_bot.getEarliestGame();
			LocalDate date = Instant.ofEpochMilli(deadline).atZone(ZoneId.systemDefault()).toLocalDate();
	
			JSONArray prop_contests = db.getRosterTemplates("BASEBALLPROPS");
			for(int i = 0; i < prop_contests.length(); i++){
				JSONObject contest = prop_contests.getJSONObject(i);
				String title = date.toString() + " | " + contest.getString("title");
				contest.put("title", title);
				contest.put("odds_source", "n/a");
				contest.put("gameIDs", gameID_array);
				contest.put("registration_deadline", deadline);
	
				JSONObject prop_data = new JSONObject(contest.getString("prop_data"));
				JSONObject pari_mutuel_data;
				if(prop_data.getString("prop_type").equals("TEAM_SNAKE"))
					pari_mutuel_data = baseball_bot.createTeamsPariMutuel(contest, prop_data);
				else
					pari_mutuel_data = baseball_bot.createPariMutuel(deadline, date.toString(), contest);
	
				MethodInstance pari_method = new MethodInstance();
				JSONObject pari_output = new JSONObject("{\"status\":\"0\"}");
				pari_method.input = pari_mutuel_data;
				pari_method.output = pari_output;
				pari_method.session = null;
				pari_method.sql_connection = sql_connection;
				try{
					Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
					c.newInstance(pari_method);
				}
				catch(Exception e){
					log(pari_method.output.toString());
					Server.exception(e);
				}
			}
	
			//read templates from `CONTEST_TEMPLATES` table
			JSONArray roster_contests = db.getRosterTemplates("BASEBALL");
			for(int index = 0; index < roster_contests.length(); index++){
				JSONObject contest = roster_contests.getJSONObject(index);
				String title = date.toString() + " | " + contest.getString("title");
				contest.put("title", title);
				contest.put("odds_source", "n/a");
				contest.put("gameIDs", gameID_array);
				contest.put("registration_deadline", deadline);
				ResultSet options;
				JSONObject prop_data = new JSONObject(contest.get("prop_data").toString());
	
				if(contest.getInt("filter") == 0){
					options = db.getOptionTable(baseball_bot.sport, false, 0, baseball_bot.getGameIDs());
				}
				else{
					options = db.getOptionTable(baseball_bot.sport, true, contest.getInt("filter"), baseball_bot.getGameIDs());
				}
	
				double original_max = 0, original_min = 0;
				if(options.first())
					original_max = options.getDouble(4);
				if(options.last())
					original_min = options.getDouble(4);
	
				double range = original_max - original_min;
	
				double new_max = (double) contest.getInt("salary_cap") * prop_data.getDouble("top_player_salary");
	
				// this new_min can eventually be a DB value
				double new_min = (double) contest.getInt("salary_cap") * 0.125;
				double new_range = new_max - new_min;
	
				options.first();
	
				JSONArray option_table = new JSONArray();
				while(options.next()){
					JSONObject player = new JSONObject();
					player.put("name", options.getString(2) + " " + options.getString(3));
					player.put("price", (int) Math.round((((options.getDouble(4) - original_min) / range) * new_range) + new_min));
					player.put("count", 0);
					player.put("id", options.getString(1));
					option_table.put(player);
				}
	
				contest.put("option_table", option_table);
				MethodInstance method = new MethodInstance();
				JSONObject output = new JSONObject("{\"status\":\"0\"}");
				method.input = contest;
				method.output = output;
				method.session = null;
				method.sql_connection = sql_connection;
				try{
					Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
					c.newInstance(method);
					int contest_id = method.output.getInt("contest_id");
					new EnterAutoplayRosters(sql_connection, contest.getInt("contest_template_id"), contest_id);
				}
				catch(Exception e){
					Server.exception(e);
				}	
	
	
			}	
		} catch (Exception e) {
			Server.exception(e);
		} finally {
			if (sql_connection != null) {
				try {
					sql_connection.close();
				} 
				catch (SQLException ignore) {
					// ignore
				}
			}
		}
	}	
	
	/**	 
	 * Check to see if Golf contests are in play, settle if necessary.
	 * 
	 * Needs to settle both voting round and betting round if settlement time has elapsed.
	 * 
	 * @see com.coinroster.api.SettleContest Spawns instance of SettleContest if a contest has concluded
	 * @throws Exception Can throw an error if new class instance cannot spawn
	 * @throws SQLException
	 */
	public static void checkBaseballContests() {
		Connection sql_connection = null;
		try {
			sql_connection = Server.sql_connection();
			DB db_connection = new DB(sql_connection);
			JSONObject roster_contests = db_connection.check_if_in_play("FANTASYSPORTS", "BASEBALL", "ROSTER");
			JSONObject pari_contests = db_connection.get_active_pari_mutuels("BASEBALLPROPS", "PARI-MUTUEL");
	
			if(!(roster_contests.length() == 0) || !(pari_contests.length() == 0)){
				BaseballBot baseball_bot = new BaseballBot(sql_connection);
				log("Baseball games are in play and minute is multiple of 20");
				baseball_bot.scrapeGameIDs();
				ArrayList<String> gameIDs = baseball_bot.getGameIDs();
				boolean games_ended;
				games_ended = baseball_bot.scrape(gameIDs);
	
				Iterator<?> roster_contest_ids = roster_contests.keys();
				while(roster_contest_ids.hasNext()){
	
					String c_id = (String) roster_contest_ids.next();
					log("baseball contest: " + c_id);
					String scoring_rules_string = roster_contests.getString(c_id);
					JSONObject scoring_rules = new JSONObject(scoring_rules_string);
					JSONArray player_scores = baseball_bot.updateScores(scoring_rules, gameIDs);
					JSONObject fields = new JSONObject();
					fields.put("contest_id", Integer.parseInt(c_id));
					fields.put("normalization_scheme", "INTEGER");
					fields.put("player_scores", player_scores);
	
					MethodInstance method = new MethodInstance();
					JSONObject output = new JSONObject("{\"status\":\"0\"}");
					method.input = fields;
					method.output = output;
					method.session = null;
					method.sql_connection = sql_connection;
					try{
						Constructor<?> c = Class.forName("com.coinroster.api." + "UpdateScores").getConstructor(MethodInstance.class);
						c.newInstance(method);
					}
					catch(Exception e){
						e.printStackTrace();
					}
	
				}
				if(games_ended){
					log("baseball games have ended. Settling now...");
					roster_contests = db_connection.check_if_in_play("FANTASYSPORTS", "BASEBALL", "ROSTER");
					roster_contest_ids = roster_contests.keys();
					while(roster_contest_ids.hasNext()){
						String c_id = (String) roster_contest_ids.next();
						String scoring_rules_string = roster_contests.getString(c_id);
						JSONObject scoring_rules = new JSONObject(scoring_rules_string);
						JSONArray player_scores = baseball_bot.updateScores(scoring_rules, gameIDs);
	
						JSONObject fields = new JSONObject();
						fields.put("contest_id", Integer.parseInt(c_id));
						fields.put("normalization_scheme", "INTEGER");
						fields.put("player_scores", player_scores);
	
						MethodInstance method = new MethodInstance();
						JSONObject output = new JSONObject("{\"status\":\"0\"}");
						method.input = fields;
						method.output = output;
						method.session = null;
						method.sql_connection = sql_connection;
						try{
							Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
							c.newInstance(method);
						}
						catch(Exception e){
							Server.exception(e);
						}
					}
	
					//SETTLE PARIMUTUELS FROM NIGHT'S GAMES
					pari_contests = db_connection.get_active_pari_mutuels("BASEBALLPROPS", "PARI-MUTUEL");
					Iterator<?> pari_contest_ids = pari_contests.keys();	
					while(pari_contest_ids.hasNext()){
						String c_id = (String) pari_contest_ids.next();
	
						JSONObject scoring_rules = new JSONObject(pari_contests.getJSONObject(c_id).getString("scoring_rules"));
						JSONObject prop_data = new JSONObject(pari_contests.getJSONObject(c_id).getString("prop_data"));
						JSONArray option_table = new JSONArray(pari_contests.getJSONObject(c_id).getString("option_table"));
	
						JSONObject pari_fields = baseball_bot.settlePariMutuel(Integer.parseInt(c_id), scoring_rules, prop_data, option_table, gameIDs);
						MethodInstance pari_method = new MethodInstance();
						JSONObject pari_output = new JSONObject("{\"status\":\"0\"}");
						pari_method.input = pari_fields;
						pari_method.output = pari_output;
						pari_method.session = null;
						pari_method.sql_connection = sql_connection;
						try{
							Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
							c.newInstance(pari_method);
						}
						catch(Exception e){
							Server.exception(e);
						}		
					}
				}
			}
		} catch (Exception e) {
			Server.exception(e);
		} finally {
			if (sql_connection != null) {
				try {
					sql_connection.close();
				} 
				catch (SQLException ignore) {
					// ignore
				}
			}
		}
	}
	
	/**
	 * Get current day as an integer.
	 * SUN = 1
	 * MON = 2
	 * TUES = 3
	 * WED = 4
	 * THUR = 5
	 * FRI = 6
	 * SAT = 7
	 * 
	 * @return int representing day of week
	 * 
	 */
	public static int getToday(){
		Calendar c = Calendar.getInstance();        		
		int today = c.get(Calendar.DAY_OF_WEEK);
		return today;
	}
}
