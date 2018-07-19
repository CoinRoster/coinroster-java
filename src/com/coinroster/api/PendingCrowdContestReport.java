package com.coinroster.api;

import java.sql.Connection;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.bots.CrowdSettleBot;

public class PendingCrowdContestReport extends Utils {
	
	public static String method_level = "admin";
	
	@SuppressWarnings("unused")
	public PendingCrowdContestReport(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			ArrayList<Integer> pending_contest = db.get_pending_voting_contests();
			JSONArray contest_report = new JSONArray();
			CrowdSettleBot crowd_bot = new CrowdSettleBot(sql_connection);
			
			for (int contest_id: pending_contest) {
				JSONObject entries = crowd_bot.settlePariMutuel(contest_id);
				JSONObject contest = db.select_contest(contest_id);
				contest.put("entries", entries);
				contest_report.put(contest);
			}
			
			
			output.put("contest_report", contest_report);			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
			
		}	
}
