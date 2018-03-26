package com.coinroster.bots;

import java.sql.Connection;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import com.coinroster.Server;
import com.coinroster.Utils;
import com.coinroster.api.JsonReader;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class GolfBot extends Utils {

	public static String method_level = "admin";
	protected Map<Integer, Player> players_list;
	private String tourneyID;
	private String tourneyName;
	private long startDate;
	private String sport = "GOLF";
	private static Connection sql_connection = null;
	
	public GolfBot(Connection sql_connection) throws IOException, JSONException{
		GolfBot.sql_connection = sql_connection;
	}
	
	public Map<Integer, Player> getPlayerHashMap(){
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
	
	public void scrapeTourneyID() throws IOException, JSONException{
	// get the Thursday date in yyyy-MM-dd format
		// THIS ASSUMES ITS BEING RUN ON A MONDAY
		SimpleDateFormat formattedDate = new SimpleDateFormat("yyyy-MM-dd");            
		Calendar c = Calendar.getInstance();        
		c.add(Calendar.DATE, 3);  // add 3 days to get to Thurs
		String thursday = (String)(formattedDate.format(c.getTime()));
		c.set(Calendar.HOUR_OF_DAY, 7);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.MILLISECOND, 0);
		long milli = c.getTimeInMillis();
		// parse schedule json to get tournaments
		String url = "https://statdata.pgatour.com/r/current/schedule-v2.json";
		JSONObject schedule = JsonReader.readJsonFromUrl(url);
		JSONArray tournaments = schedule.getJSONArray("years").getJSONObject(0).getJSONArray("tours").getJSONObject(0).getJSONArray("trns");

		for(int index=0; index < tournaments.length(); index++){
			JSONObject tournament = tournaments.getJSONObject(index);
			
			//check 3 things: start-date is this coming thurs, tournament is not match-play, and tournament is week's primary tournament
			if(tournament.getJSONObject("date").getString("start").equals(thursday) && !tournament.getString("format").equals("Match") && tournament.getString("primaryEvent").equals("Y")){
				System.out.println(tournament.getJSONObject("trnName").getString("official"));
				this.tourneyName = tournament.getJSONObject("trnName").getString("official");
				this.tourneyID = tournament.getString("permNum");
				this.startDate = milli;
				break;
			}
		}
	}
	
	public Map<Integer, Player> setup() throws IOException, JSONException, SQLException {
		Map<Integer, Player> players = new HashMap<Integer, Player>();
		if(this.tourneyID != null){
			String url = "https://statdata.pgatour.com/r/" + tourneyID + "/field.json";
			JSONObject field = JsonReader.readJsonFromUrl(url);
			JSONArray players_json = field.getJSONObject("Tournament").getJSONArray("Players");
			for(int i = 0; i < players_json.length(); i++){
				JSONObject player = players_json.getJSONObject(i);
				int id = Integer.parseInt(player.getString("TournamentPlayerId"));
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
				double salary = 0;
				for(int q=0; q < ranked_players.length(); q++){
					JSONObject r_player = ranked_players.getJSONObject(q);
					if(id == Integer.parseInt(r_player.getString("plrNum"))){
						String points = r_player.getJSONObject("statValues").getString("statValue2");
						salary = Math.round(Double.parseDouble(points) * 10);
						if(salary < 80.0)
							salary = 80.0;
						System.out.println(name_fl + " - Salary: " + salary);
						checked = true;
						break;
					}
				}
				if(!checked){
					System.out.println(name_fl + " did not have a corresponding score");
					salary = 80.0;
				}
				
				Player p = new Player(id, name_fl, tourneyID);
				p.set_ppg_salary(salary);
				players.put(id, p);
			}
		}
		this.players_list = players;
		return players;
	}
	
	public ResultSet getAllPlayerIDs(){
		ResultSet result_set = null;
		try {
			PreparedStatement get_players = sql_connection.prepareStatement("select id from player where sport_type=?");
			get_players.setString(1, "GOLF");
			result_set = get_players.executeQuery();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return result_set;
	}
	
	public ResultSet getPlayerScores() throws SQLException{
		ResultSet result_set = null;
		try {
			PreparedStatement get_players = sql_connection.prepareStatement("select id, points from player where sport_type=?");
			get_players.setString(1, this.sport);
			result_set = get_players.executeQuery();		
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return result_set;
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
				
				PreparedStatement save_player = sql_connection.prepareStatement("insert into player(id, name, sport_type, gameID, team_abr, salary, points, bioJSON) values(?, ?, ?, ?, ?, ?, ?, ?)");				
				save_player.setInt(1, player.getPGA_ID());
				save_player.setString(2, player.getName());
				save_player.setString(3, sport);
				save_player.setString(4, player.getTourneyID());
				save_player.setString(5, "n/a");
				save_player.setDouble(6, player.getSalary());
				save_player.setDouble(7, player.getPoints());
				JSONObject emptyJSON = new JSONObject();
				save_player.setString(8, emptyJSON.toString());
				save_player.executeUpdate();	
			}
			
			log("added " + sport + " players to DB");
		}
		catch (Exception e) {
			Server.exception(e);
		}
	}

	public boolean scrapeScores() throws JSONException, IOException, SQLException{
		String url = "https://statdata.pgatour.com/r/" + this.tourneyID + "/2018/leaderboard-v2.json";
		JSONObject leaderboard = JsonReader.readJsonFromUrl(url);
		boolean finished = leaderboard.getJSONObject("leaderboard").getBoolean("is_finished");
		
		JSONArray players = leaderboard.getJSONObject("leaderboard").getJSONArray("players");
		for(int i=0; i < players.length(); i++){
			JSONObject player = players.getJSONObject(i);
			int player_id = Integer.parseInt(player.getString("player_id"));
			int score = player.getInt("total");
			editPoints(score, sql_connection, player_id);
		}	
		return finished;		
	}
	
	public JSONObject updateScoresDB(int contest_id) throws SQLException, JSONException{
		
		PreparedStatement worst_score = sql_connection.prepareStatement("select points from player where sport_type=? order by points DESC limit 1");
		worst_score.setString(1, "GOLF");
		ResultSet score = worst_score.executeQuery();
		int worstScore = 0;
		while(score.next()){
			worstScore = score.getInt(1);
		}
		log("worst score in tournament: " + worstScore);
		log("normalizing scores...");
		JSONObject fields = new JSONObject();
		fields.put("contest_id", contest_id);
		fields.put("normalization_scheme", "INTEGER-INVERT");
		ResultSet playerScores = this.getPlayerScores();
		JSONArray player_map = new JSONArray();
		while(playerScores.next()){
			JSONObject player = new JSONObject();
			int id = playerScores.getInt(1);
			double points = playerScores.getDouble(2);
			int normalizedScore = normalizeScore(points, worstScore);
			player.put("id", id);
			player.put("score_raw", Double.toString(points));
			player.put("score_normalized", normalizedScore);
			player_map.put(player);
		}
		
		fields.put("player_scores", player_map);
		return fields;	
	}
	
	public int normalizeScore(double score, int worstScore){
		int normalizedScore = (int) (worstScore - score + 1);
		return normalizedScore;
	}
	
	public static void editPoints(double pts, Connection sql_connection, int id) throws SQLException{
		PreparedStatement update_points = sql_connection.prepareStatement("update player set points = ? where id = ?");
		update_points.setDouble(1, pts);
		update_points.setInt(2, id);
		update_points.executeUpdate();
	}
	
	
	class Player {
		public String method_level = "admin";
		private String name;
		private int pga_id;
		private String tourney_ID;
		private double fantasy_points = 0;
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
		public Player(int id, String n, String tourney_ID){
			this.pga_id = id;
			this.name = n;
			this.tourney_ID = tourney_ID;
		}
		
		// methods
	
		public double getPoints(){
			return fantasy_points;
		}
		
		public void setScore(double score){
			this.fantasy_points = score;
		}
		
		public int getPGA_ID(){
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
			this.salary = pts;
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
			
	}
}
