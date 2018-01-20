package com.coinroster.api;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import com.coinroster.Server;
import com.coinroster.Utils;
import com.coinroster.api.BasketballContest.Player;

public class SavePlayers extends Utils{
	
	public static String method_level = "admin";
	public SavePlayers() throws Exception 
		{
		
		BasketballContest contest = new BasketballContest(001);
		//call ESPN API to get gameIDs
		contest.getGameIDs();
	
		// setup() returns a list of all players that should be available in the contest, and each player has
		// attributes above such as name, ESPN_ID, team_abr, ppg, salary 
		// setup() only needs to be run once per contest creation
		Map<Integer, Player> contest_data = contest.setup();
		
		Connection sql_connection = null;
		try {
			sql_connection = Server.sql_connection();
			PreparedStatement delete_old_rows = sql_connection.prepareStatement("delete from player");
			delete_old_rows.executeUpdate();
			System.out.println("deleted players from old contests");

			for(Player player : contest_data.values()){
				
				PreparedStatement save_player = sql_connection.prepareStatement("insert into player(id, name, sport_type, team_abr, salary, bioJSON) values(?, ?, ?, ?, ?, ?)");				
				save_player.setInt(1, player.getEPSN_ID());
				save_player.setString(2, player.getName());
				save_player.setString(3, "BASKETBALL");
				save_player.setString(4, player.getTeam());
				save_player.setDouble(5, player.getSalary());
				save_player.setString(6, player.getBio().toString());
				System.out.println("added "+ player.getName() + " to DB");
				save_player.executeUpdate();	
			}
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
}
