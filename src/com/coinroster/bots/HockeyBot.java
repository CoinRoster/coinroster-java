package com.coinroster.bots;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import com.coinroster.DB;
import com.coinroster.Server;
import com.coinroster.Utils;
import com.coinroster.internal.JsonReader;
import com.coinroster.internal.scrapeHTML;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HockeyBot extends Utils {
	
	public static String method_level = "admin";

	// instance variables
	protected ArrayList<String> game_IDs;
	protected JSONArray games;
	private Map<String, Player> players_list;
	private long earliest_game;
	public String sport = "HOCKEY";
	private DB db;
	private static Connection sql_connection = null;
	
	// constructor
	public HockeyBot(Connection sql_connection) throws IOException, JSONException{
		HockeyBot.sql_connection = sql_connection;
		this.db = new DB(sql_connection);

	}
	// methods
	public ArrayList<String> getGameIDs(){
		return game_IDs;
	}
	
	public long getEarliestGame(){
		return earliest_game;
	}
	
	public JSONArray getGames(){
		return games;
	}
	
	public String scrapeGameIDs() throws IOException, JSONException, InterruptedException, ParseException{
		ArrayList<String> gameIDs = new ArrayList<String>();
		JSONArray games = new JSONArray();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYYMMdd");
		String today = LocalDate.now().format(formatter);
		
		int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		// get previous days' gamesIDs because its past midnight
		if(hour >= 0 && hour <= 4){
			today = LocalDate.now().minusDays(1).format(formatter);
			log("subtracting one day because its past 12am --> date grabbing: " + today);
		}
		
		SimpleDateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
		JSONObject json = JsonReader.readJsonFromUrl("http://site.api.espn.com/apis/site/v2/sports/hockey/nhl/scoreboard?lang=en&region=us&calendartype=blacklist&limit=100&dates=" + today + "&tz=America%2FNew_York");
		JSONArray events = json.getJSONArray("events");
		if(events.length() == 0){
			this.game_IDs = null;
			this.games = null;
			return null;
		}
		else{
			boolean ready = false;
			int index = 0;
			String earliest_date = null;
			while(!ready){
				
				earliest_date = events.getJSONObject(index).getString("date");
				// check if suspended
				if(events.getJSONObject(index).getJSONObject("status").getJSONObject("type").getString("name").equals("STATUS_SUSPENDED"))
					index++;
				else
					ready = true;
	        }
	        try {
	            Date date = formatter1.parse(earliest_date.replaceAll("Z$", "+0000"));
	            long milli = date.getTime();
	            this.earliest_game = milli;
	        } 
	        catch (ParseException e) {
	            Server.exception(e);
	        }

			for(int i=index; i < events.length(); i++){
				JSONObject game = events.getJSONObject(i);
				String name = game.getString("shortName");
				Date game_date = formatter1.parse(game.getString("date").replaceAll("Z$", "+0000"));
				Long game_milli = game_date.getTime();
				JSONArray links = game.getJSONArray("links");

				String href = links.getJSONObject(0).getString("href");
				String gameID = null;
				if(href.contains("=")){
					gameID = href.split("=")[1].replace("&sourceLang", "");
				}else{
					gameID = href.split("/")[7].replace("&sourceLang", "");
				}
				
				JSONObject game_obj = new JSONObject();
				game_obj.put("name", name);
				game_obj.put("date_milli", game_milli);
				game_obj.put("gameID", gameID);		
				games.put(game_obj);
				
				gameIDs.add(gameID.toString());
			}
			this.game_IDs = gameIDs;
			this.games = games;
			return gameIDs.toString();
		}
	}
	
	public Map<String, Player> getPlayerHashMap(){
		return players_list;
	}
	
	public JSONObject createPariMutuel(Long deadline, String date, JSONObject contest) throws JSONException{
		
		JSONArray option_table = new JSONArray(); 
		ResultSet top_players = null;
		
		JSONObject none_above = new JSONObject();
		none_above.put("description", "Any Other Player");
		none_above.put("id", 1);
		option_table.put(none_above);
		
		JSONObject tie = new JSONObject();
		tie.put("description", "Tie");
		tie.put("id", 2);
		option_table.put(tie);
	
		try {
			String stmt = "select id, name, team_abr from player where sport_type = ? and gameID in (";
			
			for(int i = 0; i < this.getGameIDs().size(); i++){
				stmt += "?,";
			}
			stmt = stmt.substring(0, stmt.length() - 1);
			stmt += ") order by salary DESC limit ?";
			
			PreparedStatement get_players = sql_connection.prepareStatement(stmt);
			
			get_players.setString(1, this.sport);
			int index = 2;
			for(String game : this.getGameIDs()){
			   get_players.setString(index++, game); // or whatever it applies 
			}
			get_players.setInt(index++, contest.getInt("filter"));
			top_players = get_players.executeQuery();
			
			int p_index = 3;
			while(top_players.next()){
				JSONObject player = new JSONObject();
				player.put("description", top_players.getString(2) + " " + top_players.getString(3));
				player.put("id", p_index);
				player.put("player_id", top_players.getString(1));
				option_table.put(player);
				p_index += 1;
			}
			contest.put("option_table", option_table);
		}
		catch(Exception e){
			Server.exception(e);
		}
		
		return contest;
	}
	
	public JSONObject settlePariMutuel(int contest_id, JSONObject scoring_rules, JSONObject prop_data, JSONArray option_table, ArrayList<String> gameIDs) throws Exception{
		
		JSONObject fields = new JSONObject();
		fields.put("contest_id", contest_id);
		String prop_type = prop_data.getString("prop_type");
		int winning_outcome;
		
		switch(prop_type){
		
			case "MOST": 
				double max_points = -999.0;
				ArrayList<String> top_players = new ArrayList<String>();
				winning_outcome = 1;
				ResultSet all_players = null;
				try{
					all_players = db.getPlayerScores(this.sport, gameIDs);
				}catch(Exception e){
					log(e.toString());
				}
				// loop through players and compile ArrayList<Integer> of player_ids with top score
				while(all_players.next()){
					String player_id = all_players.getString(1);
					JSONObject data = new JSONObject(all_players.getString(2));
					Double points = 0.0;
					Iterator<?> keys = scoring_rules.keys();
					while(keys.hasNext()){
						String key = (String) keys.next();
						double multiplier = scoring_rules.getDouble(key);
						points += (double) data.getInt(key) * multiplier; 
					}
					if(points > max_points){
						max_points = points;
						top_players.clear();
						top_players.add(player_id);
						log("adding " + player_id + " to top_players array with points = " + points);
					}
					else if(points == max_points){
						top_players.add(player_id);
					}
				}
				
				log("TOP PLAYERS: " + top_players.toString());
			
				if(top_players.size() >= 2){
					//tie is correct answer;
					winning_outcome = 2;
					log("winning outcome=2 because of tie");
					fields.put("winning_outcome", winning_outcome);
					return fields;
				}
				else{
					for(String player_table_ID : top_players){
						for (int i=0; i<option_table.length(); i++){
							JSONObject option = option_table.getJSONObject(i);
							int option_id = option.getInt("id");
							try{
								String player_id = option.getString("player_id");
								if(player_id.equals(player_table_ID)){
									winning_outcome = option_id;
									fields.put("winning_outcome", winning_outcome);
									log("winning outcome is " + option.getString("description"));
									return fields;
								}
							}	
							catch(Exception e){
								continue;
							}			
						}
					}
				}
				fields.put("winning_outcome", winning_outcome);
				log("winning outcome is any other player");
				return fields;
		
			case "MATCH_PLAY":
				max_points = -999.0;
				top_players = new ArrayList<String>();
				for(int i = 0; i < option_table.length(); i++){
					JSONObject player = option_table.getJSONObject(i);
					try{
						String player_id = player.getString("player_id");
					
						JSONObject player_data = db.getPlayerScores(player_id, this.sport, gameIDs);
						Double points = 0.0;
						Iterator<?> keys = scoring_rules.keys();
						while(keys.hasNext()){
							String key = (String) keys.next();
							double multiplier = scoring_rules.getDouble(key);
							points += (double) player_data.getInt(key) * multiplier;
						}
						log(player_id + ": " + points);
						if(points > max_points){
							max_points = points;
							top_players.clear();
							top_players.add(player_id);
						}
						else if(points == max_points){
							top_players.add(player_id);
						}
					}
					catch(JSONException e){
						continue;
					}
				}
				if(top_players.size() >= 2){
					winning_outcome = 0;
					fields.put("winning_outcome", winning_outcome);
					return fields;
				}
				else{
					String winner_id = top_players.get(0);
					log("player id of winner: " + winner_id);
					for(int i=0; i<option_table.length(); i++){
						JSONObject option = option_table.getJSONObject(i);
						int option_id = option.getInt("id");
						try{
							String p_id = option.getString("player_id");
							if(winner_id.equals(p_id)){
								winning_outcome = option_id;
								fields.put("winning_outcome", winning_outcome);
								log("winning outcome is " + option.getString("description"));
								return fields;
							}
						}	
						catch(Exception e){
							continue;
						}			
					}
				}
				break;
			
			case "OVER_UNDER":
				String player_id = prop_data.getString("player_id");
				JSONObject player_data = db.getPlayerScores(player_id, this.sport, gameIDs);
				Double points = 0.0;
				Iterator<?> keys = scoring_rules.keys();
				while(keys.hasNext()){
					String key = (String) keys.next();
					double multiplier = scoring_rules.getDouble(key);
					points += (double) player_data.getInt(key) * multiplier;
				}
				double o_u = prop_data.getDouble("over_under_value");
				//over = 1, under = 2
				if(points > o_u)
					winning_outcome = 1;
				else
					winning_outcome = 2;

				fields.put("winning_outcome", winning_outcome);
				break;
			
			case "TEAM_SNAKE":
				
				double top_score_team = -999;
				winning_outcome = 0;
				for(int i = 0; i < option_table.length(); i++){
					try{
						double team_score = 0.0;
						JSONObject option = option_table.getJSONObject(i);
						log("calculating scores from team with id = " + option.getInt("id"));
						JSONArray players =  option.getJSONArray("player_ids");
						for(int q = 0; q < players.length(); q++){
							String id = players.getString(q);
							JSONObject data = db.getPlayerScores(id, this.sport, gameIDs);
							points = 0.0;
							keys = scoring_rules.keys();
							while(keys.hasNext()){
								String key = (String) keys.next();
								double multiplier = scoring_rules.getDouble(key);
								points += (double) data.getInt(key) * multiplier;
							}
							team_score += points;
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
				fields.put("winning_outcome", winning_outcome);
				break;
				
			default:
				log("error: prop_type not supported");
				break;
		}
		return fields;
	}
	
	
	public JSONObject createTeamsPariMutuel(JSONObject contest, JSONObject prop_data) throws JSONException{
		
		int num_teams = prop_data.getInt("num_teams");
		int players_per_team = prop_data.getInt("players_per_team");
		int limit = num_teams * players_per_team;
		
		Map<Integer, ArrayList<String>> players = new HashMap<>();
		ResultSet top_players = null;
		try {
			
			String stmt = "select id, name, team_abr from player where sport_type=? and gameID in (";
			
			for(int i = 0; i < this.getGameIDs().size(); i++){
				stmt += "?,";
			}
			stmt = stmt.substring(0, stmt.length() - 1);
			stmt += ") order by salary DESC limit ?";
			
			PreparedStatement get_players = sql_connection.prepareStatement(stmt);
			
			get_players.setString(1, this.sport);
			int index = 2;
			for(String game : this.getGameIDs()){
				get_players.setString(index++, game); // or whatever it applies 
			}
			get_players.setInt(index++, limit);
			top_players = get_players.executeQuery();
			
			while(top_players.next()){
				int row = top_players.getRow();
				String id = top_players.getString(1);
				String name = top_players.getString(2);
				String team = top_players.getString(3);
				ArrayList<String> player = new ArrayList<>();
				player.add(id);
				player.add(name + " " + team);
				players.put(row, player);
			}
			
			JSONArray teams = new JSONArray();
			int rounds = players_per_team;
			for(int i=1; i<=num_teams; i++){
				JSONObject team = new JSONObject();
				team.put("id", i);
				ArrayList<String> names = new ArrayList<>();
				JSONArray player_ids = new JSONArray();
				for(int round=1; round<=rounds; round++){
					int pick;
					//snake draft picking algorithm
					if(round % 2 == 0)
						pick = (round * num_teams) - i + 1;
					else
						pick = ((round - 1) * num_teams) + i;
					
					ArrayList<String> player = players.get(pick);
					String id = player.get(0);
					player_ids.put(id);
					String name = player.get(1);
					names.add(name);
				}
				
				String desc = "";
				for(int p=0; p<rounds; p++){
					desc += names.get(p) + ", ";
				}
				desc = desc.substring(0, desc.length()-2);
				
				team.put("description", desc);
				team.put("player_ids", player_ids);
				teams.put(team);
			}
			JSONObject tie = new JSONObject();
			tie.put("id", num_teams+1);
			tie.put("description", "Tie");
			teams.put(tie);
			
			contest.put("option_table", teams);
		}
		catch(Exception e){
			Server.exception(e);
		}
		
		return contest;
	}
	
	
	public void savePlayers(){
		try {
			log("closed connection? " +sql_connection.isClosed());
			
			if(sql_connection.isClosed()){
				sql_connection = Server.sql_connection();
			}
			log("valid?" + sql_connection.isValid(0));
			
//			PreparedStatement delete_old_rows = sql_connection.prepareStatement("delete from player where sport_type=?");
//			delete_old_rows.setString(1, this.sport);
//			delete_old_rows.executeUpdate();
//			log("deleted " + this.sport + " players from old contests");
			
			JSONObject empty_data_json = new JSONObject();
			empty_data_json.put("goals", 0);
			empty_data_json.put("assists", 0);
			empty_data_json.put("plus-minus", 0);
			empty_data_json.put("sog", 0);			
			empty_data_json.put("bs", 0);
			
			if(this.getPlayerHashMap() == null)
				return;
			
			for(Player player : this.getPlayerHashMap().values()){
				
				PreparedStatement save_player = sql_connection.prepareStatement("insert into player(id, name, sport_type, gameID, team_abr, salary, data, points, bioJSON) values(?, ?, ?, ?, ?, ?, ?, ?, ?)");				
				save_player.setString(1, player.getESPN_ID());
				save_player.setString(2, player.getName());
				save_player.setString(3, this.sport);
				save_player.setString(4, player.getGameID());
				save_player.setString(5, player.getTeam());
				save_player.setDouble(6, player.getSalary());
				save_player.setString(7, empty_data_json.toString());
				save_player.setDouble(8, player.getPoints());
				save_player.setString(9, player.getBio().toString());
				save_player.executeUpdate();	
			}
			log("added " + this.sport + " players to DB");
		}
		catch (Exception e) {
			Server.exception(e);
		}
	}
	
	public JSONArray updateScores(JSONObject scoring_rules, ArrayList<String> gameIDs) throws SQLException, JSONException{
		
		ResultSet playerScores = db.getPlayerScores(this.sport, gameIDs);
		JSONArray player_map = new JSONArray();
		while(playerScores.next()){
			JSONObject player = new JSONObject();
			String id = playerScores.getString(1);
			JSONObject data = new JSONObject(playerScores.getString(2));
			String data_to_display = "";
			Double points = 0.0;
			Iterator<?> keys = scoring_rules.keys();
			while(keys.hasNext()){
				String key = (String) keys.next();
				double multiplier = scoring_rules.getDouble(key);
				data_to_display += key.toUpperCase() + ": " + String.valueOf(data.getInt(key)) + ", ";
				points += (double) data.getInt(key) * multiplier;			
			}
			// chop off ", " from end of string
			if(data_to_display.contains(", "))
				data_to_display = data_to_display.substring(0, data_to_display.length() - 2);
			
			player.put("score_raw", data_to_display);
			player.put("score_normalized", points);
			player.put("id", id);

			player_map.put(player);
		}
		return player_map;
	}
	
		// setup() method creates contest by creating a hashmap of <ESPN_ID, Player> entries
		public Map<String, Player> setup() throws IOException, JSONException, SQLException, InterruptedException{
			Map<String, Player> players = new HashMap<String,Player>();
			if(this.game_IDs == null){
				log("No games scheduled");
				this.players_list = null;
				return null;
			}
			for(int i=0; i < this.game_IDs.size(); i++){
				// for each gameID, get the two teams playing
				Document page = Jsoup.connect("http://www.espn.com/nhl/game?gameId="+this.game_IDs.get(i)).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
					      .referrer("http://www.google.com").timeout(0).get();
				Elements team_divs = page.getElementsByClass("Gamestrip__Truncate");
				// for each team, go to their stats page and scrape ppg
				for(Element team : team_divs){
					String team_link = team.select("a").attr("href");
					String team_abr = team_link.split("/")[5];
					log(team_abr);
					String url  = "http://www.espn.com/nhl/team/stats/_/name/" + team_abr;
					Document team_stats_page = scrapeHTML.connect(url);
					if(team_stats_page == null){
						log("problem connecting to " + url);
						continue;
					}
					Element stats_table = team_stats_page.getElementsByClass("Table2__tbody").first();
					Elements rows = stats_table.getElementsByTag("tr");
					for (Element row : rows){
						if(row.className().contains("Table2__tr")){
							Elements cols = row.getElementsByTag("td");
							String name = cols.get(0).select("a").text();
							try{
								String ESPN_id = cols.get(0).select("a").attr("href").split("/")[7];
								Player p = new Player(ESPN_id, name, team_abr.toUpperCase());
								int rtn = p.scrape_info();
								if(rtn == 1){
									p.setPPG();
									p.gameID = this.game_IDs.get(i);
									p.set_ppg_salary(p.getPPG());
									p.createBio();		
									players.put(ESPN_id, p);
								}
							}
							catch(ArrayIndexOutOfBoundsException e){
								continue;
							}
						}
					}
				}
			}
			this.players_list = players;
			return players;
		}
		
		
		public double getNormalizationWeight(double multiplier, ArrayList<String> gameIDs, int salary_cap) throws SQLException{
			
			double top_price = 0;
			String stmt = "select salary from player where sport_type = ? and gameID in (";
			
			for(int i = 0; i < gameIDs.size(); i++){
				stmt += "?,";
			}
			stmt = stmt.substring(0, stmt.length() - 1);
			stmt += ") order by salary DESC limit 1";
		
			PreparedStatement top_player = sql_connection.prepareStatement(stmt);
			top_player.setString(1, this.sport);
			int index = 2;
			for(String game : gameIDs){
				top_player.setString(index++, game); // or whatever it applies 
			}
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
		
		public boolean scrape(ArrayList<String> gameIDs) throws IOException, SQLException{
			
			int games_ended = 0;
			for(int i=0; i < gameIDs.size(); i++){
				Document page = Jsoup.connect("http://www.espn.com/nhl/boxscore?gameId="+gameIDs.get(i)).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
					      .referrer("http://www.google.com").timeout(0).get();
				Element outer_div;
				Elements tables;
				try{
					outer_div = page.getElementsByClass("Boxscore__ResponsiveWrapper").first();
					tables = outer_div.getElementsByClass("v-top");
				}catch(NullPointerException e){
					continue;
				}
				
				//away team 
				Elements away_player_names_rows = tables.get(0).getElementsByAttribute("data-idx");
				Elements away_player_stats_rows = tables.get(1).getElementsByAttribute("data-idx");
		
				int total_rows = away_player_names_rows.size();
				for(int row = 1; row < total_rows; row++ ){
					String espn_ID_url = away_player_names_rows.get(row).select("a").attr("href");
					if(!espn_ID_url.isEmpty()){
						String espn_ID = espn_ID_url.split("/")[7];	
						Elements stats = away_player_stats_rows.get(row).getElementsByTag("td");
						String goal_string = stats.get(0).text();
						String ast_string = stats.get(1).text();
						String plus_minus_string = stats.get(2).text();
						String sog_string = stats.get(3).text();
						String bs_string = stats.get(5).text();
						
						JSONObject data = new JSONObject();
						try {
							
							if(goal_string.equals("--"))
								goal_string = "0";
							data.put("goals", Integer.parseInt(goal_string));
							
							if(ast_string.equals("--"))
								ast_string = "0";
							data.put("assists", Integer.parseInt(ast_string));
							
							if(plus_minus_string.equals("--"))
								plus_minus_string = "0";
							data.put("plus-minus", Integer.parseInt(plus_minus_string));
							
							if(sog_string.equals("--"))
								sog_string= "0";
							data.put("sog", Integer.parseInt(sog_string));
							
							if(bs_string.equals("--"))
								bs_string= "0";
							data.put("bs", Integer.parseInt(bs_string));
							
						}catch (JSONException e) {
							Server.exception(e);
						}
					
						// look up player in HashMap with ESPN_ID, update his data in DB
						db.editData(data.toString(), espn_ID, this.sport, gameIDs);	
					}
				}
				
				//home team 
				Elements home_player_names_rows = tables.get(4).getElementsByAttribute("data-idx");
				Elements home_player_stats_rows = tables.get(5).getElementsByAttribute("data-idx");
				
				total_rows = home_player_names_rows.size();
				for(int row = 1; row < total_rows; row++ ){
					String espn_ID_url = home_player_names_rows.get(row).select("a").attr("href");
					if(!espn_ID_url.isEmpty()){
						String espn_ID = espn_ID_url.split("/")[7];	
						Elements stats = home_player_stats_rows.get(row).getElementsByTag("td");
						String goal_string = stats.get(0).text();
						String ast_string = stats.get(1).text();
						String plus_minus_string = stats.get(2).text();
						String sog_string = stats.get(3).text();
						String bs_string = stats.get(5).text();
						
						JSONObject data = new JSONObject();
						try {
							
							if(goal_string.equals("--"))
								goal_string = "0";
							data.put("goals", Integer.parseInt(goal_string));
							
							if(ast_string.equals("--"))
								ast_string = "0";
							data.put("assists", Integer.parseInt(ast_string));
							
							if(plus_minus_string.equals("--"))
								plus_minus_string = "0";
							data.put("plus-minus", Integer.parseInt(plus_minus_string));
							
							if(sog_string.equals("--"))
								sog_string= "0";
							data.put("sog", Integer.parseInt(sog_string));
							
							if(bs_string.equals("--"))
								bs_string= "0";
							data.put("bs", Integer.parseInt(bs_string));
							
						}catch (JSONException e) {
							Server.exception(e);
						}
					
						// look up player in HashMap with ESPN_ID, update his data in DB
						db.editData(data.toString(), espn_ID, this.sport, gameIDs);	
					}
				}
				
	            //check to see if contest is finished - if all games are deemed final
				String time_left = page.getElementsByClass("Gamestrip__Time").first().text();
				if(time_left.contains("Final") || time_left.contains("Postponed")){
					games_ended += 1;
				}
				if(games_ended == gameIDs.size()){
					return true;
				}
			}
			return false;
		}


	class Player {
		public String method_level = "admin";
		private String name;
		private String team_abr;
		private String ESPN_ID;
		private double fantasy_points = 0;
		private double ppg;
		private double salary;
		private String birthString;
		private String height;
		private String weight;
		private String pos;
		private JSONArray last_five_games;
		private JSONObject career_stats;
		private JSONObject year_stats;
		private JSONObject bio;
		private String gameID;
		
		// constructor
		public Player(String id, String n, String team){
			this.ESPN_ID = id;
			this.name = n;
			this.team_abr = team;
		}
		
		// methods
	
		public double getPoints(){
			return fantasy_points;
		}
		public String getESPN_ID(){
			return ESPN_ID;
		}
		public String getGameID(){
			return gameID;
		}
		public void setPPG() throws JSONException{
			try{
				String pts = this.getYearStats().getString("PTS");
				String gp = this.getYearStats().getString("GP");
				double ppg = 0.0;
				ppg = Double.parseDouble(pts) / Double.parseDouble(gp);
				this.ppg = ppg;
			}
			catch(java.lang.NullPointerException e){
				this.ppg = 0.0;
			}
			catch(JSONException e){
				this.ppg = 0.0;
			}
			catch(Exception e){
				this.ppg = 0.0;
			}
		}
		public double getSalary(){
			return salary;
		}
		public double getPPG(){
			return ppg;
		}
		public String getName(){
			return name;
		}
		public String getTeam(){
			return team_abr;
		}
		public JSONArray getGameLogs(){
			return last_five_games;
		}
		public JSONObject getCareerStats(){
			if(career_stats == null){
				return null;
			}
			return career_stats;
		}
		public JSONObject getYearStats(){
			if(year_stats == null){
				return null;
			}
			return year_stats;
		}
		public String getBirthString(){
			return birthString;
		}
		public String getPosition(){
			return pos;
		}
		public String getHeight(){
			return height;
		}
		public String getWeight(){
			return weight;
		}
		public void set_ppg_salary(double pts){
			if(pts < 0.2){
				this.salary = 50.0;
			}
			else{
				this.salary = Math.round(pts * 500.0);
			}
		}
		public void createBio() throws JSONException{
			JSONObject bio = new JSONObject();
			bio.put("id", this.getESPN_ID());
			bio.put("name", this.getName());
			bio.put("birthString", this.getBirthString());
			bio.put("height", this.getHeight());
			bio.put("weight", this.getWeight());
			bio.put("pos", this.getPosition());
			bio.put("last_five_games", this.getGameLogs());
			bio.put("career_stats", this.getCareerStats());
			bio.put("year_stats", this.getYearStats());
			bio.put("team_abr", this.getTeam());

			this.bio = bio;		
		}
		public JSONObject getBio(){
			return this.bio;
		}
		
		public int scrape_info() throws IOException, JSONException, InterruptedException{
			
			String url = "http://www.espn.com/nhl/player/_/id/"+ this.getESPN_ID();
			Document page = scrapeHTML.connect(url);
			if(page == null){
				return 0;
			}
			
			Element bio = page.getElementsByClass("player-bio").first();
			// parse bio-bio div to get pos, weight, height, birthString
			Element general_info;
			try{
				general_info = bio.getElementsByClass("general-info").first();
			}catch(Exception e){
				log(this.getESPN_ID() + " - " + this.getName());
				return 0;
			}
			String[] info = general_info.text().split(" ");
			String pos = info[1];
			this.pos = pos;
			String height = info[2] + " " + info[3].replace(",", "");
			String weight = info[4] + " " + info[5];
			this.height = height;
			this.weight = weight;
			String team = null;
			try{
				team = general_info.getElementsByTag("li").last().getElementsByTag("a").first().attr("href").split("/")[7].toUpperCase();
			}catch(NullPointerException e){
				log("not saving " + this.getName() + " to DB - no team");
				return 0;
			}
			if(!this.team_abr.equals(team)){
				log("not saving " + this.getName() + " to DB - not correct team");
				return 0;
			}
			
			Elements player_metadata = bio.getElementsByClass("player-metadata");
			Element item = player_metadata.first();
			Elements lis = item.children();
			String born_string = lis.first().text().replace("Born", "");
			String age = lis.get(1).text().replace("Age", " (Age: ");
			born_string += age + ")";
			this.birthString = born_string;
			
			String[] stats = {"STAT_TYPE", "GP", "G", "A", "PTS", "+/-", "PIM",	"SOG",	"Shooting %", "PPG", "PPA",	"SHG", "SHA", "GWG", "TOI/G", "PROD"};
			Elements tables = page.getElementsByClass("tablehead");
			Element stats_table;
			try{
				stats_table = tables.get(1);
			}
			catch (Exception e){
				log("No available data for " + this.getName() + " - " + this.getESPN_ID() + " so he will not be available in contests");
				return 0;
			}
			Elements stats_rows = stats_table.getElementsByTag("tr");
			try{
				for(Element row : stats_rows){
					if(row.className().contains("oddrow") || row.className().contains("evenrow")){
						JSONObject stat = new JSONObject();
						Elements cols = row.getElementsByTag("td");
						int index = 0;
						for(Element data : cols){
							stat.put(stats[index], data.text());
							index = index + 1;
						}
						
						if(stat.get("STAT_TYPE").equals("Career")){
							this.career_stats = stat;
						}
						else if(stat.get("STAT_TYPE").toString().contains("Regular")){
							this.year_stats = stat;
						}
					}
				}
			}
			catch (java.lang.ArrayIndexOutOfBoundsException e){
			}
		
			String[] game_log_stats = {"DATE","OPP","SCORE", "G", "A", "PTS", "+/-", "PIM",	"SOG",	"Shooting %", "PPG", "PPA",	"SHG", "SHA", "GWG", "TOI/G", "PROD"};
			Element game_log_table;
			try{
				game_log_table = tables.get(2);
			}
			catch(java.lang.IndexOutOfBoundsException e){
				//player does not have a year and career stats table, so his game logs is second table on page, not third
				game_log_table = tables.get(1);
			}
			Elements rows = game_log_table.getElementsByTag("tr");
			JSONArray game_logs = new JSONArray();
			for(Element row : rows){
				if(row.className().contains("oddrow") || row.className().contains("evenrow")){
					if(row.children().size() < 2){
						// skip the extra row in the game log - usually exists when player has been traded.
						continue;
					}
					JSONObject game = new JSONObject();
					Elements cols = row.getElementsByTag("td");
					int index = 0;
					for(Element data : cols){
						game.put(game_log_stats[index], data.text());
						index = index + 1;
					}
					game_logs.put(game);
				}	
			}
			this.last_five_games = game_logs;
			return 1;
		}
	}
	
}
