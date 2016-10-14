package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class ReferralReport extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public ReferralReport(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(method);

		method : {
			
//------------------------------------------------------------------------------------
		
			JSONArray referral_report = new JSONArray();
			
			PreparedStatement select_referrals = sql_connection.prepareStatement("select * from referral");
			ResultSet result_set = select_referrals.executeQuery();
	  
			while (result_set.next())
				{
				String 
				
				referral_key = result_set.getString(1),
				referrer_id = result_set.getString(2),
				referrer_username = db.get_username_for_id(referrer_id),
				email_address = result_set.getString(4),
				referral_pgm = result_set.getString(5),
				created = result_set.getString(6);
				
				JSONObject referral = new JSONObject();

				referral.put("referral_key", referral_key);
				referral.put("referrer_username", referrer_username);
				referral.put("email_address", email_address);
				referral.put("referral_pgm", referral_pgm);
				referral.put("created", created);
				
				referral_report.put(referral);
				}
			
			output.put("referral_report", referral_report);
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}