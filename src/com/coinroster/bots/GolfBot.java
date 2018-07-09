//package com.coinroster.bots;
//
//import java.sql.Connection;
//import java.io.IOException;
//import java.lang.reflect.Constructor;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.text.Normalizer;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.HashMap;
//import java.util.Map;
//
//import com.coinroster.DB;
//import com.coinroster.MethodInstance;
//import com.coinroster.Server;
//import com.coinroster.Utils;
//import com.coinroster.internal.JsonReader;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//import org.json.JSONArray;
//
//public class GolfBot extends Utils {
//
//	public static String method_level = "admin";
//	protected Map<Integer, Player> players_list;
//	private String tourneyID;
//	private String tourneyName;
//	private long startDate;
//	private DB db;
//	public String sport = "GOLF";
//	private static Connection sql_connection = null;
//	
//	public GolfBot(Connection sql_connection) throws IOException, JSONException{
//		GolfBot.sql_connection = sql_connection;
//		db = new DB(sql_connection);
//	}
//	
//	public Map<Integer, Player> getPlayerHashMap(){
//		return players_list;
//	}
//	public String getTourneyName(){
//		return tourneyName;
//	}
//	
//	public String getTourneyID(){
//		return this.tourneyID;
//	}
//	
//	public long getDeadline(){
//		return startDate;
//	}
//	public String getLiveTourneyID() throws SQLException{
//		ResultSet result_set = null;
//		String id = null;
//		try {
//			PreparedStatement get_games = sql_connection.prepareStatement("select distinct gameID from player where sport_type=?");
//			get_games.setString(1, this.sport);
//			result_set = get_games.executeQuery();		
//		}
//		catch(Exception e){
//			e.printStackTrace();
//		}
//		while(result_set.next()){
//			id = result_set.getString(1);	
//		}
//		return id;
//	}
//	
//	public boolean appendLateAdditions() throws SQLException, JSONException, IOException{
//		
//		String gameID = this.getLiveTourneyID();
//		Map<Integer, JSONArray> contest_players = new HashMap<Integer, JSONArray>();
//		ResultSet result_set = null;
//		
//		try {
//			PreparedStatement get_contests = sql_connection.prepareStatement("select id, option_table from contest where gameIDs=? and contest_type='ROSTER' and status=1");
//			get_contests.setString(1, gameID);
//			result_set = get_contests.executeQuery();		
//		}
//		catch(Exception e){
//			e.printStackTrace();
//		}
//		while(result_set.next()){
//			int id = result_set.getInt(1);
//			JSONArray option_table = new JSONArray(result_set.getString(2));
//			contest_players.put(id, option_table);	
//		}
//		
//		if(contest_players.isEmpty()){
//			return false;
//		}
//		
//		ArrayList<Integer> existing_ids = new ArrayList<Integer>();
//		ResultSet all_existing_players = db.getAllPlayerIDs(this.sport);
//		while(all_existing_players.next()){
//			existing_ids.add(all_existing_players.getInt(1));
//		}
//		
//		boolean flag = false;
//		String url = "https://statdata.pgatour.com/r/" + gameID + "/field.json";
//		JSONObject field = JsonReader.readJsonFromUrl(url);
//		if(field == null){
//			log("Unable to connect to pgatour API");
//			return false;
//		}
//		JSONArray players_json = field.getJSONObject("Tournament").getJSONArray("Players");
//		boolean player_table_updated = false;
//		
//		for(Map.Entry<Integer, JSONArray> entry : contest_players.entrySet()){
//			int contest_id = entry.getKey();
//			JSONArray option_table = entry.getValue();
//			
//			//check if any players in DB's player table didn't make it in:
//			for(Integer existing_id : existing_ids){
//				boolean in_option_table = false;
//				for(int option_table_index = 0; option_table_index < option_table.length(); option_table_index++){
//					int p_id = option_table.getJSONObject(option_table_index).getInt("id");
//					if(p_id == existing_id){
//						in_option_table = true;
//						break;
//					}
//				}
//				//add player from player table to option table
//				if(!in_option_table){
//					PreparedStatement get_data = sql_connection.prepareStatement("select name, salary from player where sport_type=? and id=?");
//					get_data.setString(1, this.sport);
//					get_data.setInt(2, existing_id);
//					ResultSet player_data = get_data.executeQuery();
//					while(player_data.next()){
//						String name = player_data.getString(1);
//						double price = player_data.getDouble(2);
//						JSONObject p = new JSONObject();
//						p.put("id", existing_id);
//						p.put("name", name);
//						p.put("count",0);
//						p.put("price", price);
//						option_table.put(p);
//						log("appending " + name + " to contest " + contest_id);
//						PreparedStatement update_contest = sql_connection.prepareStatement("UPDATE contest SET option_table = ? where id = ?");
//						update_contest.setString(1, option_table.toString());
//						update_contest.setInt(2, contest_id);
//						update_contest.executeUpdate();
//					}
//				}
//			}
//			
//			for(int i = 0; i < players_json.length(); i++){
//				JSONObject player = players_json.getJSONObject(i);
//				int id = Integer.parseInt(player.getString("TournamentPlayerId"));
//				
//				if(!existing_ids.contains(id)){
//					flag = true;
//					String name = player.getString("PlayerName");
//					String names[] = name.split(", ");
//					String name_fl;
//					try{
//						name_fl = names[1] + " " + names[0];
//					}
//					catch(Exception e){
//						name_fl = names[0];
//					}
//					String rank_url = "https://statdata.pgatour.com/r/stats/2018/186.json";
//					JSONObject world_rankings_json = JsonReader.readJsonFromUrl(rank_url);
//					JSONArray ranked_players = world_rankings_json.getJSONArray("tours").getJSONObject(0).getJSONArray("years").getJSONObject(0).getJSONArray("stats").getJSONObject(0).getJSONArray("details");
//					boolean checked = false;
//					String name_with_country = name_fl;
//					double salary = 0;
//					for(int q=0; q < ranked_players.length(); q++){
//						JSONObject r_player = ranked_players.getJSONObject(q);
//						if(id == Integer.parseInt(r_player.getString("plrNum"))){
//							String points = r_player.getJSONObject("statValues").getString("statValue2");
//							String country = r_player.getJSONObject("statValues").getString("statValue5");
//							name_with_country = name_fl + " " + country;
//							salary = Math.round(Double.parseDouble(points) * 10);
//							if(salary < 80.0)
//								salary = 80.0;
//							checked = true;
//							break;
//						}
//					}
//					if(!checked){
//						salary = 80.0;
//					}
//			
//					if(!player_table_updated){
//						// add player to player table
//						PreparedStatement add_player = sql_connection.prepareStatement("INSERT INTO player (id, name, sport_type, gameID, team_abr, salary, points, bioJSON) VALUES (?, ?, ?, ?, ?, ?, ?, ? )");
//						add_player.setInt(1, id);
//						add_player.setString(2, name_with_country);
//						add_player.setString(3, this.sport);
//						add_player.setString(4, gameID);
//						add_player.setString(5, "n/a");
//						add_player.setDouble(6, salary);
//						add_player.setDouble(7, 0.0);
//						add_player.setString(8, "{}");
//						add_player.executeUpdate();
//					}
//					
//					//create JSONObject to add to option table
//					JSONObject p = new JSONObject();
//					p.put("price", salary);
//					p.put("count", 0);
//					p.put("name", name_with_country);
//					p.put("id", id);
//					log("appending " + name_with_country + " to contest " + contest_id );
//					option_table.put(p);	
//				}
//			}
//			player_table_updated = true;
//			PreparedStatement update_contest = sql_connection.prepareStatement("UPDATE contest SET option_table = ? where id = ?");
//			update_contest.setString(1, option_table.toString());
//			update_contest.setInt(2, contest_id);
//			update_contest.executeUpdate();
//		}
//		return flag;
//	}
//	
//	public void scrapeTourneyID() throws IOException, JSONException{
//		// get the Thursday date in yyyy-MM-dd format
//		// THIS ASSUMES ITS BEING RUN ON A MONDAY
//		SimpleDateFormat formattedDate = new SimpleDateFormat("yyyy-MM-dd");            
//		Calendar c = Calendar.getInstance();        
//		c.add(Calendar.DATE, 3);  // add 3 days to get to Thurs
//		String thursday = (String)(formattedDate.format(c.getTime()));
//		c.set(Calendar.HOUR_OF_DAY, 7);
//		c.set(Calendar.MINUTE, 0);
//		c.set(Calendar.SECOND, 0);
//		long milli = c.getTimeInMillis();
//		// parse schedule json to get tournaments
//		String url = "https://statdata.pgatour.com/r/current/schedule-v2.json";
//		JSONObject schedule = JsonReader.readJsonFromUrl(url);
//		JSONArray tournaments = schedule.getJSONArray("years").getJSONObject(0).getJSONArray("tours").getJSONObject(0).getJSONArray("trns");
//
//		for(int index=0; index < tournaments.length(); index++){
//			JSONObject tournament = tournaments.getJSONObject(index);
//			
//			//check 3 things: start-date is this coming thurs, tournament is stroke play, and tournament is week's primary tournament
//			if(tournament.getJSONObject("date").getString("start").equals(thursday) && tournament.getString("format").equals("Stroke") && tournament.getString("primaryEvent").equals("Y")){
//				this.tourneyName = tournament.getJSONObject("trnName").getString("official");
//				this.tourneyID = tournament.getString("permNum");
//				this.startDate = milli;
//				break;
//			}
//		}
//	}
//	
//	public Map<Integer, Player> setup() throws IOException, JSONException, SQLException {
//		Map<Integer, Player> players = new HashMap<Integer, Player>();
//		if(this.tourneyID != null){
//			String url = "https://statdata.pgatour.com/r/" + tourneyID + "/field.json";
//			JSONObject field = JsonReader.readJsonFromUrl(url);
//			JSONArray players_json = field.getJSONObject("Tournament").getJSONArray("Players");
//			for(int i = 0; i < players_json.length(); i++){
//				JSONObject player = players_json.getJSONObject(i);
//				int id = Integer.parseInt(player.getString("TournamentPlayerId"));
//				String name = player.getString("PlayerName");
//				String names[] = name.split(", ");
//				String name_fl;
//				try{
//					name_fl = names[1] + " " + names[0];
//				}
//				catch(Exception e){
//					name_fl = names[0];
//				}
//				String rank_url = "https://statdata.pgatour.com/r/stats/2018/186.json";
//				JSONObject world_rankings_json = JsonReader.readJsonFromUrl(rank_url);
//				JSONArray ranked_players = world_rankings_json.getJSONArray("tours").getJSONObject(0).getJSONArray("years").getJSONObject(0).getJSONArray("stats").getJSONObject(0).getJSONArray("details");
//				boolean checked = false;
//				String country = "";
//				double salary = 0;
//				for(int q=0; q < ranked_players.length(); q++){
//					JSONObject r_player = ranked_players.getJSONObject(q);
//					if(id == Integer.parseInt(r_player.getString("plrNum"))){
//						String points = r_player.getJSONObject("statValues").getString("statValue2");
//						country = r_player.getJSONObject("statValues").getString("statValue5");
//						salary = Math.round(Double.parseDouble(points) * 10);
//						if(salary < 80.0)
//							salary = 80.0;
//						checked = true;
//						break;
//					}
//				}
//				if(!checked){
//					salary = 80.0;
//				}
//				
//				Player p = new Player(id, name_fl, tourneyID);
//				p.set_ppg_salary(salary);
//				p.setCountry(country);
//				players.put(id, p);
//			}
//		}
//		this.players_list = players;
//		return players;
//	}
//	
//	public void savePlayers(){
//		try {
//			PreparedStatement delete_old_rows = sql_connection.prepareStatement("delete from player where sport_type=?");
//			delete_old_rows.setString(1, this.sport);
//			delete_old_rows.executeUpdate();
//			log("deleted " + this.sport + " players from old contests");
//
//			if(this.getPlayerHashMap() == null)
//				return;
//			
//			for(Player player : this.getPlayerHashMap().values()){
//				
//				PreparedStatement save_player = sql_connection.prepareStatement("insert into player(id, name, sport_type, gameID, team_abr, salary, points, bioJSON) values(?, ?, ?, ?, ?, ?, ?, ?)");				
//				save_player.setInt(1, player.getPGA_ID());
//				save_player.setString(2, player.getName());
//				save_player.setString(3, sport);
//				save_player.setString(4, player.getTourneyID());
//				save_player.setString(5, player.getCountry());
//				save_player.setDouble(6, player.getSalary());
//				save_player.setDouble(7, player.getPoints());
//				JSONObject emptyJSON = new JSONObject();
//				save_player.setString(8, emptyJSON.toString());
//				save_player.executeUpdate();	
//			}
//			
//			log("added " + sport + " players to DB");
//		}
//		catch (Exception e) {
//			Server.exception(e);
//		}
//	}
//
//	public boolean scrapeScores(String tourneyID) throws JSONException, IOException, SQLException{
//		String url = "https://statdata.pgatour.com/r/" + tourneyID + "/2018/leaderboard-v2.json";
//		JSONObject leaderboard = JsonReader.readJsonFromUrl(url);
//		if(leaderboard == null){
//			log("couldn't connect to pgatour API");
//			return false;
//		}
//		boolean finished = leaderboard.getJSONObject("leaderboard").getBoolean("is_finished");
//		JSONArray players = leaderboard.getJSONObject("leaderboard").getJSONArray("players");
//		ResultSet playerScores = db.getPlayerScores(this.sport);
//		while(playerScores.next()){
//			int score;
//			boolean in_leaderboard = false;
//			int id = playerScores.getInt(1);
//			for(int i=0; i < players.length(); i++){
//				JSONObject player = players.getJSONObject(i);
//				int player_id = Integer.parseInt(player.getString("player_id"));
//				if(id == player_id){
//					in_leaderboard = true;
//					String status = player.getString("status");
//					JSONObject data = new JSONObject();
//					JSONObject empty = new JSONObject();
//					data.put("1", empty);
//					data.put("2", empty);
//					data.put("3", empty);
//					data.put("4", empty);
//					data.put("overall", 0);
//					if(status.equals("cut"))
//						score = -888;
//					else if(status.equals("wd"))
//						score = -999;
//					else{
//						try{
//							score = player.getInt("total");
//						}
//						catch(JSONException e){
//							e.printStackTrace();
//							score = -888;
//						}
//					}	
//					db.editPoints(score, player_id, this.sport);
//					break;
//				}
//			}
//			if(!in_leaderboard){
//				score = -777;
//				db.editPoints(score, id, this.sport);	
//			}
//		}
//		return finished;		
//	}
//	
//	public JSONArray updateScoresDB() throws SQLException, JSONException{
//		
//		PreparedStatement worst_score = sql_connection.prepareStatement("select points from player where sport_type=? order by points DESC limit 1");
//		worst_score.setString(1, "GOLF");
//		ResultSet score = worst_score.executeQuery();
//		int worstScore = 0;
//		while(score.next()){
//			worstScore = score.getInt(1);
//		}
//		log("worst score in tournament: " + worstScore);
//		log("normalizing scores...");
//		
//		ResultSet playerScores = db.getPlayerScores(this.sport);
//		JSONArray player_map = new JSONArray();
//		while(playerScores.next()){
//			JSONObject player = new JSONObject();
//			int id = playerScores.getInt(1);
//			int points = (int) playerScores.getDouble(2);
//			
//			// -999 means WD
//			if(points==-999){
//				player.put("id", id);
//				player.put("score_raw", "WD");
//				player.put("score_normalized", 0);
//				player_map.put(player);
//			}
//			else if(points==-888){
//				player.put("id", id);
//				player.put("score_raw", "CUT");
//				player.put("score_normalized", 0);
//				player_map.put(player);
//			}
//			else if(points== -777){
//				player.put("id", id);
//				player.put("score_raw", "INACTIVE");
//				player.put("score_normalized", 0);
//				player_map.put(player);
//			}
//			else{
//				int normalizedScore = normalizeScore(points, worstScore);
//				player.put("id", id);
//				player.put("score_raw", Integer.toString(points));
//				player.put("score_normalized", normalizedScore);
//				player_map.put(player);
//			}
//		}
//		return player_map;
//	}
//	
//	public int normalizeScore(int score, int worstScore){
//		int normalizedScore = worstScore - score + 1;
//		return normalizedScore;
//	}
//	
//	public JSONObject createTeamsPariMutuel(Long deadline) throws JSONException{
//		JSONObject fields = new JSONObject();
//		fields.put("category", "FANTASYSPORTS");
//		fields.put("sub_category", "GOLF");
//		fields.put("contest_type", "PARI-MUTUEL");
//		fields.put("progressive", "");
//		String title = this.getTourneyName() + " | Team Competition";
//		fields.put("title", title);
//		fields.put("description", "Place your bet on which team will accumulate the best score!");
//		fields.put("rake", 5);
//		fields.put("cost_per_entry", 0.00000001);
//		fields.put("registration_deadline", deadline);
//		
//		//get 30 most expensive golfers
//		Map<Integer, ArrayList<String>> players = new HashMap<>();
//		ResultSet top_players = null;
//		try {
//			PreparedStatement get_players = sql_connection.prepareStatement("select id, name from player where sport_type=? order by salary DESC limit 30");
//			get_players.setString(1, "GOLF");
//			top_players = get_players.executeQuery();
//			
//			while(top_players.next()){
//				int row = top_players.getRow();
//				String id = String.valueOf(top_players.getInt(1));
//				String name = top_players.getString(2);
//				ArrayList<String> golfer = new ArrayList<>();
//				golfer.add(id);
//				golfer.add(name);
//				players.put(row, golfer);
//			}
//			
//			JSONArray teams = new JSONArray();
//			int num_teams = 5;
//			int rounds = 6;
//			//create 5 teams, 6 rounds draft 
//			for(int i=1; i<=num_teams; i++){
//				JSONObject team = new JSONObject();
//				team.put("id", i);
//				ArrayList<String> names = new ArrayList<>();
//				ArrayList<Integer> golfer_ids = new ArrayList<>();
//				for(int round=1; round<=rounds; round++){
//					int pick;
//					//snake draft picking algorithm
//					if(round % 2 == 0)
//						pick = (round * num_teams) - i + 1;
//					else
//						pick = ((round - 1) * num_teams) + i;
//					
//					ArrayList<String> golfer = players.get(pick);
//					int id = Integer.parseInt(golfer.get(0));
//					golfer_ids.add(id);
//					String name = golfer.get(1);
//					names.add(name);
//				}
//				String desc = names.get(0) + ", " + names.get(1) + ", " + names.get(2) + ", " + names.get(3) + ", " + names.get(4);
//				team.put("description", desc);
//				team.put("player_ids", golfer_ids.toString());
//				teams.put(team);
//			}
//			JSONObject tie = new JSONObject();
//			tie.put("id", 6);
//			tie.put("description", "Tie");
//			teams.put(tie);
//			
//			fields.put("option_table", teams);
//		}
//		catch(Exception e){
//			e.printStackTrace();
//		}
//		
//		return fields;
//	}
//	
//	
//	public JSONObject createPariMutuel(Long deadline, String round) throws JSONException{
//		JSONObject fields = new JSONObject();
//		fields.put("category", "FANTASYSPORTS");
//		fields.put("sub_category", "GOLFPROPS");
//		fields.put("contest_type", "PARI-MUTUEL");
//		
//		String progressive;
//		switch(round){
//	        case "1":
//	            progressive = "THURSDAY";
//	            break;
//	        case "2":
//	        	progressive = "FRIDAY";
//	        	break;
//	        case "3":
//	        	progressive = "SATURDAY";
//	        	break;
//	        case "4":
//	        	progressive = "SUNDAY";
//	        	break;
//	        default:
//	        	progressive = "";
//	        	break;
//		}
//		
//		fields.put("progressive", progressive);
//		String title = this.getTourneyName() + " - Top Score in Round " + round;
//		fields.put("title", title);
//		fields.put("description", "Place your bet on who you will think will have the top score in round " + round);
//		fields.put("rake", 5);
//		fields.put("cost_per_entry", 0.00000001);
//		fields.put("registration_deadline", deadline);
//		JSONArray option_table = new JSONArray(); 
//		ResultSet top_players = null;
//		
//		JSONObject none_above = new JSONObject();
//		none_above.put("description", "Any Other Golfer");
//		none_above.put("id", 1);
//		option_table.put(none_above);
//		
//		JSONObject tie = new JSONObject();
//		tie.put("description", "Tie");
//		tie.put("id", 2);
//		option_table.put(tie);
//	
//		int index = 3;
//		try {
//			PreparedStatement get_players = sql_connection.prepareStatement("select id, name from player where sport_type=? order by salary DESC limit 40");
//			get_players.setString(1, "GOLF");
//			top_players = get_players.executeQuery();
//			while(top_players.next()){
//				JSONObject player = new JSONObject();
//				String name = top_players.getString(2);
//				String name2 = Normalizer.normalize(name, Normalizer.Form.NFD);
//				String nameNormalized = name2.replaceAll("[^\\p{ASCII}]", "");
//				player.put("description", nameNormalized);
//				player.put("id", index);
//				player.put("player_id", top_players.getInt(1));
//				option_table.put(player);
//				index += 1;
//			}
//			fields.put("option_table", option_table);
//		}
//		catch(Exception e){
//			e.printStackTrace();
//		}
//		
//		return fields;
//	}
//	
//	public void checkPariMutuelStatus(ArrayList<Integer> pari_contest_ids) throws Exception{
//
//		for(Integer contest_id : pari_contest_ids){
//			// get the round for the contest
//			PreparedStatement getRound = sql_connection.prepareStatement("select title from contest where id=?");
//			getRound.setInt(1, contest_id);
//			ResultSet result = getRound.executeQuery();
//			String title = null;
//			while(result.next()){
//				title = result.getString(1);
//			}
//			String round = title.split(" - Top Score in Round ")[1];
//			
//			String url = "https://statdata.pgatour.com/r/" + this.getLiveTourneyID() + "/2018/leaderboard-v2.json";
//			JSONObject leaderboard = JsonReader.readJsonFromUrl(url).getJSONObject("leaderboard");
//			String roundStatus = leaderboard.getString("round_state");
//			// if the round status is complete/official get the round
//			if(roundStatus.equals("Official") && String.valueOf(leaderboard.getInt("current_round")).equals(round)){
//			
//				log("settling contest with id " + contest_id + " (Round " + round + ")");
//				JSONObject pari_fields = this.settlePariMutuel(contest_id, round);
//				MethodInstance pari_method = new MethodInstance();
//				JSONObject pari_output = new JSONObject("{\"status\":\"0\"}");
//				pari_method.input = pari_fields;
//				pari_method.output = pari_output;
//				pari_method.session = null;
//				pari_method.sql_connection = sql_connection;
//				try{
//					Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
//					c.newInstance(pari_method);
//				}
//				catch(Exception e){
//					e.printStackTrace();
//				}
//			}
//		}
//	}	
//	
//	public JSONObject settlePariMutuel(int contest_id, String round) throws Exception{
//		JSONObject fields = new JSONObject();
//		fields.put("contest_id", contest_id);
//		DB db = new DB(sql_connection);
//		
//		String url = "https://statdata.pgatour.com/r/" + this.getLiveTourneyID() + "/2018/leaderboard-v2.json";
//		JSONObject leaderboard = JsonReader.readJsonFromUrl(url);
//		int par = Integer.parseInt(leaderboard.getJSONObject("leaderboard").getJSONArray("courses").getJSONObject(0).getString("par_total"));
//		log("par for the course is " + par);
//		JSONArray players = leaderboard.getJSONObject("leaderboard").getJSONArray("players");
//		int best_score = 500;
//		ArrayList<Integer> best_players = new ArrayList<Integer>();
//		for(int i=0; i < players.length(); i++){
//			JSONObject player = players.getJSONObject(i);
//			int player_id = Integer.parseInt(player.getString("player_id"));
//			int score = 999;
//			try{
//				if(player.getJSONArray("rounds").getJSONObject(Integer.parseInt(round) - 1).getInt("round_number") == Integer.parseInt(round) && !player.getString("status").equals("wd") && !player.getString("status").equals("cut") )
//					score = (player.getJSONArray("rounds").getJSONObject(Integer.parseInt(round) - 1).getInt("strokes") - par);
//			}
//			catch(JSONException e){
//				log("player status: " + player.getString("status"));
//				score = 999;
//			}
//			if(score < best_score){
//				best_score = score;
//				best_players.clear();
//				best_players.add(player_id);
//			}
//			else if(score == best_score){
//				best_players.add(player_id);
//			}
//		}		
//	
//		JSONObject contest = db.select_contest(contest_id);
//		JSONArray option_table = new JSONArray(contest.getString("option_table"));
//		int winning_outcome = 1;
//	
//		log("Top score from round " + round + " is " + best_score);
//
//		if(best_players.size() > 1){
//			//tie is correct answer;
//			winning_outcome = 2;
//			log("winning outcome=2 because of tie");
//			fields.put("winning_outcome", winning_outcome);
//			return fields;
//		}
//		else{
//			for(Integer player_table_ID : best_players){
//				for (int i=0; i<option_table.length(); i++){
//					JSONObject option = option_table.getJSONObject(i);
//					int option_id = option.getInt("id");
//					try{
//						int player_id = option.getInt("player_id");
//						if(player_id == player_table_ID){
//							winning_outcome = option_id;
//							fields.put("winning_outcome", winning_outcome);
//							log("winning outcome is " + option.getString("description"));
//							return fields;	
//						}
//					}	
//					catch(Exception e){
//						continue;
//					}			
//				}
//			}
//		}
//		
//		fields.put("winning_outcome", winning_outcome);
//		log("winning outcome is any other player");
//		return fields;
//	}
//	
//	class Player {
//		public String method_level = "admin";
//		private String name;
//		private String country;
//		private int pga_id;
//		private String tourney_ID;
//		private double fantasy_points = 0;
//		private double salary;
//		private String birthString;
//		private String height;
//		private String weight;
//		private String pos;
//		private JSONArray last_five_games;
//		private JSONObject career_stats;
//		private JSONObject year_stats;
//		private JSONObject bio;
//		
//		// constructor
//		public Player(int id, String n, String tourney_ID){
//			this.pga_id = id;
//			this.name = n;
//			this.tourney_ID = tourney_ID;
//		}
//	
//		// methods
//		public double getPoints(){
//			return fantasy_points;
//		}
//		
//		public void setCountry(String c){
//			this.country = c;
//		}
//		
//		public void setScore(double score){
//			this.fantasy_points = score;
//		}
//		
//		public String getCountry(){
//			return country;
//		}
//		
//		public int getPGA_ID(){
//			return pga_id;
//		}
//		public String getTourneyID(){
//			return tourney_ID;
//		}
//		
//		public double getSalary(){
//			return salary;
//		}
//		
//		public String getName(){
//			return name;
//		}
//		
//		public JSONArray getGameLogs(){
//			return last_five_games;
//		}
//		
//		public JSONObject getCareerStats(){
//			if(career_stats == null){
//				return null;
//			}
//			return career_stats;
//		}
//		
//		public JSONObject getYearStats(){
//			if(year_stats == null){
//				return null;
//			}
//			return year_stats;
//		}
//		public String getBirthString(){
//			return birthString;
//		}
//		public String getPosition(){
//			return pos;
//		}
//		public String getHeight(){
//			return height;
//		}
//		public String getWeight(){
//			return weight;
//		}
//		public void set_ppg_salary(double pts){
//			this.salary = pts;
//		}
//		public void createBio() throws JSONException{
//			JSONObject bio = new JSONObject();
//			bio.put("birthString", this.getBirthString());
//			bio.put("height", this.getHeight());
//			bio.put("Weight", this.getWeight());
//			bio.put("pos", this.getPosition());
//			bio.put("last_five_games", this.getGameLogs());
//			bio.put("career_stats", this.getCareerStats());
//			bio.put("year_stats", this.getYearStats());
//	
//			this.bio = bio;		
//		}
//		public JSONObject getBio(){
//			return this.bio;
//		}
//			
//	}
//}
