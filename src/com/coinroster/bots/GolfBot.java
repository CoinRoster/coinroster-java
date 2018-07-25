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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Utils;
import com.coinroster.internal.JsonReader;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.json.JSONArray;

public class GolfBot extends Utils {

	public static String method_level = "admin";
	public String year = "2018";
	protected Map<String, Player> players_list;
	private String tourneyID;
	private String tourneyName;
	private long startDate;
	private DB db;
	public String sport = "GOLF";
	private static Connection sql_connection = null;
	
	public GolfBot(Connection sql_connection) throws IOException, JSONException{
		GolfBot.sql_connection = sql_connection;
		db = new DB(sql_connection);
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
		
		String gameID = this.getLiveTourneyID();
		Map<Integer, JSONArray> contest_players = new HashMap<Integer, JSONArray>();
		ResultSet result_set = null;
		
		try {
			PreparedStatement get_contests = sql_connection.prepareStatement("select id, option_table from contest where gameIDs=? and contest_type='ROSTER' and status=1");
			get_contests.setString(1, gameID);
			result_set = get_contests.executeQuery();		
		}
		catch(Exception e){
			e.printStackTrace();
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
		ResultSet all_existing_players = db.getAllPlayerIDs(this.sport);
		while(all_existing_players.next()){
			existing_ids.add(all_existing_players.getString(1));
		}
		
		boolean flag = false;
		String url = "https://statdata.pgatour.com/r/" + gameID + "/field.json";
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
					PreparedStatement get_data = sql_connection.prepareStatement("select name, team_abr, salary from player where sport_type=? and id=?");
					get_data.setString(1, this.sport);
					get_data.setString(2, existing_id);
					ResultSet player_data = get_data.executeQuery();
					while(player_data.next()){
						String name = player_data.getString(1);
						String country = player_data.getString(2);
						double price = player_data.getDouble(3);
						JSONObject p = new JSONObject();
						p.put("id", existing_id);
						p.put("name", name + " " + country);
						p.put("count",0);
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
			
			for(int i = 0; i < players_json.length(); i++){
				JSONObject player = players_json.getJSONObject(i);
				String id = player.getString("TournamentPlayerId");
				
				if(!existing_ids.contains(id)){
					flag = true;
					String name = player.getString("PlayerName");
					String names[] = name.split(", ");
					String name_fl, country = "";
					try{
						name_fl = names[1] + " " + names[0];
					}
					catch(Exception e){
						name_fl = names[0];
					}
					String rank_url = "https://statdata.pgatour.com/r/stats/2018/186.json";
					JSONObject world_rankings_json = JsonReader.readJsonFromUrl(rank_url);
					JSONArray ranked_players = world_rankings_json.getJSONArray("tours").getJSONObject(0).getJSONArray("years").getJSONObject(0).getJSONArray("stats").getJSONObject(0).getJSONArray("details");
					boolean checked = false;
					
					double salary = 0;
					for(int q=0; q < ranked_players.length(); q++){
						JSONObject r_player = ranked_players.getJSONObject(q);
						if(id.equals(r_player.getString("plrNum"))){
							String points = r_player.getJSONObject("statValues").getString("statValue2");
							country = r_player.getJSONObject("statValues").getString("statValue5");
							if(country.isEmpty())
								country = getCountry(id);
							salary = Math.round(Double.parseDouble(points) * 10);
							if(salary < 80.0)
								salary = 80.0;
							checked = true;
							break;
						}
					}
					if(!checked){
						salary = 80.0;
					}
			
					if(!player_table_updated){
						Player p = new Player(id, name_fl, gameID);
						p.setCountry(country);
						JSONObject data = p.getDashboardData(this.year);
						// add player to player table
						PreparedStatement add_player = sql_connection.prepareStatement("INSERT INTO player (id, name, sport_type, gameID, team_abr, salary, data, points, bioJSON, filter_on) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
						add_player.setString(1, id);
						add_player.setString(2, name_fl);
						add_player.setString(3, this.sport);
						add_player.setString(4, gameID);
						add_player.setString(5, country);
						add_player.setDouble(6, salary);
						add_player.setString(7, initializeGolferScores().toString());
						add_player.setDouble(8, 0.0);
						add_player.setString(9, data.toString());
						add_player.setInt(10, 4);
						add_player.executeUpdate();
					}
					
					//create JSONObject to add to option table
					JSONObject p = new JSONObject();
					p.put("price", salary);
					p.put("count", 0);
					p.put("name", name_fl + " " + country);
					p.put("id", id);
					log("appending " + name_fl + " to contest " + contest_id );
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
		// THIS ASSUMES ITS BEING RUN ON A MONDAY
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
		
		String thursday = (String)(formattedDate.format(c.getTime()));
		c.set(Calendar.HOUR_OF_DAY, 7);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		long milli = c.getTimeInMillis();
		// parse schedule json to get tournaments
		String url = "https://statdata.pgatour.com/r/current/schedule-v2.json";
		JSONObject schedule = JsonReader.readJsonFromUrl(url);
		JSONArray tournaments = schedule.getJSONArray("years").getJSONObject(0).getJSONArray("tours").getJSONObject(0).getJSONArray("trns");

		for(int index=0; index < tournaments.length(); index++){
			JSONObject tournament = tournaments.getJSONObject(index);
			
			//check 3 things: start-date is this coming thurs, tournament is stroke play, and tournament is week's primary tournament
			if(tournament.getJSONObject("date").getString("start").equals(thursday) && tournament.getString("format").equals("Stroke") && tournament.getString("primaryEvent").equals("Y")){
				this.tourneyName = tournament.getJSONObject("trnName").getString("official");
				this.tourneyID = tournament.getString("permNum");
				this.startDate = milli;
				break;
			}
		}
	}
	
	public String getCountry(String id) throws JSONException, IOException, InterruptedException{
		Document page = Jsoup.connect("https://www.pgatour.com/players/player." + id + ".html").userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
			      .referrer("http://www.google.com").timeout(6000).get();
		Element country_img = page.getElementsByClass("country").first().getElementsByTag("span")
				.first().getElementsByTag("img").first();
		String country_code = country_img.attr("src").split("/")[7].replace(".png", "");
		return country_code;
	}
	
	public Map<String, Player> setup() throws IOException, JSONException, SQLException, InterruptedException {
		Map<String, Player> players = new HashMap<String, Player>();
		if(this.tourneyID != null){
			String url = "https://statdata.pgatour.com/r/" + tourneyID + "/field.json";
			JSONObject field = JsonReader.readJsonFromUrl(url);
			JSONArray players_json = field.getJSONObject("Tournament").getJSONArray("Players");
			for(int i = 0; i < players_json.length(); i++){
				JSONObject player = players_json.getJSONObject(i);
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
				String rank_url = "https://statdata.pgatour.com/r/stats/2018/186.json";
				JSONObject world_rankings_json = JsonReader.readJsonFromUrl(rank_url);
				JSONArray ranked_players = world_rankings_json.getJSONArray("tours").getJSONObject(0).getJSONArray("years").getJSONObject(0).getJSONArray("stats").getJSONObject(0).getJSONArray("details");
				boolean checked = false;
				String country = "";
				double salary = 0;
				for(int q=0; q < ranked_players.length(); q++){
					JSONObject r_player = ranked_players.getJSONObject(q);
					if(id.equals(r_player.getString("plrNum"))){
						String points = r_player.getJSONObject("statValues").getString("statValue2");
						country = r_player.getJSONObject("statValues").getString("statValue5");
						if(country.isEmpty())
							country = getCountry(id);
						salary = Math.round(Double.parseDouble(points) * 10);
						if(salary < 80.0)
							salary = 80.0;
						checked = true;
						break;
					}
				}
				if(!checked){
					salary = 80.0;
				}
				
				Player p = new Player(id, name_fl, tourneyID);
				p.set_ppg_salary(salary);
				p.setCountry(country);
				players.put(id, p);
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
			round.put("double-bogeys+", 0);
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
			PreparedStatement delete_old_rows = sql_connection.prepareStatement("delete from player where sport_type=?");
			delete_old_rows.setString(1, this.sport);
			delete_old_rows.executeUpdate();
			log("deleted " + this.sport + " players from old contests");

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
		
		String url = "https://statdata.pgatour.com/r/" + tourneyID + "/2018/leaderboard-v2.json";
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

		JSONArray players = leaderboard.getJSONObject("leaderboard").getJSONArray("players");
		ResultSet playerScores = db.getPlayerScores(this.sport);
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
					scores_to_edit.put("double-bogeys+", (player.getJSONObject("par_performance").getInt("double_bogeys") + player.getJSONObject("par_performance").getInt("more_bogeys")));
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
					db.updateGolferScore(player_id, this.sport, data.toString(), score, status_int);
					break;
				}
			}
			if(!in_leaderboard){
				score = -777;
				JSONObject data = new JSONObject(playerScores.getString(2));
				data.put("overall", score);
				db.updateGolferScore(id, this.sport, data.toString(), score, 0);

			}
		}
		return tournament_statuses;		
	}
	
	public int findWorstScore(String when) throws SQLException, JSONException{
		int worstScore = 0;
		if(when.equals("tournament")){
			PreparedStatement worst_score = sql_connection.prepareStatement("select points from player where sport_type=? order by points DESC limit 1");
			worst_score.setString(1, "GOLF");
			ResultSet score = worst_score.executeQuery();
			while(score.next()){
				worstScore = score.getInt(1);
			}
		}
		else{
			int worst = -999;
			ResultSet all_player_scores = db.getPlayerScores("GOLF");
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
		
		ResultSet playerScores = db.getPlayerScoresAndStatus(this.sport);
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
							log(e.getMessage());
							log("player id: " + id);
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
			PreparedStatement best_score = sql_connection.prepareStatement("select points from player where sport_type=? order by points ASC limit 1");
			best_score.setString(1, "GOLF");
			ResultSet res = best_score.executeQuery();
			while(res.next()){
				topScore = res.getInt(1);
			}
			return topScore;
		}
		//need to loop through all players scores since its a round contest
		else{
			topScore = 999;
			ResultSet all_player_scores = db.getPlayerScores("GOLF");
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
	
	public JSONObject createTeamsPariMutuel(JSONObject contest, Long deadline) throws JSONException{
		String title = this.getTourneyName() + " | " + contest.getString("title");
		contest.put("title", title);
		contest.put("registration_deadline", deadline);
		int num_teams = contest.getJSONObject("prop_data").getInt("num_teams");
		int players_per_team = contest.getJSONObject("prop_data").getInt("players_per_team");
		int limit = num_teams * players_per_team;
		//get 30 most expensive golfers
		Map<Integer, ArrayList<String>> players = new HashMap<>();
		ResultSet top_players = null;
		try {
			PreparedStatement get_players = sql_connection.prepareStatement("select id, name, team_abr from player where sport_type=? order by salary DESC limit ?");
			get_players.setString(1, "GOLF");
			get_players.setInt(2, limit);
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
				team.put("player_ids", golfer_ids.toString());
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
			String title = this.getTourneyName() + " | " + contest.getString("title");
			contest.put("title", title);
			contest.put("odds_source", "n/a");
			contest.put("gameIDs", this.getTourneyID());
			if(when.equals("tournament") || when.equals("1"))
				contest.put("registration_deadline", this.getDeadline());
			else
				contest.put("registration_deadline", (this.getDeadline() + ((Integer.parseInt(when) - 1) * 86400000)));
            JSONArray option_table = db.getGolfRosterOptionTable();
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
						get_players = sql_connection.prepareStatement("select id, name, team_abr from player where sport_type = ? and filter_on = 4 order by salary desc limit 20");
					}else{
						get_players = sql_connection.prepareStatement("select id, name, team_abr from player where sport_type = ? and filter_on = 4 order by points asc desc limit 20");
					}
					get_players.setString(1, this.sport);
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
					String title = this.getTourneyName() + " | " + contest.getString("title");
					contest.put("title", title);
					contest.put("option_table", option_table);
					break;


				case "WINNER":
					option_table = new JSONArray();
					get_players = sql_connection.prepareStatement("select id, name, team_abr from player where sport_type = ? and filter_on = 4 order by salary desc limit 20");
					get_players.setString(1, this.sport);
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
					
					title = this.getTourneyName() + " | " + contest.getString("title");
					contest.put("title", title);
					contest.put("option_table", option_table);
					break;
					
				case "MAKE_CUT":
					ResultSet result_set = null;
					try {
						get_players = sql_connection.prepareStatement("select name, team_abr, id from player where sport_type = ? order by salary desc limit 5");
						get_players.setString(1, this.sport);
						result_set = get_players.executeQuery();		
					}
					catch(Exception e){
						e.printStackTrace();
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
						method.session = null;
						method.sql_connection = sql_connection;
						try{
							Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
							c.newInstance(method);
						}
						catch(Exception e){
							Server.exception(e);
							log(e.toString());
							log(e.getMessage());
							log(e.getClass());
							log(e.getCause());
							log(e.getStackTrace().toString());
						}	
					}
					break;
					
				case "PLAYOFF":
					
					title = this.getTourneyName() + " | " + contest.getString("title");
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
					contest = this.createTeamsPariMutuel(contest, this.getDeadline());
					break;
					
				default:
					return 0;

			}
			
			if(!prop_data.getString("prop_type").equals("MAKE_CUT")){
				MethodInstance method = new MethodInstance();
				JSONObject output = new JSONObject("{\"status\":\"0\"}");
				method.input = contest;
				method.output = output;
				method.session = null;
				method.sql_connection = sql_connection;
				try{
					Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
					c.newInstance(method);
				}
				catch(Exception e){
					Server.exception(e);
					log(e.toString());
					log(e.getMessage());
					log(e.getClass());
					log(e.getCause());
				}	
			}
		}
		
		// CREATE A WINNER contest on THURS, FRI, SAT (all of them settle sunday)
		else if(prop_data.getString("prop_type").equals("WINNER") && !when.equals("1")){
			
			JSONArray option_table = new JSONArray();
			PreparedStatement get_players;	
			get_players = sql_connection.prepareStatement("select id, name, team_abr from player where sport_type = ? and filter_on = 4 order by points asc desc limit 20");
			get_players.setString(1, this.sport);
			ResultSet players = get_players.executeQuery();
			
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
				p.put("name", nameNormalized + " " + players.getString(3));
				option_table.put(p);
				index += 1;
			}
			String title = this.getTourneyName() + " | " + contest.getString("title");
			contest.put("title", title);
			contest.put("option_table", option_table);
			contest.put("registration_deadline", (this.getDeadline() + ((Integer.parseInt(when) - 1) * 86400000)));
			contest.put("odds_source", "n/a");
			contest.put("gameIDs", this.getTourneyID());
			
			MethodInstance method = new MethodInstance();
			JSONObject output = new JSONObject("{\"status\":\"0\"}");
			method.input = contest;
			method.output = output;
			method.session = null;
			method.sql_connection = sql_connection;
			try{
				Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
				c.newInstance(method);
			}
			catch(Exception e){
				log(e.toString());
				log(e.getMessage());
			}	
		}
		
		// CREATE A PLAYOFF contest on THURS, FRI, SAT (all of them settle sunday)
		else if(prop_data.getString("prop_type").equals("PLAYOFF") && !when.equals("1")){
			
			String title = this.getTourneyName() + " | " + contest.getString("title");
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
			method.session = null;
			method.sql_connection = sql_connection;
			try{
				Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
				c.newInstance(method);
			}
			catch(Exception e){
				Server.exception(e);
				log(e.toString());
				log(e.getMessage());
			}	
		}
		return 1;
	}
	
	
	public int settlePropBet(JSONObject contest) throws JSONException, SQLException, IOException{
		
		JSONObject scoring_rules = contest.getJSONObject("scoring_rules");
		JSONObject prop_data = contest.getJSONObject("prop_data");
		JSONArray option_table = contest.getJSONArray("option_table");
		
		String url = "https://statdata.pgatour.com/r/" + this.getLiveTourneyID() + "/2018/leaderboard-v2.json";
		JSONObject leaderboard = JsonReader.readJsonFromUrl(url);
		
		int winning_outcome = 0;
		
		switch(prop_data.getString("prop_type")){
		
			case "WINNER":
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
				return 1; // must be "Any Other Golfer (1)
				
			
			case "PLAYOFF":
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
				ResultSet players = db.getPlayerScoresAndStatus(this.sport);
				
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
					log("winning outcome = 2 because of tie");
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
				int status = db.getGolferStatus(player_id, this.sport);
				if(status == 4)
					winning_outcome = 1;
				else
					winning_outcome = 2;
				return winning_outcome;
				
			
			case "MOST":
				
				double max_points = -999.0;
				ArrayList<String> top_players = new ArrayList<String>();
				winning_outcome = 1;
				ResultSet all_players = null;
				try{
					all_players = db.getPlayerScoresAndStatus("GOLF");
				}catch(Exception e){
					log(e.toString());
				}
				// loop through players and compile ArrayList<Integer> of player_ids with top score
				while(all_players.next()){
					player_id = all_players.getString(1);
					JSONObject data = new JSONObject(all_players.getString(2));
					int score = all_players.getInt(4);
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
				
				double top_score_team = -999;
				winning_outcome = 0;
				for(int i = 0; i < option_table.length(); i++){
					double team_score = 0.0;
					JSONObject option = option_table.getJSONObject(i);
					log("calculating scores from team with id = " + option.getInt("id"));
					JSONArray golfers =  new JSONArray(option.getJSONArray("player_ids"));
					for(int q = 0; q < golfers.length(); q++){
						String id = golfers.getString(q);
						ResultSet player_data = db.getPlayerScoresData(id, this.sport);
						int overall_score = 0;
						JSONObject data = new JSONObject();
						if(player_data.next()){
							data = new JSONObject(player_data.getString(1));
							overall_score = player_data.getInt(2);
						}
						double pts = this.calculateMultiStatPoints(prop_data.getString("when"), data, overall_score, scoring_rules);
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
				}
				log("Winning team: " + winning_outcome);
				return winning_outcome;
			
			case "MATCH_PLAY":
			case "OVER_UNDER":
			case "NUMBER_SHOTS":
				break;

		}
		
		return 0;
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
	
	public void checkForInactives(int contest_id) throws Exception{
		JSONArray entries = db.select_contest_entries(contest_id);
		for(int i = 0; i < entries.length(); i++){
			JSONObject entry = entries.getJSONObject(i);
			JSONArray entry_data = new JSONArray(entry.getString("entry_data"));
			for(int q = 0; q < entry_data.length(); q++){
				JSONObject player = entry_data.getJSONObject(q);
				int status = db.getGolferStatus(player.getString("id"), this.sport);
				// Roster contains INACTIVE player
				if(status == 0){
					// get next available player (one player cheaper)
					JSONObject new_player = replaceInactivePlayer(player.getString("id"), player.getDouble("price"));
					// remove inactive player from roster
					entry_data.remove(q);
					// put in new one
					entry_data.put(new_player);
					log("replacing INACTIVE rostered golfer " + player.getString("id") + " with " + new_player.toString());
					String title = db.get_contest_title(contest_id);
					JSONObject user = db.select_user("id", entry.getString("user_id"));
					// email user to notify
					db.notify_user_roster_player_replacement(user.getString("username"), user.getString("email_address"), 
							db.getPlayerName(player.getString("id"), this.sport), db.getPlayerName(new_player.getString("id"), this.sport), 
							entry.getInt("entry_id"), title, contest_id);
					
				}
			}
		}
	}
	
	public JSONObject createMakeCutProp(JSONObject fields, JSONObject player) throws JSONException{
		
		String title = this.getTourneyName() + " | Will " + player.getString("name") + " Make the Cut?";
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
	
	public JSONObject replaceInactivePlayer(String id, double salary) throws SQLException, JSONException{
		JSONObject player = new JSONObject();
		PreparedStatement get_player = sql_connection.prepareStatement("SELECT id, salary from player where id != ? "
																	+ "and sport_type = ? and filter_on = 4 and salary <= ? "
																	+ "order by salary desc, name asc limit 1");
		get_player.setString(1,  id);
		get_player.setString(2, this.sport);
		get_player.setDouble(3, salary);
		ResultSet rtn = get_player.executeQuery();
		if(rtn.next()){
			player.put("id", rtn.getString(1));
			player.put("price", rtn.getDouble(2));
		}
		return player;
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
				      .referrer("http://www.google.com").timeout(12000).get();
			
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
						.ignoreContentType(true).referrer("http://www.google.com").timeout(6000).get(); 
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
						.ignoreContentType(true).referrer("http://www.google.com").timeout(6000).get(); 
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
			bio.put("Weight", weight);
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
