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
	private Map<Integer, Player> players_list;
	private long earliest_game;
	public String sport = "BASEBALL";
	private DB db;
	private static Connection sql_connection = null;
	
	// constructor
	public BaseballBot(Connection sql_connection) throws IOException, JSONException{
		BaseballBot.sql_connection = sql_connection;
		db = new DB(sql_connection);

	}
	// methods
	public ArrayList<String> getGameIDs(){
		return game_IDs;
	}
	
	public long getEarliestGame(){
		return earliest_game;
	}
	public String scrapeGameIDs() throws IOException, JSONException{
		ArrayList<String> gameIDs = new ArrayList<String>();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYYMMdd");
		String today = LocalDate.now().format(formatter);
		JSONObject json = JsonReader.readJsonFromUrl("http://site.api.espn.com/apis/site/v2/sports/baseball/mlb/scoreboard?lang=en&region=us&calendartype=blacklist&limit=100&dates=" + today + "&tz=America%2FNew_York");
		JSONArray events = json.getJSONArray("events");
		if(events.length() == 0){
			log("No baseball games today");
			this.game_IDs = null;
			return null;
		}
		else{
			String earliest_date = events.getJSONObject(0).getString("date");
	        SimpleDateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
	        try {
	            Date date = formatter1.parse(earliest_date.replaceAll("Z$", "+0000"));
	            long milli = date.getTime();
	            this.earliest_game = milli;
	        } 
	        catch (ParseException e) {
	            e.printStackTrace();
	        }

			for(int i=0; i < events.length(); i++){	
				JSONArray links = events.getJSONObject(i).getJSONArray("links");
				String href = links.getJSONObject(0).getString("href");
				String gameID = href.split("=")[1].replace("&sourceLang", "");
				gameIDs.add(gameID.toString());
			}
			this.game_IDs = gameIDs;
			return gameIDs.toString();
		}
	}
	
	public Map<Integer, Player> getPlayerHashMap(){
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
	
		int index = 3;
		try {
			PreparedStatement get_players = sql_connection.prepareStatement("select id, name, team_abr from player where sport_type=? order by salary DESC limit ?");
			get_players.setString(1, "BASEBALL");
			get_players.setInt(2, contest.getInt("filter"));
			top_players = get_players.executeQuery();
			while(top_players.next()){
				JSONObject player = new JSONObject();
				player.put("description", top_players.getString(2) + " " + top_players.getString(3));
				player.put("id", index);
				player.put("player_id", top_players.getInt(1));
				option_table.put(player);
				index += 1;
			}
			contest.put("option_table", option_table);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		return contest;
	}
	
	public JSONObject settlePariMutuel(int contest_id, JSONObject scoring_rules) throws Exception{
		
		JSONObject fields = new JSONObject();
		fields.put("contest_id", contest_id);
		JSONObject contest = db.select_contest(contest_id);
		JSONArray option_table = new JSONArray(contest.getString("option_table"));
		int winning_outcome = 1;
		double max_points = -999.0;
		ArrayList<Integer> top_players = new ArrayList<Integer>();
		
		ResultSet all_players = null;
		try{
			all_players = db.getPlayerScores("GOLF");
		}catch(Exception e){
			log(e.toString());
		}
		// loop through players and compile ArratList<Integer> of player_ids with top score
		while(all_players.next()){
			int player_id = all_players.getInt(1);
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
			}
			else if(points == max_points){
				top_players.add(player_id);
			}
		}
	
		if(top_players.size() >= 2){
		//tie is correct answer;
		winning_outcome = 2;
		log("winning outcome=2 because of tie");
		fields.put("winning_outcome", winning_outcome);
		return fields;
		}
		else{
			for(Integer player_table_ID : top_players){
				for (int i=0; i<option_table.length(); i++){
					JSONObject option = option_table.getJSONObject(i);
					int option_id = option.getInt("id");
					try{
						int player_id = option.getInt("player_id");
						if(player_id == player_table_ID){
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
	}
	
	public void savePlayers(){
		try {
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
				save_player.setInt(1, player.getESPN_ID());
				save_player.setString(2, player.getName());
				save_player.setString(3, this.sport);
				save_player.setString(4, player.getGameID());
				save_player.setString(5, player.getTeam());
				save_player.setDouble(6, player.getSalary());
				save_player.setString(7, empty_data_json.toString());
				save_player.setDouble(8, player.getPoints());
				save_player.setString(9, "{}");
				save_player.setInt(10, player.get_filter());
				save_player.executeUpdate();	
			}
			log("added " + this.sport + " players to DB");
		}
		catch (Exception e) {
			Server.exception(e);
		}
	}
	
	public JSONArray updateScores(JSONObject scoring_rules) throws SQLException, JSONException{
		
		ResultSet playerScores = db.getPlayerScores(this.sport);
		JSONArray player_map = new JSONArray();
		while(playerScores.next()){
			JSONObject player = new JSONObject();
			int id = playerScores.getInt(1);
			JSONObject data = new JSONObject(playerScores.getString(2));
			player.put("id", id);
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
			player_map.put(player);
		}
		return player_map;
	}
	
	// setup() method creates contest by creating a hashmap of <ESPN_ID, Player> entries
		public Map<Integer, Player> setup() throws IOException, JSONException, SQLException{
			Map<Integer, Player> players = new HashMap<Integer,Player>();
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
					Document team_stats_page = Jsoup.connect("http://www.espn.com/mlb/team/stats/batting/_/name/" +team_abr + "/cat/atBats").userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
						      .referrer("http://www.google.com").timeout(6000).get();
					Element stats_table = team_stats_page.select("table.tablehead").first();
					Elements rows = stats_table.getElementsByTag("tr");
					for (Element row : rows){
						if(row.className().contains("oddrow") || row.className().contains("evenrow")){
							Elements cols = row.getElementsByTag("td");
							String name = cols.get(0).select("a").text();
							String team_name = team_abr.toUpperCase();
							try{
								int ESPN_id = Integer.parseInt(cols.get(0).select("a").attr("href").split("/")[7]);
								double batting_avg = Double.parseDouble(cols.get(13).text());
								int at_bats = Integer.parseInt(cols.get(2).text());
								double price = (batting_avg * 1000);
								if(price < 80 || at_bats < 15)
									price = 80;
								// create a player object, save it to the hashmap
								Player p = new Player(ESPN_id, name, team_name);
								//p.scrape_info();
								p.gameID = this.game_IDs.get(i);
								p.set_salary(price);
								p.set_filter(at_bats);
								//p.createBio();		
								players.put(ESPN_id, p);
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
									int espn_ID = Integer.parseInt(espn_ID_url.split("/")[7]);		
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
									db.editData(data.toString(), espn_ID, this.sport);
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
		private int ESPN_ID;
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
		private int filter;
		
		// constructor
		public Player(int id, String n, String team){
			this.ESPN_ID = id;
			this.name = n;
			this.team_abr = team;
		}
		
		// methods
	
		public double getPoints(){
			return fantasy_points;
		}
		public int getESPN_ID(){
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
			bio.put("Weight", this.getWeight());
			bio.put("pos", this.getPosition());
			bio.put("last_five_games", this.getGameLogs());
			bio.put("career_stats", this.getCareerStats());
			bio.put("year_stats", this.getYearStats());

			this.bio = bio;		
		}
		public JSONObject getBio(){
			return this.bio;
		}
		
		public void scrape_info() throws IOException, JSONException{
			
//			Document page = Jsoup.connect("http://www.espn.com/nba/player/_/id/"+Integer.toString(this.getESPN_ID())).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
//				      .referrer("http://www.google.com").timeout(6000).get();
//			Elements bio_divs = page.getElementsByClass("player-bio");
//			
//			// parse bio-bio div to get pos, weight, height, birthString
//			for(Element bio : bio_divs){
//				Elements general_info = bio.getElementsByClass("general-info");
//				String[] info = general_info.first().text().split(" ");
//				String pos = info[1];
//				this.pos = pos;
//				String height = info[2] + " " + info[3].replace(",", "");
//				String weight = info[4] + " " + info[5];
//				this.height = height;
//				this.weight = weight;
//				Elements player_metadata = bio.getElementsByClass("player-metadata");
//				Element item = player_metadata.first();
//				Elements lis = item.children();
//				String born_string = lis.first().text().replace("Born", "");
//				this.birthString = born_string;
//			}
//			String[] stats = {"STAT_TYPE","GP","MPG","FGM-FGA","FG%","3PM-3PA","3P%","FTM-FTA","FT%","RPG","APG","BLKPG","STLPG","PFPG","TOPG","PPG"};
//			Elements tables = page.getElementsByClass("tablehead");
//			Element stats_table;
//			try{
//				stats_table = tables.get(1);
//			}
//			catch (Exception e){
//				log("No available data for " + this.getName() + " - " + this.getESPN_ID() + " so he will not be available in contests");
//				return;
//			}
//			Elements stats_rows = stats_table.getElementsByTag("tr");
//			try{
//				for(Element row : stats_rows){
//					if(row.className().contains("oddrow") || row.className().contains("evenrow")){
//						JSONObject stat = new JSONObject();
//						Elements cols = row.getElementsByTag("td");
//						int index = 0;
//						for(Element data : cols){
//							stat.put(stats[index], data.text());
//							index = index + 1;
//						}
//						
//						if(stat.get("STAT_TYPE").equals("Career")){
//							this.career_stats = stat;
//						}
//						else{
//							this.year_stats = stat;
//						}
//					}
//				}
//			}
//			catch (java.lang.ArrayIndexOutOfBoundsException e){
//			}
//		
//			String[] game_log_stats = {"DATE","OPP","SCORE","MIN","FGM-FGA","FG%","3PM-3PA","3P%","FTM-FTA","FT%","REB","AST","BLK","STL","PF","TO","PTS"};
//			Element game_log_table;
//			try{
//				game_log_table = tables.get(2);
//			}
//			catch(java.lang.IndexOutOfBoundsException e){
//				//player does not have a year and career stats table, so his game logs is second table on page, not third
//				game_log_table = tables.get(1);
//			}
//			Elements rows = game_log_table.getElementsByTag("tr");
//			JSONArray game_logs = new JSONArray();
//			for(Element row : rows){
//				if(row.className().contains("oddrow") || row.className().contains("evenrow")){
//					if(row.children().size() < 2){
//						// skip the extra row in the game log - usually exists when player has been traded.
//						continue;
//					}
//					JSONObject game = new JSONObject();
//					Elements cols = row.getElementsByTag("td");
//					int index = 0;
//					for(Element data : cols){
//						game.put(game_log_stats[index], data.text());
//						index = index + 1;
//					}
//					game_logs.put(game);
//				}	
//			}
//			this.last_five_games = game_logs;
		}
	}	
}
