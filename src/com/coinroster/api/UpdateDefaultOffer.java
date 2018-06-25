package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

public class UpdateDefaultOffer extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public UpdateDefaultOffer(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			double default_referral_offer = input.getDouble("default_referral_offer");

            if (default_referral_offer < 0 || default_referral_offer > 1)
            	{
                output.put("error", "Default offer cannot be < 0 or > 1");
                break method;
            	}
            
            String default_referral_offer_string = Double.toString(default_referral_offer);
            
            PreparedStatement update_default_offer = sql_connection.prepareStatement("update control set value = ? where name = 'default_referral_offer'");
            update_default_offer.setString(1, default_referral_offer_string);
            update_default_offer.executeUpdate();
			
			Server.control.put("default_referral_offer", default_referral_offer_string);
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}