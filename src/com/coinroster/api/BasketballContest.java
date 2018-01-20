package com.coinroster.api;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.coinroster.Utils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BasketballContest extends Utils {
	
	
	
//	public static void main(String[] args) throws IOException, JSONException, SQLException {
//				
//		int contest_id = 001;
//		Basketball_Contest test_contest = new Basketball_Contest(contest_id);
//		
//		//call ESPN API to get gameIDs
//		test_contest.getGameIDs();
//	
//		// setup() returns a list of all players that should be available in the contest, and each player has
//		// attributes above such as name, ESPN_ID, team_abr, ppg, salary 
//		// setup() only needs to be run once per contest creation
//		Map<Integer, Player> contest_data = test_contest.setup();
//		
//		for(Player p : contest_data.values()){
//			System.out.println(p.getName() + ", " + p.getTeam() +  " - " + p.getEPSN_ID() + " - " + p.getSalary() + " - " + p.getHeight());
//			System.out.println("Last Game Played PPG:");
//			System.out.println(p.getGameLogs().getJSONObject(0).get("PTS"));
//			if(p.getGameLogs() != null)
//				System.out.println(p.getGameLogs().toString());
//			if(p.getYearStats() != null)
//				System.out.println(p.getYearStats().toString());
//			
//		}
//		
//		// scrape() method should be run every minute or so
//		// it scrapes all the boxscores for the gameIDs per contest, and then it updates the player's points 
//		test_contest.scrape();
//	
//		for(Player p : test_contest.getPlayerHashMap().values()){
//			System.out.println(p.getName() + " - " + p.getPoints());
//		}
//		
//		test_contest.scrape();
//	}
	
	
	public static String method_level = "admin";

	// instance variables
	private int ID;
	private ArrayList<String> game_IDs;
	private Map<Integer, Player> players_list;
	private boolean contest_ended = false;
	
	// constructor
	public BasketballContest(int id) throws Exception{
		this.ID = id;
	}
	// methods
	public void set_contest_ended_flag(boolean flag){
		this.contest_ended = flag;
	}
	public int getContestID(){
		return ID;
	}
	public void getGameIDs() throws IOException, JSONException{
		ArrayList<String> gameIDs = new ArrayList<String>();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYYMMdd");
		String today = LocalDate.now().format(formatter);
		JSONObject json = JsonReader.readJsonFromUrl("http://site.api.espn.com/apis/site/v2/sports/basketball/nba/scoreboard?lang=en&region=us&calendartype=blacklist&limit=100&dates=" + today + "&tz=America%2FNew_York");
		JSONArray events = json.getJSONArray("events");
		for(int i=0; i < events.length(); i++){
			JSONArray links = events.getJSONObject(i).getJSONArray("links");
			String href = links.getJSONObject(0).getString("href");
			String gameID = href.split("=")[1];
			gameIDs.add(gameID.toString());
			this.game_IDs = gameIDs;
		}
	}
	
	public boolean get_contest_ended_flag(){
		return contest_ended;
	}
	public Map<Integer, Player> getPlayerHashMap(){
		return players_list;
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
						System.out.println(p.getName() + " - " + p.getEPSN_ID());
						p.scrape_info();
						p.setPPG();
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
	public void scrape() throws IOException{
		
		if(this.get_contest_ended_flag()){
			System.out.println("CONTEST ENDED");
			//CONTEST HAS ENDED
			return;
		}
		int games_ended = 0;
		
		for(int i=0; i < this.game_IDs.size(); i++){
			Document page = Jsoup.connect("http://www.espn.com/nba/boxscore?gameId="+this.game_IDs.get(i)).timeout(6000).get();
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
								int pts;
								// if player played, get their points
								if(!points_string.isEmpty() && !points_string.contains("--")){
									pts = Integer.parseInt(points_string);		
								}
								// if player did not play, set pts=0
								else{
									pts = 0;
								}
								// look up player in HashMap with ESPN_ID, update his points
								this.players_list.get(espn_ID).editPoints(pts);
							}			
						}			
					}
				}
				catch (NullPointerException nullPointer){		
				}		
			}
			
			// check to see if contest is finished - if all games are deemed final
			String time_left = page.getElementsByClass("status-detail").first().text();
			if(time_left.equals("Final")){
				games_ended += 1;
			}
			if(games_ended == this.game_IDs.size()){
				this.set_contest_ended_flag(true);
			}
		}
	}

	class Player {
		public static final String method_level = "admin";
		private String name;
		private String team_abr;
		private int ESPN_ID;
		private int fantasy_points = 0;
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
		
		// constructor
		public Player(int id, String n, String team){
			this.ESPN_ID = id;
			this.name = n;
			this.team_abr = team;
		}
		
		// methods
		public void editPoints(int pts){
			this.fantasy_points = pts;
		}
		public int getPoints(){
			return fantasy_points;
		}
		public int getEPSN_ID(){
			return ESPN_ID;
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
			this.ppg = pts;
			if(pts < 5.0){
				this.salary = 50.0;
			}
			else{
				this.salary = Math.round(ppg * 10.0) / 1.0;
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
			
			Document page = Jsoup.connect("http://www.espn.com/nba/player/_/id/"+Integer.toString(this.getEPSN_ID())).timeout(6000).get();
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
