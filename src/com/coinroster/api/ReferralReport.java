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

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			JSONArray referral_report = new JSONArray();
			
			PreparedStatement select_referrals = sql_connection.prepareStatement("select referral_key from referral");
			ResultSet result_set = select_referrals.executeQuery();
	  
			while (result_set.next())
				{
				String referral_key = result_set.getString(1);
				
				JSONObject referral = db.select_referral(referral_key);

				referral_report.put(referral);
				}
			
			output.put("referral_report", referral_report);
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}