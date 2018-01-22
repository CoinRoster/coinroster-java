package com.coinroster.api;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Utils;
import com.coinroster.api.BasketballContest.Player;

public class PlayersList extends Utils{
	
	public static String method_level = "admin";
	private BasketballContest contest;
	private String sport;
	public PlayersList(String sport) throws Exception 
		{
		
		this.sport = sport;
		this.contest = new BasketballContest(001);
		//call ESPN API to get gameIDs
		contest.getGameIDs();
	
		// setup() returns a list of all players that should be available in the contest, and each player has
		// attributes above such as name, ESPN_ID, team_abr, ppg, salary 
		// setup() only needs to be run once per contest creation
		Map<Integer, Player> contest_data = contest.setup();
		
		Connection sql_connection = null;
		try {
			sql_connection = Server.sql_connection();
			PreparedStatement delete_old_rows = sql_connection.prepareStatement("delete from player where sport_type=?");
			delete_old_rows.setString(1, sport);
			delete_old_rows.executeUpdate();
			System.out.println("deleted " + sport + " players from old contests");

			for(Player player : contest_data.values()){
				
				PreparedStatement save_player = sql_connection.prepareStatement("insert into player(id, name, sport_type, team_abr, salary, bioJSON) values(?, ?, ?, ?, ?, ?)");				
				save_player.setInt(1, player.getEPSN_ID());
				save_player.setString(2, player.getName());
				save_player.setString(3, sport);
				save_player.setString(4, player.getTeam());
				save_player.setDouble(5, player.getSalary());
				save_player.setString(6, player.getBio().toString());
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
	public void autoCreateContest(String category, String contest_type, String progressive_code, 
			String title, String desc, double rake, double cost_per_entry, String settlement_type, 
			int salary_cap, int min_users, int max_users, int entries_per_user, int roster_size, 
			String odds_source, String score_header, double[] payouts) throws Exception{

		MethodInstance method = new MethodInstance();
		JSONObject 
		output = method.output;

		Connection sql_connection = Server.sql_connection();

		DB db = new DB(sql_connection);

		method : {

			if (title.length() > 255)
			{
				output.put("error", "Title is too long");
				break method;
			}

			if (this.contest.getEarliestGame() - System.currentTimeMillis() < 1 * 60 * 60 * 1000)
			{
				output.put("error", "Registration deadline must be at least 1 hour from now");
				break method;
			}

			if (rake < 0 || rake >= 100)
			{
				output.put("error", "Rake cannot be < 0 or > 100");
				break method;
			}

			rake = divide(rake, 100, 0); // convert to %

			if (cost_per_entry == 0)
			{
				output.put("error", "Cost per entry cannot be 0");
				break method;
			}

			if (progressive_code.equals("")) progressive_code = null; // default value
			else
			{
				JSONObject progressive = db.select_progressive(progressive_code);

				if (progressive == null)
				{
					output.put("error", "Invalid Progressive");
					break method;
				}

				if (!progressive.getString("category").equals(category) || !progressive.getString("sub_category").equals(sport))
				{
					output.put("error", "Progressive belongs to a different category");
					break method;
				}
			}

			log("Contest parameters:");

			log("category: " + category);
			log("sub_category: " + sport);
			log("progressive: " + progressive_code);
			log("contest_type: " + contest_type);
			log("title: " + title);
			log("description: " + desc);
			log("registration_deadline: " + this.contest.getEarliestGame());
			log("rake: " + rake);
			log("cost_per_entry: " + cost_per_entry);

			if(contest_type.equals("ROSTER")){
				if (min_users < 2)
				{
					output.put("error", "Invalid value for [min users]");
					break method;
				}
				if (max_users < min_users && max_users != 0) // 0 = unlimited
				{
					output.put("error", "Invalid value for [max users]");
					break method;
				}

				if (roster_size < 0)
				{
					output.put("error", "Roster size cannobe be negative");
					break method;
				}

				if (entries_per_user < 0)
				{
					output.put("error", "Invalid value for [entries per user]");
					break method;
				}

				if (score_header.equals(""))
				{
					output.put("error", "Please choose a score column header");
					break method;
				}

				JSONArray option_table = new JSONArray();
				for(Player p : this.contest.getPlayerHashMap().values()){
					JSONObject player = new JSONObject();
					player.put("name", p.getName() + " " + p.getTeam());
					player.put("price", p.getSalary());
					player.put("count", 0);
					player.put("id", p.getEPSN_ID());
					option_table.put(player);
				}
								
				switch (settlement_type)
				{
				case "HEADS-UP":

					if (min_users != 2 || max_users != 2)
					{
						output.put("error", "Invalid value(s) for number of users");
						break method;
					}
					break;

				case "DOUBLE-UP": break;

				case "JACKPOT": break;

				default:

					output.put("error", "Invalid value for [settlement type]");
					break method;

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
					create_contest.setString(2, sport);
					create_contest.setString(3, progressive_code);
					create_contest.setString(4, contest_type);
					create_contest.setString(5, title);
					create_contest.setString(6, desc);
					create_contest.setLong(7, this.contest.getEarliestGame());
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
					System.out.println("added contest to db");
				}
				catch(Exception e){
					e.printStackTrace();
				}
	        }
		}
		method.response.send(output);
	}
}
