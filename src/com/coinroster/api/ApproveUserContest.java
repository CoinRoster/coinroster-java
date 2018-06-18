package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.BuildLobby;
import com.coinroster.internal.UpdateContestStatus;

public class ApproveUserContest extends Utils {
	
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public ApproveUserContest(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;
		
		DB db = new DB(sql_connection);

		method : {
			
			log(input.toString());
//------------------------------------------------------------------------------------
			if (input.getString("admin_approval").equals("1")) {
				log("Admin has approved contest " + input.getInt("contest_id"));
				new UpdateContestStatus(sql_connection, input.getInt("contest_id"), 1);
				new BuildLobby(sql_connection);
			} else {
				log("Admin has rejected contest " + input.getInt("contest_id"));
				PreparedStatement delete_contest = sql_connection.prepareStatement("delete from contest where id = ?");
				delete_contest.setInt(1, input.getInt("contest_id"));
				delete_contest.executeUpdate();
			}
			output.put("status", "1");
//------------------------------------------------------------------------------------

		} method.response.send(output);
	}
}