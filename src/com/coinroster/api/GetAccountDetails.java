package com.coinroster.api;

import java.sql.Connection;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class GetAccountDetails extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public GetAccountDetails(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(method);

		method : {
			
//------------------------------------------------------------------------------------
		
			String user_id = session.user_id();
			
			String[] user_xref = db.select_user_xref("id", user_id);
			
			if (user_xref != null)
				{
				output.put("btc_balance", user_xref[1]);
				output.put("rc_balance", user_xref[2]);
				output.put("ext_address", user_xref[3]);
				output.put("email_address", user_xref[4]);
				output.put("email_ver_flag", user_xref[6]);
				output.put("newsletter_flag", user_xref[7]);
				
				output.put("status", "1");
				}
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}