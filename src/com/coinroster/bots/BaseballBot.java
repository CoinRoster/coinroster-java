package com.coinroster.bots;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import com.coinroster.DB;
import com.coinroster.Server;
import com.coinroster.Utils;
import com.coinroster.internal.JsonReader;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BaseballBot extends Utils {
	
	public static String method_level = "admin";

	// instance variables
	protected ArrayList<String> game_IDs;
	protected JSONArray games;
	private Map<String, Player> players_list;
	private long earliest_game;
	public String sport = "BASEBALL";
	private DB db;
	private Connection sql_connection = null;
	
	// constructor
	public BaseballBot(Connection sql_connection) throws IOException, JSONException{
		this.sql_connection = sql_connection;
		db = new DB(sql_connection);

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
		SimpleDateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
		JSONObject json = JsonReader.readJsonFromUrl("http://site.api.espn.com/apis/site/v2/sports/baseball/mlb/scoreboard?lang=en&region=us&calendartype=blacklist&limit=100&dates=" + today + "&tz=America%2FNew_York");
		JSONArray events = json.getJSONArray("events");
		if(events.length() == 0){
			log("No baseball games today");
			this.game_IDs = null;
			this.games = null;
			return null;
		}
		else{
			String earliest_date = events.getJSONObject(0).getString("date");
	        
	        try {
	            Date date = formatter1.parse(earliest_date.replaceAll("Z$", "+0000"));
	            long milli = date.getTime();
	            this.earliest_game = milli;
	        } 
	        catch (ParseException e) {
	            e.printStackTrace();
	        }

			for(int i=0; i < events.length(); i++){
				JSONObject game = events.getJSONObject(i);
				String name = game.getString("shortName");
				Date game_date = formatter1.parse(game.getString("date").replaceAll("Z$", "+0000"));
				Long game_milli = game_date.getTime();
				JSONArray links = game.getJSONArray("links");
				String href = links.getJSONObject(0).getString("href");
				String gameID = href.split("=")[1].replace("&sourceLang", "");
				
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
						int multiplier = scoring_rules.getInt(key);
						points += ((double) (data.getInt(key) * multiplier));
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
							int multiplier = scoring_rules.getInt(key);
							points += ((double) (player_data.getInt(key) * multiplier));
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
					winning_outcome = 1;
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
					int multiplier = scoring_rules.getInt(key);
					points += ((double) (player_data.getInt(key) * multiplier));
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
								int multiplier = scoring_rules.getInt(key);
								points += ((double) (data.getInt(key) * multiplier));
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
			
			PreparedStatement delete_old_rows = sql_connection.prepareStatement("delete from player where sport_type=?");
			delete_old_rows.setString(1, this.sport);
			delete_old_rows.executeUpdate();
			log("deleted " + this.sport + " players from old contests");
			
			JSONObject empty_data_json = new JSONObject();
			try {
				empty_data_json.put("hits", 0);
				empty_data_json.put("runs", 0);
				empty_data_json.put("rbis", 0);
				empty_data_json.put("walks", 0);
				empty_data_json.put("strikeouts", 0);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			if(this.getPlayerHashMap() == null)
				return;
			
			for(Player player : this.getPlayerHashMap().values()){
				
				PreparedStatement save_player = sql_connection.prepareStatement("insert into player(id, name, sport_type, gameID, team_abr, salary, data, points, bioJSON, filter_on) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
				save_player.setString(1, player.getESPN_ID());
				save_player.setString(2, player.getName());
				save_player.setString(3, this.sport);
				save_player.setString(4, player.getGameID());
				save_player.setString(5, player.getTeam());
				save_player.setDouble(6, player.getSalary());
				save_player.setString(7, empty_data_json.toString());
				save_player.setDouble(8, player.getPoints());
				save_player.setString(9, player.getBio().toString());
				save_player.setInt(10, player.get_filter());
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
				int multiplier = scoring_rules.getInt(key);
				data_to_display += key.toUpperCase() + ": " + String.valueOf(data.get(key)) + ", ";
				points += ((double) (data.getInt(key) * multiplier));			
			}
			// chop off ", " from end of string
			data_to_display = data_to_display.substring(0, data_to_display.length() - 2);
			
			player.put("score_raw", data_to_display);
			player.put("score_normalized", points);
			player.put("id", id);

			player_map.put(player);
		}
		return player_map;
	}
	
	// setup() method creates contest by creating a hashmap of <ESPN_ID, Player> entries
		public Map<String, Player> setup() throws IOException, JSONException, SQLException{
			Map<String, Player> players = new HashMap<String,Player>();
			if(this.game_IDs == null){
				log("No games scheduled");
				this.players_list = null;
				return null;
			}
			for(int i=0; i < this.game_IDs.size(); i++){
				// for each gameID, get the two teams playing
				Document page = Jsoup.connect("http://www.espn.com/mlb/game?gameId="+this.game_IDs.get(i)).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
					      .referrer("http://www.google.com").timeout(6000).get();
				Elements team_divs = page.getElementsByClass("team-info-wrapper");
				// for each team, go to their stats page and scrape ppg
				for(Element team : team_divs){
					String team_link = team.select("a").attr("href");
					String team_abr = team_link.split("/")[5];
					log(team_abr);
					Document team_stats_page = Jsoup.connect("http://www.espn.com/mlb/team/stats/batting/_/name/" +team_abr + "/cat/atBats").userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
						      .referrer("http://www.google.com").timeout(6000).get();
					Element stats_table = team_stats_page.select("table.tablehead").first();
					Elements rows = stats_table.getElementsByTag("tr");
					for (Element row : rows){
						if(row.className().contains("oddrow") || row.className().contains("evenrow")){
							Elements cols = row.getElementsByTag("td");
							String name = cols.get(0).select("a").text();
							try{
								String ESPN_id = cols.get(0).select("a").attr("href").split("/")[7];
								Player p = new Player(ESPN_id, name);
								int return_value = p.scrape_info();
								if(return_value == 1){
									p.gameID = this.game_IDs.get(i);
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
		
		public boolean scrape(ArrayList<String> gameIDs) throws IOException, SQLException{
			
			int games_ended = 0;
			for(int i=0; i < gameIDs.size(); i++){
				Document page = Jsoup.connect("http://www.espn.com/mlb/boxscore?gameId="+gameIDs.get(i)).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
					      .referrer("http://www.google.com").timeout(6000).get();
				Elements tables = page.getElementsByAttributeValue("data-type", "batting");
				for (Element table : tables){
					if(!table.hasClass("stats-wrap--pre")){
						Elements rows = table.getElementsByTag("tr");
						try{
							for (Element row : rows){
								String espn_ID_url = row.getElementsByClass("name").select("a").attr("href");
								if(!espn_ID_url.isEmpty()){
									String espn_ID = espn_ID_url.split("/")[7];		
									String hits_string = row.getElementsByClass("batting-stats-h").text();
									String runs_string = row.getElementsByClass("batting-stats-r").text();
									String rbi_string = row.getElementsByClass("batting-stats-rbi").text();
									String bb_string = row.getElementsByClass("batting-stats-bb").text();
									String k_string = row.getElementsByClass("batting-stats-k").text();
									JSONObject data = new JSONObject();
									try {
										if(hits_string.equals("--"))
											hits_string = "0";
										data.put("hits", Integer.parseInt(hits_string));
										if(runs_string.equals("--"))
											runs_string = "0";
										data.put("runs", Integer.parseInt(runs_string));
										if(rbi_string.equals("--"))
											rbi_string = "0";
										data.put("rbis", Integer.parseInt(rbi_string));
										if(bb_string.equals("--"))
											bb_string = "0";
										data.put("walks", Integer.parseInt(bb_string));
										if(k_string.equals("--"))
											k_string = "0";
										data.put("strikeouts", Integer.parseInt(k_string));
									} catch (JSONException e) {
										e.printStackTrace();
									}
								
									// look up player in HashMap with ESPN_ID, update his data in DB
									db.editData(data.toString(), espn_ID, this.sport, gameIDs);
								}			
							}			
						}
						catch (NullPointerException nullPointer){		
						}	
					}
				}
	            //check to see if contest is finished - if all games are deemed final
				String time_left = page.getElementsByClass("status-detail").first().text();
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
		private JSONArray game_log;
		private JSONObject career_stats;
		private JSONObject year_stats;
		private JSONObject bio;
		private String gameID;
		private int filter;
		
		// constructor
		public Player(String id, String n){
			this.ESPN_ID = id;
			this.name = n;
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
		public void setPPG(double hits){
			this.ppg = hits;
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
			return game_log;
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
		public void set_salary(double sal){
			this.salary = sal;
		}
		public void set_filter(int at_bats){
			this.filter = at_bats;
		}
		public int get_filter(){
			return filter;
		}
		
		public void createBio() throws JSONException{
			JSONObject bio = new JSONObject();
			bio.put("birthString", this.getBirthString());
			bio.put("height", this.getHeight());
			bio.put("weight", this.getWeight());
			bio.put("pos", this.getPosition());
			bio.put("last_ten_games", this.getGameLogs());
			bio.put("career_stats", this.getCareerStats());
			bio.put("year_stats", this.getYearStats());
			bio.put("name", this.name);
			bio.put("id", this.getESPN_ID());
			bio.put("team_abr", this.getTeam());

			this.bio = bio;		
		}
		public JSONObject getBio(){
			return this.bio;
		}
		
		public int scrape_info() throws IOException, JSONException{
			
			Document page = null;
			try{
				page = Jsoup.connect("http://www.espn.com/mlb/player/_/id/" + this.getESPN_ID())
						.userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
						.referrer("http://www.google.com").timeout(0).get();
			}catch(SocketTimeoutException e){
				log("socket timeout - will try again to connect...");
				page = null;
				while(page == null){
					page = Jsoup.connect("http://www.espn.com/mlb/player/_/id/" + this.getESPN_ID())
						.userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
						.referrer("http://www.google.com").timeout(0).get();
					log(page.toString().getBytes());
				}
				
			}
			if(page == null){
				return 0;
			}
			
			Element bio = page.getElementsByClass("player-bio").first();
			
			// parse bio-bio div to get pos, weight, height, birthString
			Element general_info = bio.getElementsByClass("general-info").first();
			try{
				String pos = general_info.getElementsByTag("li").first().text().split(" ")[1];
				this.pos = pos;
				String team = general_info.getElementsByTag("li").last().getElementsByTag("a").first().attr("href").split("/")[7].toUpperCase();
				this.team_abr = team;
			}catch(NullPointerException e){
				return 0;
			}
			
			String birthDate = "", birthPlace = "", height = "", weight = "";
			Elements player_metadata = bio.getElementsByClass("player-metadata").first().getElementsByTag("li");
			for(Element li : player_metadata){
				if(li.getElementsByTag("span").first().text().equals("Birth Date")){
					birthDate = li.text().replace("Birth Date", "");
				}
				else if(li.getElementsByTag("span").first().text().equals("Birthplace")){
					birthPlace = li.text().replace("Birthplace", "");
				}
				else if(li.getElementsByTag("span").first().text().equals("Ht/Wt")){
					String ht_wt = li.text().replace("Ht/Wt", "");
					height = ht_wt.split(", ")[0].replace("-", "'") + "\"";
					weight = ht_wt.split(", ")[1].replace(".", "");
				}
			}
			this.birthString = birthDate.split("\\(")[0] + "in " + birthPlace + " (" + birthDate.split("\\(")[1];
			this.height = height;
			this.weight = weight;
			
			if(this.height.isEmpty()  || this.weight.isEmpty()){
				try{
					player_metadata = bio.getElementsByClass("player-metadata").last().getElementsByTag("li");
					for(Element li : player_metadata){
						if(li.getElementsByTag("span").first().text().equals("Ht/Wt")){
							String ht_wt = li.text().replace("Ht/Wt", "");
							height = ht_wt.split(", ")[0].replace("-", "'") + "\"";
							weight = ht_wt.split(", ")[1].replace(".", "");
						}
					}
					this.height = height;
					this.weight = weight;
				}catch(Exception e){
				}
			}
			
			String[] stats = {"STAT_TYPE","GP","AB","R","H","2B","3B","HR","RBI","BB","K","SB","CS","AVG","OBP","SLG", "OPS"};
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
					if((row.className().contains("oddrow") || row.className().contains("evenrow")) && !row.getElementsByTag("td").first().text().equals("Projected")){
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
						else{
							this.year_stats = stat;
						}
					}
				}
			}
			catch (java.lang.ArrayIndexOutOfBoundsException e){
			}
		
			String[] game_log_stats = {"DATE","OPP","SCORE","AB","R","H","2B","3B","HR","RBI","BB","K","SB","AVG"};
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
			this.game_log = game_logs;
			
			try{
				Double avg = Double.parseDouble(this.year_stats.getString("AVG"));
				int at_bats = Integer.parseInt(this.year_stats.getString("AB"));
				double price = (avg * 1000);
				if(price < 80 || at_bats < 15)
					price = 80;
				this.set_salary(price);
				this.set_filter(at_bats);
			}catch(Exception e){
				log(this.getName() + " " + this.getESPN_ID());
				Server.exception(e);
				return 0;
			}

			return 1;
		}
	}	
}
