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
import java.util.Map;
import com.coinroster.DB;
import com.coinroster.Server;
import com.coinroster.Utils;
import com.coinroster.api.JsonReader;
import com.coinroster.bots.BasketballBot.Player;

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
	public void scrapeGameIDs() throws IOException, JSONException{
		ArrayList<String> gameIDs = new ArrayList<String>();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYYMMdd");
		String today = LocalDate.now().format(formatter);
		JSONObject json = JsonReader.readJsonFromUrl("http://site.api.espn.com/apis/site/v2/sports/baseball/mlb/scoreboard?lang=en&region=us&calendartype=blacklist&limit=100&dates=" + today + "&tz=America%2FNew_York");
		JSONArray events = json.getJSONArray("events");
		if(events.length() == 0){
			log("No games");
			this.game_IDs = null;
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
				System.out.println(gameID);
				gameIDs.add(gameID.toString());
			}
			this.game_IDs = gameIDs;
		}
	}
	
	public Map<Integer, Player> getPlayerHashMap(){
		return players_list;
	}
	
	public JSONObject createPariMutuel(Long deadline, String date) throws JSONException{
		JSONObject fields = new JSONObject();
		fields.put("category", "FANTASYSPORTS");
		fields.put("sub_category", this.sport);
		fields.put("contest_type", "PARI-MUTUEL");
		fields.put("progressive", "MOSTHITS");
		String title = "Most Hits in MLB Tonight | " + date;
		fields.put("title", title);
		fields.put("description", "Place your bet on who you will think will have the most hits today!");
		fields.put("rake", 5);
		fields.put("cost_per_entry", 0.00000001);
		fields.put("registration_deadline", deadline);
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
			PreparedStatement get_players = sql_connection.prepareStatement("select id, name, team_abr from player where sport_type=? order by salary DESC limit 7");
			get_players.setString(1, "BASEBALL");
			top_players = get_players.executeQuery();
			while(top_players.next()){
				JSONObject player = new JSONObject();
				player.put("description", top_players.getString(2) + " " + top_players.getString(3));
				player.put("id", index);
				player.put("player_id", top_players.getInt(1));
				option_table.put(player);
				index += 1;
			}
			fields.put("option_table", option_table);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		return fields;
	}
	
	public JSONObject settlePariMutuel(int contest_id) throws Exception{
		JSONObject fields = new JSONObject();
		fields.put("contest_id", contest_id);
		JSONObject contest = db.select_contest(contest_id);
		JSONArray option_table = new JSONArray(contest.getString("option_table"));
		int winning_outcome = 1;
		ResultSet max_pts = null;
		try {
			PreparedStatement get_pts = sql_connection.prepareStatement("select max(points) from player where sport_type=?");
			get_pts.setString(1, this.sport);
			max_pts = get_pts.executeQuery();
			double points = 0;
			while(max_pts.next())
				points = max_pts.getDouble(1);
			log("Max hits tonight was " + points);
			ResultSet players = null;
			try {
				PreparedStatement get_players = sql_connection.prepareStatement("select id from player where sport_type=? and points=?");
				get_players.setString(1, this.sport);
				get_players.setDouble(2, points);
				players = get_players.executeQuery();
				
				ArrayList<Integer> IDs = new ArrayList<Integer>();
				while(players.next()){
					IDs.add(players.getInt(1));
				}
				
				if(IDs.size() > 1){
					//tie is correct answer;
					winning_outcome = 2;
					log("winning outcome=2 because of tie");
					fields.put("winning_outcome", winning_outcome);
					return fields;
				}
				else{
					for(Integer player_table_ID : IDs){
						log(player_table_ID);
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
			}
				
			catch(Exception e){
				e.printStackTrace();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		return fields;
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
				save_player.setInt(1, player.getESPN_ID());
				save_player.setString(2, player.getName());
				save_player.setString(3, this.sport);
				save_player.setString(4, player.getGameID());
				save_player.setString(5, player.getTeam());
				save_player.setDouble(6, player.getSalary());
				save_player.setDouble(7, player.getPoints());
				save_player.setString(8, player.getBio().toString());
				save_player.executeUpdate();	
			}
			log("added " + this.sport + " players to DB");
		}
		catch (Exception e) {
			Server.exception(e);
		}
	}
	
	public JSONObject updateScores(int contest_id) throws SQLException, JSONException{
		JSONObject fields = new JSONObject();
		fields.put("contest_id", contest_id);
		fields.put("normalization_scheme", "INTEGER");
		
		ResultSet playerScores = db.getPlayerScores(this.sport);
		JSONArray player_map = new JSONArray();
		while(playerScores.next()){
			JSONObject player = new JSONObject();
			int id = playerScores.getInt(1);
			double points = playerScores.getDouble(2);
			player.put("id", id);
			player.put("score_raw", Double.toString(points));
			player.put("score_normalized", points);
			player_map.put(player);
		}
		
		fields.put("player_scores", player_map);
		return fields;
	}
	
	
}
