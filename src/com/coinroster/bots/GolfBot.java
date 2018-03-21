package com.coinroster.bots;

import java.sql.Connection;
import java.io.IOException;
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
import com.coinroster.api.JsonReader;
import com.coinroster.bots.GolfBot.Player;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class GolfBot extends Utils {

	public static String method_level = "admin";
	protected Map<Integer, Player> players_list;
	private String tourney_ID;
	private long start_date;
	private String sport = "GOLF";
	private static Connection sql_connection = null;
	
	public GolfBot(Connection sql_connection) throws IOException, JSONException{
		GolfBot.sql_connection = sql_connection;
	}
	
	public Map<Integer, Player> getPlayerHashMap(){
		return players_list;
	}
	
	public void scrapeTourneyID(){
		// write code to get tourneyID
		this.tourney_ID = "522";
	}
	
	public Map<Integer, Player> setup() throws IOException, JSONException, SQLException {
		Map<Integer, Player> players = new HashMap<Integer,Player>();
		String url = "https://statdata.pgatour.com/r/" + this.tourney_ID + "/field.json";
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
			log(name_fl);
			Player p = new Player(id, name_fl, tourney_ID);
			players.put(id, p);
		}
		this.players_list = players;
		return players;
	}
	
	public static ResultSet getAllPlayerIDs(){
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
				save_player.setString(8, player.getBio().toString());
				save_player.executeUpdate();	
			}
			
			log("added " + sport + " players to DB");
		}
		catch (Exception e) {
			Server.exception(e);
		}
	}

		
	class Player {
		public String method_level = "admin";
		private String name;
		private int pga_id;
		private String tourney_ID;
		private double fantasy_points = 0;
		private double avg_points;
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
		public int getPGA_ID(){
			return pga_id;
		}
		public String getTourneyID(){
			return tourney_ID;
		}
		public void setAvgPoints(double avg_pts){
			this.avg_points = avg_pts;
		}
		public double getSalary(){
			return salary;
		}
		public double getAvgPts(){
			return avg_points;
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
			// set salary
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
		
		public void scrape_info(int tourney_ID) throws IOException, JSONException{
			//
			
			
		}	
	}
}
