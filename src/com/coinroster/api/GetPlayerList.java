package com.coinroster.api;

import java.sql.Connection;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class GetPlayerList extends Utils{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public GetPlayerList(MethodInstance method) throws Exception {
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;
		DB db = new DB(sql_connection);

		method : {	
//------------------------------------------------------------------------------------
			try{
				String sport = input.getString("sport");
				JSONArray players = db.get_all_players(sport);
				output.put("player_list", players);
				output.put("status", "1");
			}catch(Exception e){
				log(e.toString());
				output.put("error", e.toString());
				output.put("status", "0");
			}
//------------------------------------------------------------------------------------
		}
		method.response.send(output);
	}
}