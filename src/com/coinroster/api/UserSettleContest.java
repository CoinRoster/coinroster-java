package com.coinroster.api;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.NotifyAdmin;

public class UserSettleContest extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public UserSettleContest(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------

			int 
			
			contest_id = input.getInt("contest_id"),
			winning_outcome = input.getInt("winning_outcome");

			String message_body;

//			MethodInstance settle_method = new MethodInstance();
//			settle_method.input = input;
//			settle_method.output = output;
//			settle_method.session = null;
//			settle_method.sql_connection = sql_connection;
			
			try{
				log("Settling voting round");
				Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
				c.newInstance(method);
				
				if(db.is_voting_contest(contest_id)) {
					JSONObject betting_input = new JSONObject();
					betting_input.put("winning_outcome", winning_outcome);
					betting_input.put("contest_id", db.get_original_contest(contest_id));
					
					MethodInstance betting_method = new MethodInstance();
					betting_method.input = betting_input;
					betting_method.output = output;
					betting_method.session = null;
					betting_method.sql_connection = sql_connection;
					
					log("Settling betting round round");
					Constructor<?> b = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
					c.newInstance(betting_method);
				}
			}
			catch(Exception e){
				e.printStackTrace(System.out);
				output.put("error", "Invalid winning outcome. An admin has been notified");
				message_body = "The contest: <b>" + contest_id + "</b> was not settled properly from the user-settling panel.";
				new NotifyAdmin(sql_connection, "User-Settle Failure: " + contest_id, message_body);
				break method;
			}
			

			output.put("status", "1");
			message_body = "The contest: <b>" + contest_id + "</b> settled successfully by the user with winning-outcome: " + winning_outcome;
			new NotifyAdmin(sql_connection, "User-Settle Failure: " + contest_id, message_body);
						
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}