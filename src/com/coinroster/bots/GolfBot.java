package com.coinroster.bots;

import java.sql.Connection;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Utils;
import com.coinroster.internal.BackoutContest;
import com.coinroster.internal.EnterAutoplayRosters;
import com.coinroster.internal.JsonReader;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.json.JSONArray;

public class GolfBot extends Utils {

	public static String method_level = "admin";
	public String year = "2019";
	protected Map<String, Player> players_list;
	private String tourneyID;
	private String tourneyName;
	private long startDate;
	private DB db;
	public String sport = "GOLF";
	public ArrayList<String> gameIDs = new ArrayList<String>();
	private static Connection sql_connection = null;
	
	public GolfBot(Connection sql_connection) throws IOException, JSONException{
		GolfBot.sql_connection = sql_connection;
		db = new DB(sql_connection);
	}
	
	public GolfBot(Connection sql_connection, String tourney_id) throws IOException, JSONException{
		GolfBot.sql_connection = sql_connection;
		db = new DB(sql_connection);
		this.tourneyID = tourney_id;
		ArrayList<String> gameIDs = new ArrayList<String>();
		gameIDs.add(tourney_id);
		this.gameIDs = gameIDs;
	}
	
	
	public Map<String, Player> getPlayerHashMap(){
		return players_list;
	}
	public String getTourneyName(){
		return tourneyName;
	}
	
	public String getTourneyID(){
		return this.tourneyID;
	}
	
	public long getDeadline(){
		return startDate;
	}
	
	public ArrayList<String> getGameIDs(){
		return gameIDs;
	}
	
	public String getLiveTourneyID() throws SQLException{
		ResultSet result_set = null;
		String id = null;
		try {
			PreparedStatement get_games = sql_connection.prepareStatement("select distinct gameID from player where sport_type=?");
			get_games.setString(1, this.sport);
			result_set = get_games.executeQuery();		
		}
		catch(Exception e){
			e.printStackTrace();
		}
		while(result_set.next()){
			id = result_set.getString(1);	
		}
		return id;
	}
	
	public boolean appendLateAdditions() throws SQLException, JSONException, IOException, InterruptedException{
		
		Map<Integer, JSONArray> contest_players = new HashMap<Integer, JSONArray>();
		ResultSet result_set = null;
		
		try {
			PreparedStatement get_contests = sql_connection.prepareStatement("select id, option_table from contest where gameIDs=? and contest_type='ROSTER' and status=1");
			get_contests.setString(1, this.getTourneyID());
			result_set = get_contests.executeQuery();		
		}
		catch(Exception e){
			Server.exception(e);
		}
		while(result_set.next()){
			int id = result_set.getInt(1);
			JSONArray option_table = new JSONArray(result_set.getString(2));
			contest_players.put(id, option_table);	
		}
		
		if(contest_players.isEmpty()){
			return false;
		}
		
		ArrayList<String> existing_ids = new ArrayList<String>();
		try{
			ResultSet all_existing_players = db.getAllPlayerIDs(this.sport, this.getTourneyID());
			while(all_existing_players.next()){
				existing_ids.add(all_existing_players.getString(1));
			}
		}catch(Exception e){		
			Server.exception(e);
		}
				
		boolean flag = false;
		String url = "https://statdata.pgatour.com/r/" + this.getTourneyID() + "/field.json";
		JSONObject field = JsonReader.readJsonFromUrl(url);
		if(field == null){
			log("Unable to connect to pgatour API");
			return false;
		}
		
		JSONArray players_json = field.getJSONObject("Tournament").getJSONArray("Players");
		boolean player_table_updated = false;
		
		for(Map.Entry<Integer, JSONArray> entry : contest_players.entrySet()){
			int contest_id = entry.getKey();
			JSONArray option_table = entry.getValue();
			
			//check if any players in DB's player table didn't make it in:
			for(String existing_id : existing_ids){
				boolean in_option_table = false;
				for(int option_table_index = 0; option_table_index < option_table.length(); option_table_index++){
					String p_id = option_table.getJSONObject(option_table_index).getString("id");
					if(p_id.equals(existing_id)){
						in_option_table = true;
						break;
					}
				}
				
				//add player from player table to option table
				if(!in_option_table){
					log("could not find a match for " + existing_id);
					PreparedStatement get_data = sql_connection.prepareStatement("select name, team_abr, salary, filter_on from player where sport_type = ? and id = ? and gameID = ?");
					get_data.setString(1, this.sport);
					get_data.setString(2, existing_id);
					get_data.setString(3, this.getTourneyID());
					ResultSet player_data = get_data.executeQuery();
					if(player_data.next()){
						String name = player_data.getString(1);
						String country = player_data.getString(2);
						double price = player_data.getDouble(3);
						int status = player_data.getInt(4);
						if(status != 4){
							log(existing_id + " is inactive");
							continue;
						}
							
						JSONObject p = new JSONObject();
						p.put("id", existing_id);
						p.put("name", name + " " + country);
						p.put("count", 0);
						p.put("price", price);
						option_table.put(p);
						log("appending " + name + " to contest " + contest_id);
						PreparedStatement update_contest = sql_connection.prepareStatement("UPDATE contest SET option_table = ? where id = ?");
						update_contest.setString(1, option_table.toString());
						update_contest.setInt(2, contest_id);
						update_contest.executeUpdate();
					}
				}
			}
			
			// check for players new players in field
			for(int i = 0; i < players_json.length(); i++){
				JSONObject player = players_json.getJSONObject(i);
				String id = player.getString("TournamentPlayerId");
				
				if(!existing_ids.contains(id)){
					
					PricingStat pricing = this.getPricingData();
					Player new_player = this.initializePlayer(player, pricing);
					if(!player_table_updated){
						JSONObject data = new_player.getDashboardData(this.year);
						// add player to player table
						try{
							PreparedStatement add_player = sql_connection.prepareStatement("INSERT INTO player (id, name, sport_type, gameID, team_abr, salary, data, points, bioJSON, filter_on) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
						
							add_player.setString(1, id);
							add_player.setString(2, new_player.getName());
							add_player.setString(3, this.sport);
							add_player.setString(4, this.getTourneyID());
							add_player.setString(5, new_player.getCountry());
							add_player.setDouble(6, new_player.getSalary());
							add_player.setString(7, initializeGolferScores().toString());
							add_player.setDouble(8, 0.0);
							add_player.setString(9, data.toString());
							add_player.setInt(10, 4);
							add_player.executeUpdate();
							log("adding " + id + " to the player table");
						}
						catch(MySQLIntegrityConstraintViolationException e){
							log("player with id " + id + " is already in the player table");
						}
					}
					
					//create JSONObject to add to option table
					JSONObject p = new JSONObject();
					p.put("price", new_player.getSalary());
					p.put("count", 0);
					p.put("name", new_player.getName() + " " + new_player.getCountry());
					p.put("id", id);
					log("appending " + new_player.getName() + " to contest " + contest_id );
					option_table.put(p);	
				}
			}
			player_table_updated = true;
			PreparedStatement update_contest = sql_connection.prepareStatement("UPDATE contest SET option_table = ? where id = ?");
			update_contest.setString(1, option_table.toString());
			update_contest.setInt(2, contest_id);
			update_contest.executeUpdate();
		}
		return flag;
	}
	
	public void scrapeTourneyID(int today) throws IOException, JSONException, InterruptedException{
		
		// get the Thursday date in yyyy-MM-dd format
		SimpleDateFormat formattedDate = new SimpleDateFormat("yyyy-MM-dd");            
		Calendar c = Calendar.getInstance();
		
		if(today == 2)
			c.add(Calendar.DATE, 3);
		else if(today == 3)
			c.add(Calendar.DATE, 2);
		else if(today == 4)
			c.add(Calendar.DATE, 1);
		else if(today == 6)
			c.add(Calendar.DATE, -1);
		else if(today == 7)
			c.add(Calendar.DATE, -2);
		else if(today == 1)
			c.add(Calendar.DATE, -3);
		
		String thursday = (String)(formattedDate.format(c.getTime()));
		c.set(Calendar.HOUR_OF_DAY, 7);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		long milli = c.getTimeInMillis();
		// parse schedule json to get tournaments
		String url = "https://statdata.pgatour.com/r/current/schedule-v2.json";
		JSONObject schedule = JsonReader.readJsonFromUrl(url);
		// sometimes pgatour.com decides to change their API and we'll get an exception here
		// if so, change getJSONObject(x) from 1 --> 0 or 0 --> 1
		JSONArray tournaments = schedule.getJSONArray("years").getJSONObject(0).getJSONArray("tours").getJSONObject(0).getJSONArray("trns");

		for(int index=0; index < tournaments.length(); index++){
			JSONObject tournament = tournaments.getJSONObject(index);
			
			//check 3 things: start-date is this coming thurs, tournament is stroke play, and tournament is week's primary tournament
			if(tournament.getJSONObject("date").getString("start").equals(thursday) && tournament.getString("format").equals("Stroke") && tournament.getString("primaryEvent").equals("Y")){
				this.tourneyName = tournament.getJSONObject("trnName").getString("official");
				this.tourneyID = tournament.getString("permNum");
				this.gameIDs.add(this.tourneyID);
				this.startDate = milli;
				log("setting up for tournament: " + this.getTourneyName());
				break;
			}
		}
	}
	
	public String getCountry(String id) throws JSONException, IOException, InterruptedException{
		Document page = Jsoup.connect("https://www.pgatour.com/players/player." + id + ".html").userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
			      .referrer("http://www.google.com").timeout(0).get();
		Element country_img = page.getElementsByClass("country").first().getElementsByTag("span")
				.first().getElementsByTag("img").first();
		String country_code = country_img.attr("src").split("/")[7].replace(".png", "");
		return country_code;
	}
	
	
	public PricingStat getPricingData() throws IOException, JSONException{
		
		//OWGR stats - preference 1
		String rank_url = "https://statdata.pgatour.com/r/stats/" + this.year + "/186.json";
		JSONObject world_rankings_json = JsonReader.readJsonFromUrl(rank_url);
		boolean owgr = false;
		JSONArray players_list = new JSONArray();
		Map<String, Double> player_prices = new HashMap<String, Double>();
		
		try{
			players_list = world_rankings_json.getJSONArray("tours").getJSONObject(0).getJSONArray("years").getJSONObject(0).getJSONArray("stats").getJSONObject(0).getJSONArray("details");
			if(players_list.length() > 1)
				owgr = true;
		}catch(Exception e){
			owgr = false;
		}
		if(!owgr){
			//scoring average stats - preference 2
			log("OWGR seems to be unavailable so we are grabbing scoring average stats");
			String avg_url = "https://statdata.pgatour.com/r/stats/" + this.year + "/120.json";
			JSONObject avg_json = JsonReader.readJsonFromUrl(avg_url);
			players_list = avg_json.getJSONArray("tours").getJSONObject(0).getJSONArray("years").getJSONObject(0).getJSONArray("stats").getJSONObject(0).getJSONArray("details");
			Double[] normal_dist = new Double[players_list.length()];
			for(int index = 0; index < players_list.length(); index++){
				Random r = new Random();
				Double sample = r.nextGaussian() * 75 + 200;
				normal_dist[index] = sample;
			}
			
			// sort the array
			Arrays.sort(normal_dist, Collections.reverseOrder());

			for(int i = 0; i < players_list.length(); i++){
				String player_id = players_list.getJSONObject(i).getString("plrNum");
				player_prices.put(player_id, normal_dist[i]);
			}
		}
		PricingStat pricing = new PricingStat(owgr, players_list);
		if(!owgr) pricing.setPriceMap(player_prices);
		
		return pricing;
	}
	
	public Player initializePlayer(JSONObject player, PricingStat pricing) throws JSONException, IOException, InterruptedException{
		
		boolean checked = false;
		String country = "";
		double salary = 0;
		String id = player.getString("TournamentPlayerId");
		String name = player.getString("PlayerName");
		String names[] = name.split(", ");
		String name_fl;
		try{
			name_fl = names[1] + " " + names[0];
		}
		catch(Exception e){
			name_fl = names[0];
		}
		
		for(int q = 0; q < pricing.getPlayerPricingArray().length(); q++){
			JSONObject r_player = pricing.getPlayerPricingArray().getJSONObject(q);
			if(id.equals(r_player.getString("plrNum"))){
				String points = null;
				// owgr stat 
				if(pricing.isOWGR()){
					points = r_player.getJSONObject("statValues").getString("statValue2");
					country = r_player.getJSONObject("statValues").getString("statValue5");
					salary = Math.round(Double.parseDouble(points) * 10);
				}	
				// get normalized price from normal distribution
				else{
					salary = pricing.getPriceMap().get(id);
				}
				
				if(country.isEmpty())
					country = getCountry(id);
				
				if(salary < 80.0)
					salary = 80.0;
				checked = true;
				break;
			}
		}
		if(!checked){
			salary = 80.0;
		}
		
		Player p = new Player(id, name_fl, this.tourneyID);
		p.set_ppg_salary(salary);
		p.setCountry(country);
		return p;
	}
	
	public Map<String, Player> setup() throws IOException, JSONException, SQLException, InterruptedException {
		
		Map<String, Player> players = new HashMap<String, Player>();
		
		if(this.tourneyID != null){
			// tourney field
			String url = "https://statdata.pgatour.com/r/" + tourneyID + "/field.json";
			JSONObject field = JsonReader.readJsonFromUrl(url);
			JSONArray players_json = field.getJSONObject("Tournament").getJSONArray("Players");
			log("getting the field...");
			
			// get pricing
			PricingStat pricing = this.getPricingData();
			
			for(int i = 0; i < players_json.length(); i++){
				JSONObject player = players_json.getJSONObject(i);
				Player p = this.initializePlayer(player, pricing);
				players.put(p.getPGA_ID(), p);
			}
		}
		this.players_list = players;
		return players;
	}
	
	public JSONObject initializeGolferScores() throws JSONException{
		JSONObject data = new JSONObject();
		for(int i = 1; i <= 4; i++){
			JSONObject round = new JSONObject();
			round.put("eagles+", 0);
			round.put("double-bogeys-", 0);
			round.put("bogeys", 0);
			round.put("pars", 0);
			round.put("birdies", 0);
			round.put("today", 0);
			data.put(String.valueOf(i), round);
		}
		data.put("overall", 0);
		return data;
	}
	
	public void savePlayers(){
		try {
			
			if(sql_connection.isClosed()){
				sql_connection = Server.sql_connection();
			}
			
			if(this.getPlayerHashMap() == null)
				return;
			
			for(Player player : this.getPlayerHashMap().values()){
				
				PreparedStatement save_player = sql_connection.prepareStatement("insert into player(id, name, sport_type, gameID, team_abr, salary, data, points, bioJSON, filter_on) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
				save_player.setString(1, player.getPGA_ID());
				save_player.setString(2, player.getName());
				save_player.setString(3, sport);
				save_player.setString(4, player.getTourneyID());
				save_player.setDouble(6, player.getSalary());
				save_player.setString(7, initializeGolferScores().toString());
				save_player.setDouble(8, player.getPoints());
				JSONObject data = player.getDashboardData(this.year);
				save_player.setString(5, player.getCountry());
				save_player.setString(9, data.toString());
				save_player.setInt(10, 4);
				save_player.executeUpdate();	
			}
			
			log("added " + sport + " players to DB");
		}
		catch (Exception e) {
			Server.exception(e);
		}
	}
	
	/*
	 * INACTIVE = 0
	 * WD = 1
	 * DQ = 2 
	 * CUT = 3
	 * ACTIVE = 4
	 */
	public int setStatusInt(String status){
		int status_int = 0;
		switch(status){
			case "active":
				status_int = 4;
				break;
			case "cut": 
				status_int = 3;
				break;
			case "dq":
				status_int = 2;
				break;
			case "wd":
				status_int = 1;
				break;
			//not in leaderboard
			default:
				status_int = 0;
				break;
		}
		return status_int;
	}

	public JSONObject scrapeScores(String tourneyID) throws JSONException, IOException, SQLException, InterruptedException{
		JSONObject tournament_statuses = new JSONObject();
		
		tournament_statuses.put("1", false);
		tournament_statuses.put("2", false);
		tournament_statuses.put("3", false);
		tournament_statuses.put("4", false);
		tournament_statuses.put("tournament", false);
		
		String url = "https://statdata.pgatour.com/r/" + tourneyID + "/" + this.year + "/leaderboard-v2.json";
		JSONObject leaderboard = JsonReader.readJsonFromUrl(url);
		if(leaderboard == null){
			log("couldn't connect to pgatour API");
			return tournament_statuses;
		}
		boolean finished = leaderboard.getJSONObject("leaderboard").getBoolean("is_finished");
		int round = leaderboard.getJSONObject("leaderboard").getInt("current_round");
		String round_state = leaderboard.getJSONObject("leaderboard").getString("round_state");
		
		if(finished){
			tournament_statuses.put("1", true);
			tournament_statuses.put("2", true);
			tournament_statuses.put("3", true);
			tournament_statuses.put("4", true);
			tournament_statuses.put("tournament", true);
		}else if(round_state.equals("Official")){
			for(int i = round; i >= 1; i--){
				tournament_statuses.put(String.valueOf(i), true);
			}
		}
		
		log("tournament status: " + tournament_statuses.toString());

		JSONArray players = leaderboard.getJSONObject("leaderboard").getJSONArray("players");
		log(this.getGameIDs().toString());
		ResultSet playerScores = db.getPlayerScores(this.sport, this.getGameIDs());
		while(playerScores.next()){
			boolean in_leaderboard = false;
			String id = playerScores.getString(1);
			int score;
			for(int i=0; i < players.length(); i++){
				JSONObject player = players.getJSONObject(i);
				String player_id = player.getString("player_id");
				if(id.equals(player_id)){
					JSONObject data = new JSONObject(playerScores.getString(2));
					JSONObject scores_to_edit = data.getJSONObject(String.valueOf(round));
					in_leaderboard = true;
					String status = player.getString("status");
					int status_int = setStatusInt(status);
					scores_to_edit.put("eagles+", (player.getJSONObject("par_performance").getInt("double_eagles") + player.getJSONObject("par_performance").getInt("eagles")));
					scores_to_edit.put("double-bogeys-", (player.getJSONObject("par_performance").getInt("double_bogeys") + player.getJSONObject("par_performance").getInt("more_bogeys")));
					scores_to_edit.put("birdies", player.getJSONObject("par_performance").getInt("birdies"));
					scores_to_edit.put("bogeys", player.getJSONObject("par_performance").getInt("bogeys"));
					scores_to_edit.put("pars", player.getJSONObject("par_performance").getInt("par"));
					scores_to_edit.put("today", player.get("today"));
					data.put(String.valueOf(round), scores_to_edit);

					if(status.equals("cut"))
						score = -888;
					else if(status.equals("wd"))
						score = -999;
					else if(status.equals("dq"))
						score = -666;
					else{
						try{
							score = player.getInt("total");
						}
						catch(JSONException e){
							score = -888;
						}
					}
					data.put("overall", score);
					db.updateGolferScore(player_id, this.sport, data.toString(), score, status_int, this.getTourneyID());
					break;
				}
			}
			if(!in_leaderboard){
				score = -777;
				JSONObject data = new JSONObject(playerScores.getString(2));
				data.put("overall", score);
				db.updateGolferScore(id, this.sport, data.toString(), score, 0, this.getTourneyID());

			}
		}
		return tournament_statuses;		
	}
	
	public int findWorstScore(String when) throws SQLException, JSONException{
		int worstScore = 0;
		if(when.equals("tournament")){
			PreparedStatement worst_score = sql_connection.prepareStatement("select points from player where sport_type=? and gameID = ? order by points DESC limit 1");
			worst_score.setString(1, this.sport);
			worst_score.setString(2, this.getTourneyID());
			ResultSet score = worst_score.executeQuery();
			while(score.next()){
				worstScore = score.getInt(1);
			}
		}
		else{
			int worst = -999;
			ResultSet all_player_scores = db.getPlayerScores(this.sport, this.gameIDs);
			while(all_player_scores.next()){
				JSONObject scores = new JSONObject(all_player_scores.getString(2));
				try{
					int score = scores.getJSONObject(when).getInt("today");
					if(score > worst)
						worst = score;
				}catch(JSONException e){
					continue;
				}
			}
			worstScore = worst;
		}
		return worstScore;
	}
	
	public JSONArray updateScores(JSONObject scoring_rules, String when) throws SQLException, JSONException{
		
		ResultSet playerScores = db.getPlayerScoresAndStatus(this.sport, this.getTourneyID());
		JSONArray player_map = new JSONArray();
		
		// no scoring rules = score to par tournament
		if(scoring_rules.length() == 0 || scoring_rules == null){
			
			log("updating option table for score-to-par contests...");
			// get worst score relative to when (tournament or round)
			int worstScore = findWorstScore(when);
			if(when.equals("tournament"))
				log("worst score in " + when + ": " + worstScore);
			else
				log("worst score in round " + when + ": " + worstScore);
			
			log("normalizing scores...");
			
			while(playerScores.next()){
				JSONObject player = new JSONObject();
				String id = playerScores.getString(1);
				JSONObject data = new JSONObject(playerScores.getString(2));
				int status = playerScores.getInt(3);
				int score_db = playerScores.getInt(4);
				
				// TOURNAMENT ROSTER CONTEST
				if(when.equals("tournament")){
					if(score_db == -999){
						player.put("id", id);
						player.put("score_raw", "WD");
						player.put("score_normalized", 0);
						player_map.put(player);
					}
					else if(score_db == -888){
						player.put("id", id);
						player.put("score_raw", "CUT");
						player.put("score_normalized", 0);
						player_map.put(player);
					}
					else if(score_db == -777){
						player.put("id", id);
						player.put("score_raw", "INACTIVE");
						player.put("score_normalized", 0);
						player_map.put(player);
					}
					else if(score_db == -666){
						player.put("id", id);
						player.put("score_raw", "DQ");
						player.put("score_normalized", 0);
						player_map.put(player);
					}
					else{
						int normalizedScore = normalizeScore(score_db, worstScore);
						player.put("id", id);
						player.put("score_raw", Integer.toString(score_db));
						player.put("score_normalized", normalizedScore);
						player_map.put(player);
					}
				}
				
				// ROUND ROSTER CONTEST
				else{
					if(status == 4){
						int score = 0;
						try{
							score = data.getJSONObject(when).getInt("today");
						}catch(Exception e){
							score = 0;
						}
						int normalizedScore = normalizeScore(score, worstScore);
						player.put("id", id);
						player.put("score_raw", Integer.toString(score));
						player.put("score_normalized", normalizedScore);
						player_map.put(player);
					}
					
					else if(status == 0){
						player.put("id", id);
						player.put("score_raw", "INACTIVE");
						player.put("score_normalized", 0);
						player_map.put(player);
					}
					else if(status == 1){
						player.put("id", id);
						player.put("score_raw", "WD");
						player.put("score_normalized", 0);
						player_map.put(player);
					}
					else if(status == 2){
						player.put("id", id);
						player.put("score_raw", "DQ");
						player.put("score_normalized", 0);
						player_map.put(player);
					}
					else if(status == 3){
						player.put("id", id);
						player.put("score_raw", "CUT");
						player.put("score_normalized", 0);
						player_map.put(player);
					}		
				}
			}
		}
		//there are contest rules so its a fantasy points contest
		else{
			log("updating option table for multi-stat contests...");
			while(playerScores.next()){
				JSONObject player = new JSONObject();
				String id = playerScores.getString(1);
				JSONObject data = new JSONObject(playerScores.getString(2));
				//int status = playerScores.getInt(3);
				int score_db = playerScores.getInt(4);
				String data_to_display = "";
				Double points = 0.0;
				Iterator<?> keys = scoring_rules.keys();
				while(keys.hasNext()){
					String key = (String) keys.next();
					int multiplier = scoring_rules.getInt(key);
					
					// deal with top-score separately
					if(key.equals("top-score")){
						if(when.equals("tournament")){
							int top_score = getTopScore(when);
							if(score_db == top_score){
								points += ((double) multiplier);
								data_to_display += key.toUpperCase() + ", ";
							}
						}
						else{
							int score;
							try{
								score = data.getJSONObject(when).getInt("today");
								int top_score = getTopScore(when);
								if(score == top_score){
									points += ((double) multiplier);
									data_to_display += key.toUpperCase() + ", ";
								}
							}catch(JSONException e){
							}	
						}
					}
					else{
						if(when.equals("tournament")){
							int total_shots = 0;
							for(int i = 1; i <= 4; i++){
								total_shots += data.getJSONObject(String.valueOf(i)).getInt(key);
							}
							data_to_display += key.toUpperCase() + ": " + String.valueOf(total_shots) + ", ";
							points += ((double) (total_shots * multiplier));
						}else{
							data_to_display += key.toUpperCase() + ": " + String.valueOf(data.getJSONObject(when).getInt(key)) + ", ";
							points += ((double) (data.getJSONObject(when).getInt(key) * multiplier));		
						}
					}
				}
				// chop off ", " from end of string
				data_to_display = data_to_display.substring(0, data_to_display.length() - 2);
				player.put("score_raw", data_to_display);
				player.put("score_normalized", points);
				player.put("id", id);
				player_map.put(player);
			}
		}
		return player_map;
	}
	
	public int normalizeScore(int score, int worstScore){
		int normalizedScore = worstScore - score + 1;
		return normalizedScore;
	}
	
	public int getTopScore(String when) throws SQLException, JSONException{
		int topScore = 999;
		//check points column in `player` table since its a tournament contest
		if(when.equals("tournament")){
			PreparedStatement best_score = sql_connection.prepareStatement("select points from player where sport_type=? and points > -200 and gameID = ? order by points ASC limit 1");
			best_score.setString(1, this.sport);
			best_score.setString(2, this.getTourneyID());
			
			ResultSet res = best_score.executeQuery();
			while(res.next()){
				topScore = res.getInt(1);
			}
			return topScore;
		}
		//need to loop through all players scores since its a round contest
		else{
			topScore = 999;
			ResultSet all_player_scores = db.getPlayerScores(this.sport, this.gameIDs);
			while(all_player_scores.next()){
				JSONObject scores = new JSONObject(all_player_scores.getString(2));
				try{
					int today = scores.getJSONObject(when).getInt("today");
					if(today < topScore)
						topScore = today;
				}catch(JSONException e){
					continue;
				}
			}
			return topScore;
		}
	}
	
	public JSONObject createTeamsPariMutuel(JSONObject contest, JSONObject prop_data, Long deadline) throws JSONException{
		String title = this.getTourneyName() + " | Full Tournament | " + contest.getString("title");
		contest.put("title", title);
		contest.put("registration_deadline", deadline);
		int num_teams = prop_data.getInt("num_teams");
		int players_per_team = prop_data.getInt("players_per_team");
		int limit = num_teams * players_per_team;
		
		Map<Integer, ArrayList<String>> players = new HashMap<>();
		ResultSet top_players = null;
		try {
			PreparedStatement get_players = sql_connection.prepareStatement("select id, name, team_abr from player where sport_type=? and gameID = ? order by salary DESC limit ?");
			get_players.setString(1, this.sport);
			get_players.setString(2, this.getTourneyID());
			get_players.setInt(3, limit);
			top_players = get_players.executeQuery();
			
			while(top_players.next()){
				int row = top_players.getRow();
				String id = top_players.getString(1);
				String name = top_players.getString(2);
				String country = top_players.getString(3);
				ArrayList<String> golfer = new ArrayList<>();
				golfer.add(id);
				golfer.add(name + " " + country);
				players.put(row, golfer);
			}
			
			JSONArray teams = new JSONArray();
			int rounds = players_per_team;
			//create 5 teams, 6 rounds draft 
			for(int i=1; i<=num_teams; i++){
				JSONObject team = new JSONObject();
				team.put("id", i);
				ArrayList<String> names = new ArrayList<>();
				JSONArray golfer_ids = new JSONArray();
				for(int round=1; round<=rounds; round++){
					int pick;
					//snake draft picking algorithm
					if(round % 2 == 0)
						pick = (round * num_teams) - i + 1;
					else
						pick = ((round - 1) * num_teams) + i;
					
					ArrayList<String> golfer = players.get(pick);
					String id = golfer.get(0);
					golfer_ids.put(id);
					String name = golfer.get(1);
					names.add(name);
				}
				
				String desc = "";
				for(int p=0; p<rounds; p++){
					desc += names.get(p) + ", ";
				}
				desc = desc.substring(0, desc.length()-2);
				
				team.put("description", desc);
				team.put("player_ids", golfer_ids);
				teams.put(team);
			}
			JSONObject tie = new JSONObject();
			tie.put("id", num_teams+1);
			tie.put("description", "Tie");
			teams.put(tie);
			
			contest.put("option_table", teams);
		}
		catch(Exception e){
			log(e.toString());
			log(e.getMessage());
		}
		
		return contest;
	}
	

	public void createGolfRosterContest( JSONObject contest, String when) throws JSONException, SQLException{
		JSONObject prop_data = new JSONObject(contest.get("prop_data").toString());
		if(prop_data.getString("when").equals(when)){
			String title = "";
			if(when.equals("tournament"))
				title = this.getTourneyName() + " | Full Tournament | " + contest.getString("title");
			else
				title = this.getTourneyName() + " | Round " + when + " | " + contest.getString("title");
		
			contest.put("title", title);
			contest.put("odds_source", "n/a");
			contest.put("gameIDs", this.getTourneyID());
			if(when.equals("tournament") || when.equals("1"))
				contest.put("registration_deadline", this.getDeadline());
			else
				contest.put("registration_deadline", (this.getDeadline() + ((Integer.parseInt(when) - 1) * 86400000)));
			
			double weight = this.getNormalizationWeight(prop_data.getDouble("top_player_salary"), contest.getInt("salary_cap"));
			
			JSONArray option_table = db.getGolfRosterOptionTable(this.getTourneyID(), weight);
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
				log(e.toString());
				log(e.getMessage());
			}	
		}
	}
	
	
	public int createGolfPropBet( JSONObject contest, String when) throws JSONException, SQLException{
		
		JSONObject prop_data = new JSONObject(contest.get("prop_data").toString());
		
		if(prop_data.getString("when").equals(when)){
			contest.put("odds_source", "n/a");
			contest.put("gameIDs", this.getTourneyID());
			
			if(when.equals("tournament") || when.equals("1"))
				contest.put("registration_deadline", this.getDeadline());
			else
				contest.put("registration_deadline", (this.getDeadline() + ((Integer.parseInt(when) - 1) * 86400000)));
           
			switch(prop_data.getString("prop_type")){
			
				case "MOST":
				case "TOP_SCORE":
					
					JSONArray option_table = new JSONArray();
					PreparedStatement get_players;
					if(when.equals("tournament") || when.equals("1")){
						get_players = sql_connection.prepareStatement("select id, name, team_abr from player where sport_type = ? and gameID = ? and filter_on = 4 order by salary desc limit 20");
					}else{
						get_players = sql_connection.prepareStatement("select id, name, team_abr from player where sport_type = ? and gameID = ? and filter_on = 4 and points > -200 order by points asc limit 20");
					}
					get_players.setString(1, this.sport);
					get_players.setString(2, this.getTourneyID());
					ResultSet players = get_players.executeQuery();
					
					JSONObject none_above = new JSONObject();
					none_above.put("description", "Any Other Golfer");
					none_above.put("id", 1);
					option_table.put(none_above);
					
					JSONObject tie = new JSONObject();
					tie.put("description", "Tie");
					tie.put("id", 2);
					option_table.put(tie);
					
					int index = 3;
					while(players.next()){
						
						JSONObject p = new JSONObject();
						p.put("id", index);
						p.put("player_id", players.getString(1));
						String name = players.getString(2);
						String name2 = Normalizer.normalize(name, Normalizer.Form.NFD);
						String nameNormalized = name2.replaceAll("[^\\p{ASCII}]", "");
						p.put("description", nameNormalized + " " + players.getString(3));
						option_table.put(p);
						index += 1;
					}
					String title = "";
					if(when.equals("tournament"))
						title = this.getTourneyName() + " | Full Tournament | " + contest.getString("title");
					else
						title = this.getTourneyName() + " | Round " + when + " | " + contest.getString("title");
					contest.put("title", title);
					contest.put("option_table", option_table);
					break;


				case "WINNER":
					option_table = new JSONArray();
					get_players = sql_connection.prepareStatement("select id, name, team_abr from player where sport_type = ? and gameID = ? and filter_on = 4 order by salary desc limit 20");
					get_players.setString(1, this.sport);
					get_players.setString(2, this.getTourneyID());
					players = get_players.executeQuery();
					
					none_above = new JSONObject();
					none_above.put("description", "Any Other Golfer");
					none_above.put("id", 1);
					option_table.put(none_above);
					
					index = 2;
					while(players.next()){
						
						JSONObject p = new JSONObject();
						p.put("id", index);
						p.put("player_id", players.getString(1));
						String name = players.getString(2);
						String name2 = Normalizer.normalize(name, Normalizer.Form.NFD);
						String nameNormalized = name2.replaceAll("[^\\p{ASCII}]", "");
						p.put("description", nameNormalized + " " + players.getString(3));
						option_table.put(p);
						index += 1;
					}
					
					if(when.equals("tournament"))
						title = this.getTourneyName() + " | Full Tournament | " + contest.getString("title");
					else
						title = this.getTourneyName() + " | Round " + when + " | " + contest.getString("title");
					contest.put("title", title);
					contest.put("option_table", option_table);
					contest.put("progressive", "TOURNWINNERMON");
					break;
				
					
				case "MAKE_CUT":
					ResultSet result_set = null;
					try {
						get_players = sql_connection.prepareStatement("select name, team_abr, id from player where sport_type = ? and gameID = ? order by salary desc limit 5");
						get_players.setString(1, this.sport);
						get_players.setString(2, this.getTourneyID());
						result_set = get_players.executeQuery();		
					}
					catch(Exception e){
						Server.exception(e);
					}
					while(result_set.next()){
						JSONObject player = new JSONObject();
						player.put("name", result_set.getString(1) + " " + result_set.getString(2));
						player.put("id", result_set.getString(3));
						contest = this.createMakeCutProp(contest, player);
						
						MethodInstance method = new MethodInstance();
						JSONObject output = new JSONObject("{\"status\":\"0\"}");
						method.input = contest;
						method.output = output;
						method.internal_caller = true;
						method.session = null;
						method.sql_connection = sql_connection;
						try{
							Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
							c.newInstance(method);
						}
						catch(Exception e){
							Server.exception(e);
						}	
					}
					break;
				
					
				case "PLAYOFF":
					
					title = this.getTourneyName() + " | Full Tournament | " + contest.getString("title");
					contest.put("title", title);

					option_table = new JSONArray(); 
					JSONObject yes = new JSONObject();
					yes.put("description", "Yes");
					yes.put("id", 1);
					option_table.put(yes);
					JSONObject no = new JSONObject();
					no.put("description", "No");
					no.put("id", 2);
					option_table.put(no);
					contest.put("option_table", option_table);
					break;
				
					
				case "TEAM_SNAKE":
					contest = this.createTeamsPariMutuel(contest, prop_data, this.getDeadline());
					break;
					
				default:
					return 0;
			}
			
			if(!prop_data.getString("prop_type").equals("MAKE_CUT")){
				MethodInstance method = new MethodInstance();
				JSONObject output = new JSONObject("{\"status\":\"0\"}");
				method.input = contest;
				method.output = output;
				method.internal_caller = true;
				method.session = null;
				method.sql_connection = sql_connection;
				try{
					Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
					c.newInstance(method);
				}
				catch(Exception e){
					Server.exception(e);
				}	
			}
		}
		
		// CREATE A WINNER contest on THURS, FRI, SAT (all of them settle sunday)
		else if(prop_data.getString("prop_type").equals("WINNER") && !when.equals("1")){
			
			JSONArray option_table = new JSONArray();
			PreparedStatement get_players;	
			get_players = sql_connection.prepareStatement("select id, name, team_abr from player where sport_type = ? and gameID = ? and filter_on = 4 and points > -200 order by points asc limit 20");
			get_players.setString(1, this.sport);
			get_players.setString(2, this.getTourneyID());
			
			ResultSet players = get_players.executeQuery();
			
			String progressive = "";
			switch(when){
				case "2":
					progressive = "TOURNWINNERTHURS";
					break;
				case "3":
					progressive = "TOURNWINNERFRI";
					break;
				case "4":
					progressive = "TOURNWINNERSAT";
					break;
				default:
					break;
			}
			contest.put("progressive", progressive);
			
			JSONObject none_above = new JSONObject();
			none_above.put("description", "Any Other Golfer");
			none_above.put("id", 1);
			option_table.put(none_above);
	
			int index = 2;
			while(players.next()){
				
				JSONObject p = new JSONObject();
				p.put("id", index);
				p.put("player_id", players.getString(1));
				String name = players.getString(2);
				String name2 = Normalizer.normalize(name, Normalizer.Form.NFD);
				String nameNormalized = name2.replaceAll("[^\\p{ASCII}]", "");
				p.put("description", nameNormalized + " " + players.getString(3));
				option_table.put(p);
				index += 1;
			}
			
			String title = this.getTourneyName() + " | Full Tournament | " + contest.getString("title");
			contest.put("title", title);
			contest.put("option_table", option_table);
			contest.put("registration_deadline", (this.getDeadline() + ((Integer.parseInt(when) - 1) * 86400000)));
			contest.put("odds_source", "n/a");
			contest.put("gameIDs", this.getTourneyID());
			
			MethodInstance method = new MethodInstance();
			JSONObject output = new JSONObject("{\"status\":\"0\"}");
			method.input = contest;
			method.output = output;
			method.internal_caller = true;
			method.session = null;
			method.sql_connection = sql_connection;
			try{
				Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
				c.newInstance(method);
			}
			catch(Exception e){
				Server.exception(e);
			}	
		}
		
		// CREATE A PLAYOFF contest on THURS, FRI, SAT (all of them settle sunday)
		else if(prop_data.getString("prop_type").equals("PLAYOFF") && !when.equals("1")){
			
			String title = this.getTourneyName() + " | Full Tournament | " + contest.getString("title");
			contest.put("title", title);

			JSONArray option_table = new JSONArray(); 
			JSONObject yes = new JSONObject();
			yes.put("description", "Yes");
			yes.put("id", 1);
			option_table.put(yes);
			JSONObject no = new JSONObject();
			no.put("description", "No");
			no.put("id", 2);
			option_table.put(no);
			contest.put("option_table", option_table);
			contest.put("registration_deadline", (this.getDeadline() + ((Integer.parseInt(when) - 1) * 86400000)));
			contest.put("odds_source", "n/a");
			contest.put("gameIDs", this.getTourneyID());
			
			MethodInstance method = new MethodInstance();
			JSONObject output = new JSONObject("{\"status\":\"0\"}");
			method.input = contest;
			method.output = output;
			method.internal_caller = true;
			method.session = null;
			method.sql_connection = sql_connection;
			try{
				Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
				c.newInstance(method);
			}
			catch(Exception e){
				Server.exception(e);
			}	
		}
		return 1;
	}
	
	
	public int settlePropBet(JSONObject contest, String contest_id) throws JSONException, SQLException, IOException{
		
		JSONObject scoring_rules = contest.getJSONObject("scoring_rules");
		JSONObject prop_data = contest.getJSONObject("prop_data");
		JSONArray option_table = contest.getJSONArray("option_table");
		
		String url = "https://statdata.pgatour.com/r/" + this.getTourneyID() + "/" + this.year + "/leaderboard-v2.json";
		JSONObject leaderboard = JsonReader.readJsonFromUrl(url);
		
		int winning_outcome = 0;
		
		switch(prop_data.getString("prop_type")){
		
			case "WINNER":
				log("settling winner prop...");
				String winner_id = leaderboard.getJSONObject("leaderboard").getJSONArray("players").getJSONObject(0).getString("player_id");
				for(int i = 0; i < option_table.length(); i++){
					try{
						JSONObject option = option_table.getJSONObject(i);
						String player_id = option.getString("player_id");
						if(player_id.equals(winner_id)){
							winning_outcome = option.getInt("id");
							return winning_outcome;
						}
					}catch(Exception e){
						continue;
					}
				}
				return 1; // must be Any Other Golfer (1)
				
			
			case "PLAYOFF":
				log("settling will there be a playoff prop...");
				int first = leaderboard.getJSONObject("leaderboard").getJSONArray("players").getJSONObject(0).getInt("total_strokes");
				int second = leaderboard.getJSONObject("leaderboard").getJSONArray("players").getJSONObject(1).getInt("total_strokes");
				if(first == second){
					winning_outcome = 1;
				}
				else
					winning_outcome = 2;
				
				return winning_outcome;
				
			
			case "TOP_SCORE":
				log("settling top score prop...");
				int top_score = this.getTopScore(prop_data.getString("when"));
				ResultSet players = db.getPlayerScoresAndStatus(this.sport, this.getTourneyID());
				
				ArrayList<String> best_players = new ArrayList<String>();
				
				if(prop_data.getString("when").equals("tournament")){
					while(players.next()){
						if(players.getInt(4) == top_score){
							best_players.add(players.getString(1));
						}
					}
				}
				else{
					while(players.next()){
						int score;
						JSONObject data = new JSONObject(players.getString(2));
						try{
							score = data.getJSONObject(prop_data.getString("when")).getInt("today");
							if(score == top_score){
								best_players.add(players.getString(1));
							}
						}catch(JSONException e){
						}	
					}
				}
				
				winning_outcome = 1;
				if(best_players.size() > 1){
					//tie is correct answer;
					winning_outcome = 2;
					return winning_outcome;
				}
				else{
					for(String player_table_ID : best_players){
						for (int i=0; i<option_table.length(); i++){
							JSONObject option = option_table.getJSONObject(i);
							int option_id = option.getInt("id");
							try{
								String player_id = option.getString("player_id");
								if(player_id.equals(player_table_ID)){
									winning_outcome = option_id;
									return winning_outcome;
								}
							}	
							catch(Exception e){
								continue;
							}			
						}
					}
					return winning_outcome;
				}
				
			
			case "MAKE_CUT":
				
				String player_id = prop_data.getString("player_id");
				int status = db.getGolferStatus(player_id, this.sport, this.getTourneyID());
				if(status == 4)
					winning_outcome = 1;
				else
					winning_outcome = 2;
				return winning_outcome;
				
			
			case "MOST":
				log(contest_id);
				double max_points = -999.0;
				ArrayList<String> top_players = new ArrayList<String>();
				winning_outcome = 1;
				ResultSet all_players = null;
				log("gameID: " + this.getTourneyID());
				try{
					all_players = db.getPlayerScoresAndStatus(this.sport, this.getTourneyID());
				}catch(Exception e){
					Server.exception(e);
				}
				// loop through players and compile ArrayList<String> of player_ids with top score
				while(all_players.next()){
					player_id = all_players.getString(1);
					JSONObject data = new JSONObject(all_players.getString(2));
					int score = all_players.getInt(4);
					log(player_id + ": " + data.toString());
					double points = this.calculateMultiStatPoints(prop_data.getString("when"), data, score, scoring_rules);
					if(points > max_points){
						max_points = points;
						top_players.clear();
						top_players.add(player_id);
						log("clearing top_players");
						log("adding " + player_id + " to top_players array with points = " + points);
					}
					else if(points == max_points){
						log("adding " + player_id + " to top_players array with points = " + points);
						top_players.add(player_id);
					}
				}
				
				log("TOP PLAYERS: " + top_players.toString());
			
				if(top_players.size() >= 2){
					//tie is correct answer;
					winning_outcome = 2;
					log("winning outcome=2 because of tie");
					return winning_outcome;
				}
				else{
					for(String player_table_ID : top_players){
						for (int i=0; i<option_table.length(); i++){
							JSONObject option = option_table.getJSONObject(i);
							int option_id = option.getInt("id");
							try{
								player_id = option.getString("player_id");
								if(player_id.equals(player_table_ID)){
									winning_outcome = option_id;
									log("winning outcome is " + option.getString("description"));
									return winning_outcome;
								}
							}	
							catch(Exception e){
								continue;
							}			
						}
					}
				}
				return winning_outcome;	// any other golfer
			
			case "TEAM_SNAKE":
				
				log("snake draft " + contest_id);
				double top_score_team = -999;
				winning_outcome = 0;
				for(int i = 0; i < option_table.length(); i++){
					try{
						double team_score = 0.0;
						JSONObject option = option_table.getJSONObject(i);
						log("calculating scores from team with id = " + option.getInt("id"));
						JSONArray golfers =  option.getJSONArray("player_ids");
						for(int q = 0; q < golfers.length(); q++){
							String id = golfers.getString(q);
							log("golfer: " + id);
							ResultSet player_data = db.getPlayerScoresData(id, this.sport, this.getTourneyID());
							int overall_score = 0;
							JSONObject data = new JSONObject();
							if(player_data.next()){
								data = new JSONObject(player_data.getString(1));
								log(data.toString());
								overall_score = player_data.getInt(2);
								log(overall_score);
							}
							double pts = this.calculateMultiStatPoints(prop_data.getString("when"), data, overall_score, scoring_rules);
							log("golfer pts: " + pts);
							team_score += pts;
						}
					
						log("team " + option.getInt("id") + " score: " + team_score);
						if(team_score > top_score_team){
							top_score_team = team_score;
							winning_outcome = option.getInt("id");
						}
						else if(team_score == top_score_team){
							winning_outcome = 6;
						}
					}catch(Exception e){
						continue;
					}
				}
				log("Winning team: " + winning_outcome);
				return winning_outcome;
			
			
			case "MATCH_PLAY":	
				log("match play " + contest_id);
				// Multi stat match play
				if(prop_data.getString("multi_stp").equals("multi-stat")){
					double most_pts = -100;
					top_players = new ArrayList<String>();
					for(int i = 0; i < option_table.length(); i++){
						try{
							player_id = option_table.getJSONObject(i).getString("player_id");
							ResultSet rs = db.getPlayerScoresData(player_id, this.sport, this.getTourneyID());
							if(rs.next()){
								JSONObject data = new JSONObject(rs.getString(1));
								int overall_score = rs.getInt(2);
								Double pts = this.calculateMultiStatPoints(prop_data.getString("when"), data, overall_score, scoring_rules);
								if(pts > most_pts){
									most_pts = pts;
									top_players.clear();
									top_players.add(player_id);
								}
								else if(pts == most_pts){
									top_players.add(player_id);
								}
							}
							
						}catch(Exception e){
							continue;
						}
					}
					log("top players: " + top_players.toString());

					if(top_players.size() >= 2){
						//tie is correct answer;
						winning_outcome = 0;
						log("winning outcome=0 because of tie");
						return winning_outcome;
					}
					else{
						for(String player_table_ID : top_players){
							for (int i=0; i<option_table.length(); i++){
								JSONObject option = option_table.getJSONObject(i);
								int option_id = option.getInt("id");
								try{
									player_id = option.getString("player_id");
									if(player_id.equals(player_table_ID)){
										winning_outcome = option_id;
										log("winning outcome is " + option.getString("description"));
										return winning_outcome;
									}
								}	
								catch(Exception e){
									continue;
								}			
							}
						}
					}
				}
				
				// score to par match play
				else{
					top_score = 300;
					top_players = new ArrayList<String>();
					for(int i = 0; i < option_table.length(); i++){
						try{
							player_id = option_table.getJSONObject(i).getString("player_id");
							ResultSet rs = db.getPlayerScoresData(player_id, this.sport, this.getTourneyID());
							if(rs.next()){
								int overall_score = rs.getInt(2);
								if(overall_score < top_score && overall_score > -200){
									top_score = overall_score;
									top_players.clear();
									top_players.add(player_id);
								}
								else if(overall_score == top_score && overall_score > -200){
									top_players.add(player_id);
								}
							}
							
						}catch(Exception e){
							continue;
						}
					}
					log("top players: " + top_players.toString());
					if(top_players.size() >= 2){
						//tie is correct answer;
						winning_outcome = 0;
						log("winning outcome=0 because of tie");
						return winning_outcome;
					}
					else{
						for(String player_table_ID : top_players){
							for (int i=0; i<option_table.length(); i++){
								JSONObject option = option_table.getJSONObject(i);
								int option_id = option.getInt("id");
								try{
									player_id = option.getString("player_id");
									if(player_id.equals(player_table_ID)){
										winning_outcome = option_id;
										log("winning outcome is " + option.getString("description"));
										return winning_outcome;
									}
								}	
								catch(Exception e){
									continue;
								}			
							}
						}
					}
				}

			
			case "OVER_UNDER":
				log("over under " + contest_id);
				double o_u = prop_data.getDouble("over_under_value");
				player_id = prop_data.getString("player_id");
				ResultSet rs = db.getPlayerScoresData(player_id, this.sport, this.getTourneyID());
				if(rs.next()){
					JSONObject data = new JSONObject(rs.getString(1));
					int overall_score = rs.getInt(2);
					status = rs.getInt(3);
					
					//multi-stat over under
					if(prop_data.getString("multi_stp").equals("multi-stat")){
						Double pts = this.calculateMultiStatPoints(prop_data.getString("when"), data, overall_score, scoring_rules);
						if(pts > o_u){
							//over
							winning_outcome = 1;
							return winning_outcome;
						}
						else if (pts < o_u){
							//under
							winning_outcome = 2;
							return winning_outcome;
						}
						// push
						else{
							log("Over/under contest " + contest_id + " pushed: backout initiated");
							new BackoutContest(sql_connection, Integer.parseInt(contest_id), "");
							break;
						}
					}
					// score to par over under
					else{
						// Over/under = -6 and player shoots -8: we call that UNDER
						if(status == 4){
							//player beat over/under value
							if(overall_score < o_u){
								//under
								winning_outcome = 2;
								return winning_outcome;
							}
							// player did worse than over/under
							else if(overall_score > o_u){
								//over
								winning_outcome = 1;
								return winning_outcome;
							}
							//push
							else{
								log("Over/under contest " + contest_id + " pushed: backout initiated");
								new BackoutContest(sql_connection, Integer.parseInt(contest_id), null);
								break;
							}
						}else{
							log("player " + player_id + " status: " + status + " - backing out");
							new BackoutContest(sql_connection, Integer.parseInt(contest_id), null);
							break;
						}
					}
				}
				
	
			case "NUMBER_SHOTS":
				player_id = prop_data.getString("player_id");
				String shot_type = prop_data.getString("shot");
				String when = prop_data.getString("when");
				JSONObject data = db.getPlayerScores(player_id, this.sport, this.gameIDs);
				
				int num_shots = 0;
				if(when.equals("tournament")){
					for(int i = 1; i <= 4; i++){
						num_shots += data.getJSONObject(String.valueOf(i)).getInt(shot_type);
					}
				}else{
					num_shots = data.getJSONObject(when).getInt(shot_type);
				}
				
				log("player " + player_id + " shot " + num_shots + " " + shot_type + " for " + when);
				for(int index = 1; index < option_table.length(); index++){
					JSONObject option = option_table.getJSONObject(index);
					if(option.getString("description").equals(String.valueOf(num_shots))){
						winning_outcome = option.getInt("id");
						return winning_outcome;
					}
				}
				// any other number
				winning_outcome = 1;
				return winning_outcome;

		}
		
		return 0;
	}
	
	
	public double getNormalizationWeight(double multiplier, int salary_cap) throws SQLException{
		
		double top_price = 0;
		String stmt = "select salary from player where sport_type = ? and gameID = ? and filter_on = ? order by salary DESC limit 1";
	
		PreparedStatement top_player = sql_connection.prepareStatement(stmt);
		top_player.setString(1, this.sport);
		top_player.setString(2, this.getTourneyID());
		top_player.setInt(3, 4);
		
		ResultSet top = top_player.executeQuery();
		if(top.next()){
			top_price = top.getDouble(1);
			return ((double) salary_cap * multiplier) / top_price;
		}
		else{
			log("error finding normalization weight");
			return 0;
		}
	}
	
	public double calculateMultiStatPoints(String when, JSONObject data, int overall_score, JSONObject scoring_rules) throws JSONException, SQLException{
		double points = 0.0;
		int top_score;
		Iterator<?> keys = scoring_rules.keys();
		while(keys.hasNext()){
			String key = (String) keys.next();
			int multiplier = scoring_rules.getInt(key);
			
			if(key.equals("top-score")){
				if(when.equals("tournament")){
					top_score = getTopScore(when);
					if(overall_score == top_score){
						points += ((double) multiplier);
					}
				}
				else{
					try{
						int score = data.getJSONObject(when).getInt("today");
						top_score = getTopScore(when);
						if(score == top_score){
							points += ((double) multiplier);
						}
					}catch(JSONException e){
					}	
				}
			}
			else{
				if(when.equals("tournament")){
					int total_shots = 0;
					for(int i = 1; i <= 4; i++){
						total_shots += data.getJSONObject(String.valueOf(i)).getInt(key);
					}
					points += ((double) (total_shots * multiplier));
				}else{
					points += ((double) (data.getJSONObject(when).getInt(key) * multiplier));		
				}
			}
		}
		return points;
	}
	
	public void checkForInactives(int contest_id, int status_type) throws Exception{
		JSONArray entries = db.select_contest_entries(contest_id);
		for(int i = 0; i < entries.length(); i++){
			JSONObject entry = entries.getJSONObject(i);
			JSONArray entry_data = new JSONArray(entry.getString("entry_data"));
			List<String> existing_players = new ArrayList<String>();
			for(int ex_p = 0; ex_p < entry_data.length(); ex_p++){
				String p_id = entry_data.getJSONObject(ex_p).getString("id");
				existing_players.add(p_id);
			}
			
			String type = null;
			if(status_type == 0)
				type = "INACTIVE";
			else if(status_type == 3)
				type = "CUT";
			
			for(int q = 0; q < entry_data.length(); q++){
				log("entry existing players: " + existing_players.toString());
				JSONObject player = entry_data.getJSONObject(q);
				int status = db.getGolferStatus(player.getString("id"), this.sport, this.getTourneyID());
				
				// Roster contains INACTIVE or CUT player
				if(status == status_type){
					// get next available player (one player cheaper)
					JSONObject new_player = replaceInactivePlayer(player.getString("id"), player.getDouble("price"), existing_players);
					// remove inactive player from roster
					entry_data.remove(q);
					existing_players.remove(player.getString("id"));
					// put in new one
					entry_data.put(new_player);
					existing_players.add(new_player.getString("id"));
					
					log("replacing " + type + " rostered golfer " + player.getString("id") + " with " + new_player.toString());
					
					db.updateEntryWithActivePlayer(entry.getInt("entry_id"), entry_data.toString());
					String title = db.get_contest_title(contest_id);
					JSONObject user = db.select_user("id", entry.getString("user_id"));
					
					// email user to notify
					db.notify_user_roster_player_replacement(user.getString("username"), user.getString("email_address"), 
							db.getPlayerName(player.getString("id"), this.sport, this.getTourneyID()), db.getPlayerName(new_player.getString("id"), this.sport, this.getTourneyID()), 
							entry.getInt("entry_id"), title, contest_id, type);
					
				}
			}
		}
	}
	
	public JSONObject replaceInactivePlayer(String id, double salary, List<String> existing_players) throws SQLException, JSONException{
		JSONObject player = new JSONObject();
		String statement = "SELECT id, salary from player where id != ? and gameID = ? and id NOT in (";
		
		for(int i = 0; i < existing_players.size(); i++){
			statement += "?,";
		}
		statement = statement.substring(0, statement.length() - 1);
		statement += ") and sport_type = ? and filter_on = 4 and salary <= ? order by salary desc, name asc limit 1";
		
		PreparedStatement get_player = sql_connection.prepareStatement(statement);
		get_player.setString(1,  id);
		get_player.setString(2,  this.getTourneyID());
		
		int index = 3;
		Iterator<?> players = existing_players.iterator();
		while(players.hasNext()){
		   get_player.setString(index++, (String) players.next());
		}
		get_player.setString(index++, this.sport);
		get_player.setDouble(index++, salary);
		ResultSet rtn = get_player.executeQuery();
		if(rtn.next()){
			player.put("id", rtn.getString(1));
			player.put("price", rtn.getDouble(2));
		}
		return player;
	}
	
	
	public JSONObject createMakeCutProp(JSONObject fields, JSONObject player) throws JSONException{
		
		String title = this.getTourneyName() + " | Full Tournament | Will " + player.getString("name") + " Make the Cut?";
		fields.put("title", title);
		fields.put("description", "Place your bet on whether or not " + player.getString("name") + " will make the cut!");
		
		JSONObject prop_data = new JSONObject(fields.get("prop_data").toString());
		prop_data.put("player_id", player.getString("id"));
		fields.put("prop_data", prop_data.toString());
		
		JSONArray option_table = new JSONArray(); 
		JSONObject yes = new JSONObject();
		yes.put("description", "Yes");
		yes.put("id", 1);
		option_table.put(yes);
		JSONObject no = new JSONObject();
		no.put("description", "No");
		no.put("id", 2);
		option_table.put(no);
		fields.put("option_table", option_table);
		
		return fields;
		
	}
	
	class PricingStat{
		public String method_level = "admin";
		private boolean owgr;
		private JSONArray player_pricing;
		private Map<String, Double> player_prices;
		
		// constructor
		public PricingStat(boolean owgr, JSONArray player_pricing){
			this.owgr = owgr;
			this.player_pricing = player_pricing;
		}
		public void setPriceMap(Map<String, Double> player_prices){
			this.player_prices = player_prices;
		}
		public boolean isOWGR(){
			return this.owgr;
		}
		public JSONArray getPlayerPricingArray(){
			return player_pricing;
		}
		public Map<String, Double> getPriceMap(){
			return player_prices;
		}
	}
	
	class Player {
		public String method_level = "admin";
		private String name;
		private String country;
		private String pga_id;
		private String tourney_ID;
		private double fantasy_points = 0;
		private double salary;
		private String height;
		private String weight;
		private JSONObject bio;
		
		// constructor
		public Player(String id, String n, String tourney_ID){
			this.pga_id = id;
			this.name = n;
			this.tourney_ID = tourney_ID;
		}
	
		// methods
		public double getPoints(){
			return fantasy_points;
		}
		public void setCountry(String c){
			this.country = c;
		}
		public void setScore(double score){
			this.fantasy_points = score;
		}
		public String getCountry(){
			return country;
		}
		public String getPGA_ID(){
			return pga_id;
		}
		public String getTourneyID(){
			return tourney_ID;
		}
		public double getSalary(){
			return salary;
		}
		public String getName(){
			return name;
		}
		public String getHeight(){
			return height;
		}
		public String getWeight(){
			return weight;
		}
		public void set_ppg_salary(double pts){
			this.salary = pts;
		}
		
		public JSONObject getDashboardData(String year) throws JSONException, IOException{
			
			String weight = "", height = "", birthday = "", age = "", birthPlace = "", birthString = "", cut_percentage = "";
			Document page = Jsoup.connect("https://www.pgatour.com/players/player." + this.pga_id + ".html").userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
				      .referrer("http://www.google.com").timeout(0).get();
			
			try{
				Element outer_div = page.getElementsByClass("s-col-left").first();
				Elements divs = outer_div.getElementsByClass("s-col__row");
				try{
					for(Element d : divs){
						if(d.getElementsByTag("p").last().text().equals("Weight"))
							weight = d.getElementsByTag("p").get(0).text();
						else if(d.getElementsByTag("p").last().text().equals("Height")){
							height = d.getElementsByTag("p").get(0).text();
							height = height.replace("  ft,", "'").replace("  in", "\"");
						}
						else if(d.getElementsByTag("p").last().text().equals("Birthday"))
							birthday = d.getElementsByTag("p").get(0).text();
						else if(d.getElementsByTag("p").last().text().equals("AGE"))
							age = d.getElementsByTag("p").get(0).text();
						else if(d.getElementsByTag("p").last().text().equals("Birthplace"))
							birthPlace = d.getElementsByTag("p").get(0).text();
					}
					if(!birthday.isEmpty() && !birthPlace.isEmpty() && !age.isEmpty())
						birthString = birthday + " in " + birthPlace + " (Age: " + age + ")";
					
				}catch(Exception e){
					log(e.toString());
				}
			}catch(Exception e){
				log(e.toString());
				log(e.getMessage());
				log(this.getName() + " - " + this.pga_id + ": unable to grab bio data - most likely not available on pgatour.com");
			}
			
			JSONArray last_five_tournaments = new JSONArray();
			JSONArray stats = new JSONArray();
			
			try{
				String data_url = "https://statdata.pgatour.com/players/" + this.pga_id + "/" + year + "stat.json";
				page = Jsoup.connect(data_url).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
						.ignoreContentType(true).referrer("http://www.google.com").timeout(0).get(); 
				JSONObject data = new JSONObject(page.text().replace(" \"\" : \"\",", ""));
				JSONArray json_stats = data.getJSONArray("plrs").getJSONObject(0).getJSONArray("years").getJSONObject(0).getJSONArray("tours").getJSONObject(0).getJSONArray("statCats").getJSONObject(0).getJSONArray("stats");
				List<String> options = Arrays.asList("101", "102", "103", "105", "155", "156", "120", "186");
				for(int i = 0; i < json_stats.length(); i++){
					String statID = json_stats.getJSONObject(i).getString("statID");
					if(options.contains(statID))
						stats.put(json_stats.getJSONObject(i));
				}
			}catch(Exception e){
				log(e.toString());
				log(e.getMessage());
				log(this.getName() + " - " + this.pga_id + ": unable to get stats");
			}
			
			try{
				String results_url = "https://statdata.pgatour.com/players/" + this.pga_id + "/" + year + "results.json";
				page = Jsoup.connect(results_url).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
						.ignoreContentType(true).referrer("http://www.google.com").timeout(0).get(); 
				JSONObject results = new JSONObject(page.text().replace(" \"\" : \"\",", ""));
				JSONArray tourneys = results.getJSONArray("plrs").getJSONObject(0).getJSONArray("tours").getJSONObject(0).getJSONArray("trnDetails");
				int num_tourneys = tourneys.length();
				for(int i = num_tourneys - 1; i >= (num_tourneys - 5) && i >= 0; i--){
					last_five_tournaments.put(tourneys.getJSONObject(i));	
				}
				try{
					JSONObject totals = results.getJSONArray("plrs").getJSONObject(0).getJSONArray("tours").getJSONObject(0).getJSONObject("totals");
					double cuts_made = Double.parseDouble(totals.getString("cutsMade"));
					double cuts_missed = Double.parseDouble(totals.getString("cutsMissed"));
					if(cuts_missed != 0){
						double cuts_per = (cuts_made / (cuts_made + cuts_missed)) * 100;
						cut_percentage = String.valueOf(cuts_per);
						JSONObject cuts = new JSONObject();
						cuts.put("name", "Cuts Made Percentage");
						cuts.put("value", cut_percentage);
						stats.put(cuts);
					}
				}catch(Exception e){}
				
			}catch(Exception e){
				log(e.toString());
				log(e.getMessage());
				log(this.getName() + " - " + this.pga_id + ": unable to get past tournament results");
			}
			
			JSONObject bio = new JSONObject();
			bio.put("birthString", birthString);
			bio.put("country_abr", this.getCountry());
			bio.put("height", height);
			bio.put("weight", weight);
			bio.put("last_five_tournaments", last_five_tournaments);
			bio.put("stats", stats);
			bio.put("pga_id", this.getPGA_ID());
			bio.put("name", this.getName());
			this.bio = bio;	
			return bio;
		}
		
		public JSONObject getBio(){
			return this.bio;
		}
	}
}
