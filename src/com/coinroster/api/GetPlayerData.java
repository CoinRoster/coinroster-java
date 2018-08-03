package com.coinroster.api;

import java.sql.Connection;

import org.json.JSONObject;
import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Utils;

public class GetPlayerData extends Utils {
	
	public static String method_level = "guest";
	public GetPlayerData(MethodInstance method) throws Exception {
		
		JSONObject 
		input = method.input,
		output = method.output;		
		Connection sql_connection = method.sql_connection;
		
		DB db = new DB(sql_connection);

		method : {
			
			String player_id = input.getString("player_id");
			String sport = input.getString("sport");
			
			try{
				JSONObject data = db.get_player_dashboard_data(player_id, sport);
				output.put("data", data);
			}catch(Exception e){
				Server.exception(e);
				output.put("error", e.getMessage());
				output.put("status", "0");
				break method;
			}			
			output.put("status", "1");
//------------------------------------------------------------------------------------

		} method.response.send(output);
	}
}
