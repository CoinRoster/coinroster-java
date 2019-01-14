package com.coinroster.api;

import java.sql.Connection;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

/**
 * Get the default referral offer from control from the admin panel.
 * 
 * @custom.access admin
 *
 */
public class GetDefaultOffer extends Utils
	{
	public static String method_level = "admin";
	
	/**
	 * Get the default referral offer from control from the admin panel.
	 * 
	 * @param method
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	public GetDefaultOffer(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			double default_referral_offer = Double.parseDouble(Server.control.get("default_referral_offer"));

			output.put("default_referral_offer", default_referral_offer);
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}