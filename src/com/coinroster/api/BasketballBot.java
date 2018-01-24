package com.coinroster.api;
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
import java.util.Map;

import com.coinroster.DB;
import com.coinroster.Server;
import com.coinroster.Utils;
import com.coinroster.internal.BuildLobby;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BasketballBot extends Utils {
	
	public static String method_level = "admin";

	// instance variables
	protected ArrayList<String> game_IDs;
	private Map<Integer, Player> players_list;
	private boolean contest_ended = false;
	private long earliest_game;
	private String sport = "BASKETBALL";

	// constructor
	public BasketballBot() throws IOException, JSONException{
	}
	// methods
	public void set_contest_ended_flag(boolean flag){
		this.contest_ended = flag;
	}
	public ArrayList<String> getGameIDs(){
		return game_IDs;
	}
	
	public long getEarliestGame(){
		return earliest_game;
	}
	public void scrapeGameIDs() throws IOException, JSONException{
		ArrayList<String> gameIDs = new ArrayList<String>();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYYMMdd");
		String today = LocalDate.now().format(formatter);
		JSONObject json = JsonReader.readJsonFromUrl("http://site.api.espn.com/apis/site/v2/sports/basketball/nba/scoreboard?lang=en&region=us&calendartype=blacklist&limit=100&dates=" + today + "&tz=America%2FNew_York");
		JSONArray events = json.getJSONArray("events");
		if(events.length() == 0){
			System.out.println("No games");
			this.game_IDs = null;
		}
		else{
			String earliest_date = events.getJSONObject(0).getString("date");
	        SimpleDateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
	        try {
	            Date date = formatter1.parse(earliest_date.replaceAll("Z$", "+0000"));
	            System.out.println(date);
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
				this.game_IDs = gameIDs;
			}
		}
	}
	
	public boolean get_contest_ended_flag(){
		return contest_ended;
	}
	public Map<Integer, Player> getPlayerHashMap(){
		return players_list;
	}
	
	public static ResultSet getAllPlayerIDs(){
		Connection sql_connection = null;
		ResultSet result_set = null;
		try {
			sql_connection = Server.sql_connection();
			PreparedStatement get_players = sql_connection.prepareStatement("select id from player where sport_type=?");
			get_players.setString(1, "BASKETBALL");
			result_set = get_players.executeQuery();
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		return result_set;
	}
	
	public ArrayList<String> getAllGameIDsDB() throws SQLException{
		Connection sql_connection = null;
		ResultSet result_set = null;
		ArrayList<String> gameIDs = new ArrayList<String>();
		try {
			sql_connection = Server.sql_connection();
			PreparedStatement get_games = sql_connection.prepareStatement("select distinct gameID from player where sport_type=?");
			get_games.setString(1, "BASKETBALL");
			result_set = get_games.executeQuery();
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		while(result_set.next()){
			gameIDs.add(result_set.getString(1));	
		}
		return gameIDs;
	}
	
	public void savePlayers(){
		Connection sql_connection = null;
		try {
			sql_connection = Server.sql_connection();
			PreparedStatement delete_old_rows = sql_connection.prepareStatement("delete from player where sport_type=?");
			delete_old_rows.setString(1, this.sport);
			delete_old_rows.executeUpdate();
			System.out.println("deleted " + this.sport + " players from old contests");

			for(Player player : this.getPlayerHashMap().values()){
				
				PreparedStatement save_player = sql_connection.prepareStatement("insert into player(id, name, sport_type, gameID, team_abr, salary, points, bioJSON) values(?, ?, ?, ?, ?, ?, ?, ?)");				
				save_player.setInt(1, player.getESPN_ID());
				save_player.setString(2, player.getName());
				save_player.setString(3, sport);
				save_player.setString(4, player.getGameID());
				save_player.setString(5, player.getTeam());
				save_player.setDouble(6, player.getSalary());
				save_player.setDouble(7, player.getPoints());
				save_player.setString(8, player.getBio().toString());
				save_player.executeUpdate();	
			}
			
			System.out.println("added " + sport + " players to DB");
		}
		catch (Exception e) {
			Server.exception(e);
		} 
		finally {
			if (sql_connection != null){
				try {sql_connection.close();} 
				catch (SQLException ignore) {}
			}
		}
	}
	
	public static int createContest(String category, String contest_type, String progressive_code, 
			String title, String desc, double rake, double cost_per_entry, String settlement_type, 
			int salary_cap, int min_users, int max_users, int entries_per_user, int roster_size, 
			String odds_source, String score_header, double[] payouts, ResultSet playerIDs, Long deadline) throws Exception{

	
		Connection sql_connection = Server.sql_connection();
		DB db = new DB(sql_connection);

		if (title.length() > 255)
		{
			log("Title is too long");
			return 1;
		}

		if (deadline - System.currentTimeMillis() < 1 * 60 * 60 * 1000)
		{
			log( "Registration deadline must be at least 1 hour from now");
			return 1;
		}

		if (rake < 0 || rake >= 100)
		{
			log("Rake cannot be < 0 or > 100");
			return 1;
		}

		rake = divide(rake, 100, 0); // convert to %

		if (cost_per_entry == 0)
		{
			log("Cost per entry cannot be 0");
			return 1;
		}

		if (progressive_code.equals("")) progressive_code = null; // default value
		else
		{
			JSONObject progressive = db.select_progressive(progressive_code);

			if (progressive == null)
			{
				log("Invalid Progressive");
				return 1;
			}

			if (!progressive.getString("category").equals(category) || !progressive.getString("sub_category").equals("BASKETBALL"))
			{
				log("Progressive belongs to a different category");
				return 1;
			}
		}

		log("Contest parameters:");

		log("category: " + category);
		log("sub_category: BASKETBALL");
		log("progressive: " + progressive_code);
		log("contest_type: " + contest_type);
		log("title: " + title);
		log("description: " + desc);
		log("registration_deadline: " + deadline);
		log("rake: " + rake);
		log("cost_per_entry: " + cost_per_entry);

		if(contest_type.equals("ROSTER")){
			if (min_users < 2)
			{
				log( "Invalid value for [min users]");
				return 1;
			}
			if (max_users < min_users && max_users != 0) // 0 = unlimited
			{
				log( "Invalid value for [max users]");
				return 1;
			}

			if (roster_size < 0)
			{
				log("Roster size cannobe be negative");
				return 1;
			}

			if (entries_per_user < 0)
			{
				log("Invalid value for [entries per user]");
				return 1;
			}

			if (score_header.equals(""))
			{
				log("Please choose a score column header");
				return 1;
			}

			JSONArray option_table = new JSONArray();
			while(playerIDs.next()){
				PreparedStatement get_player = sql_connection.prepareStatement("select name, team_abr, salary from player where id = ?");
				get_player.setInt(1, playerIDs.getInt(1));
				ResultSet player_data = get_player.executeQuery();
				if(player_data.next()){
					JSONObject player = new JSONObject();
					player.put("name", player_data.getString(1) + " " + player_data.getString(2));
					player.put("price", player_data.getDouble(3));
					player.put("count", 0);
					player.put("id", playerIDs.getInt(1));
					option_table.put(player);
				}
			}
							
			switch (settlement_type)
			{
			case "HEADS-UP":

				if (min_users != 2 || max_users != 2)
				{
					log("Invalid value(s) for number of users");
					return 1;
				}
				break;

			case "DOUBLE-UP": break;

			case "JACKPOT": break;

			default:

				log("Invalid value for [settlement type]");
				return 1;

			}
			JSONArray pay_table = new JSONArray();
			for(int i=0; i < payouts.length; i++){
				JSONObject line = new JSONObject();
				line.put("payout", payouts[i]);
				line.put("rank", i+1);
				pay_table.put(line);
			}
			
			try{
				
				PreparedStatement create_contest = sql_connection.prepareStatement("insert into contest(category, sub_category, progressive, contest_type, title, description, registration_deadline, rake, cost_per_entry, settlement_type, min_users, max_users, entries_per_user, pay_table, salary_cap, option_table, created, created_by, roster_size, odds_source, score_header) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
				create_contest.setString(1, category);
				create_contest.setString(2, "BASKETBALL");
				create_contest.setString(3, progressive_code);
				create_contest.setString(4, contest_type);
				create_contest.setString(5, title);
				create_contest.setString(6, desc);
				create_contest.setLong(7, deadline);
				create_contest.setDouble(8, rake);
				create_contest.setDouble(9, cost_per_entry);
				create_contest.setString(10, settlement_type);
				create_contest.setInt(11, min_users);
				create_contest.setInt(12, max_users);
				create_contest.setInt(13, entries_per_user);
				create_contest.setString(14, pay_table.toString());
				create_contest.setDouble(15, salary_cap);
				create_contest.setString(16, option_table.toString());	
				create_contest.setLong(17, System.currentTimeMillis());
				create_contest.setString(18, "ColeFisher");
				create_contest.setInt(19, roster_size);
				create_contest.setString(20, odds_source);
				log(odds_source);
				create_contest.setString(21, score_header);
				create_contest.executeUpdate();
				log("added contest to db");
				new BuildLobby(sql_connection);
			}
			catch(Exception e){
				e.printStackTrace();
			}
        }
	return 0;
	}
	
	public static void editPoints(double pts, Connection sql_connection, int id) throws SQLException{
		PreparedStatement update_points = sql_connection.prepareStatement("update player set points = ? where id = ?");
		update_points.setDouble(1, pts);
		update_points.setInt(2, id);
		update_points.executeUpdate();
//		this.fantasy_points = pts;

	}
	// setup() method creates contest by creating a hashmap of <ESPN_ID, Player> entries
	public Map<Integer, Player> setup() throws IOException, JSONException, SQLException{
		Map<Integer, Player> players = new HashMap<Integer,Player>();
		for(int i=0; i < this.game_IDs.size(); i++){
			// for each gameID, get the two teams playing
			Document page = Jsoup.connect("http://www.espn.com/nba/game?gameId="+this.game_IDs.get(i)).timeout(6000).get();
			Elements team_divs = page.getElementsByClass("team-info-wrapper");
			// for each team, go to their stats page and scrape ppg
			for(Element team : team_divs){
				String team_link = team.select("a").attr("href");
				String team_abr = team_link.split("/")[5];
				Document team_stats_page = Jsoup.connect("http://www.espn.com/nba/team/roster/_/name/" + team_abr).timeout(6000).get();
				Element stats_table = team_stats_page.getElementsByClass("mod-table").first().getElementsByClass("mod-content").first();
				Elements rows = stats_table.getElementsByTag("tr");
				for (Element row : rows){
					if(row.className().contains("oddrow") || row.className().contains("evenrow")){
						Elements cols = row.getElementsByTag("td");
						String name = cols.get(1).select("a").text();
						String team_name = team_abr.toUpperCase();
						int ESPN_id = Integer.parseInt(cols.get(1).select("a").attr("href").split("/")[7]);
						// create a player object, save it to the hashmap
						Player p = new Player(ESPN_id, name, team_name);
						System.out.println(p.getName() + " - " + p.getESPN_ID());
						p.scrape_info();
						p.setPPG();
						p.gameID = this.game_IDs.get(i);
						p.set_ppg_salary(p.getPPG());
						p.createBio();		
						players.put(ESPN_id, p);
					}
				}
			}
		}
		this.players_list = players;
		return players;
	}
	
	// scrape() method gets points from ESPN boxscores. 
	// meant to run every minute or so. Updates the Contest's player hashmap
	public void scrape(ArrayList<String> gameIDs) throws IOException, SQLException{
		
		Connection sql_connection = Server.sql_connection();

//		if(this.get_contest_ended_flag()){
//			System.out.println("CONTEST ENDED");
//			//CONTEST HAS ENDED
//			return;
//		}
//		int games_ended = 0;
		
		for(int i=0; i < gameIDs.size(); i++){
			Document page = Jsoup.connect("http://www.espn.com/nba/boxscore?gameId="+gameIDs.get(i)).timeout(6000).get();
			Elements tables = page.getElementsByClass("mod-data");
			for (Element table : tables){
				Elements rows = table.getElementsByTag("tr");
				try{
					for (Element row : rows){
						Elements spans = row.getElementsByClass("name").select("a").select("span");
						String espn_ID_url = row.getElementsByClass("name").select("a").attr("href");
						if(!espn_ID_url.isEmpty()){
							int espn_ID = Integer.parseInt(espn_ID_url.split("/")[7]);
							if(spans.size() == 2){
								String points_string = row.getElementsByClass("pts").text();
								double pts;
								// if player played, get their points
								if(!points_string.isEmpty() && !points_string.contains("--")){
									pts = Double.parseDouble(points_string);		
								}
								// if player did not play, set pts=0
								else{
									pts = 0.0;
								}
								// look up player in HashMap with ESPN_ID, update his points in DB
								editPoints(pts, sql_connection, espn_ID);
							}			
						}			
					}
				}
				catch (NullPointerException nullPointer){		
				}		
			}
			
			// check to see if contest is finished - if all games are deemed final
//			String time_left = page.getElementsByClass("status-detail").first().text();
//			if(time_left.equals("Final")){
//				games_ended += 1;
//			}
//			if(games_ended == this.game_IDs.size()){
//				this.set_contest_ended_flag(true);
//			}
		}
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
		public void setPPG() throws JSONException{
			try{
				String ppg = this.getYearStats().getString("PPG");
				this.ppg = Double.parseDouble(ppg);
			}
			catch(java.lang.NullPointerException e){
				this.ppg = 0.0;
			}
			catch(JSONException e){
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
			if(pts < 5.0){
				this.salary = 50.0;
			}
			else{
				this.salary = Math.round(ppg * 10.0);
			}
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
			
			Document page = Jsoup.connect("http://www.espn.com/nba/player/_/id/"+Integer.toString(this.getESPN_ID())).timeout(6000).get();
			Elements bio_divs = page.getElementsByClass("player-bio");
			
			// parse bio-bio div to get pos, weight, height, birthString
			for(Element bio : bio_divs){
				Elements general_info = bio.getElementsByClass("general-info");
				String[] info = general_info.first().text().split(" ");
				String pos = info[1];
				this.pos = pos;
				String height = info[2] + " " + info[3].replace(",", "");
				String weight = info[4] + " " + info[5];
				this.height = height;
				this.weight = weight;
				Elements player_metadata = bio.getElementsByClass("player-metadata");
				Element item = player_metadata.first();
				Elements lis = item.children();
				String born_string = lis.first().text().replace("Born", "");
				this.birthString = born_string;
			}
			String[] stats = {"STAT_TYPE","GP","MPG","FGM-FGA","FG%","3PM-3PA","3P%","FTM-FTA","FT%","RPG","APG","BLKPG","STLPG","PFPG","TOPG","PPG"};
			Elements tables = page.getElementsByClass("tablehead");
			Element stats_table = tables.get(1);
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
						else{
							this.year_stats = stat;
						}
					}
				}
			}
			catch (java.lang.ArrayIndexOutOfBoundsException e){
			}
		
			String[] game_log_stats = {"DATE","OPP","SCORE","MIN","FGM-FGA","FG%","3PM-3PA","3P%","FTM-FTA","FT%","REB","AST","BLK","STL","PF","TO","PTS"};
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
		}
	}
	
}
