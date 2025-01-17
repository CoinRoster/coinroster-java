package com.coinroster.api;

import java.sql.Connection;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

/**
 * Get the external address for deposits and withdrawals
 * 
 * @deprecated This is handled by CGS
 *
 */
public class GetExtAddress extends Utils
	{
	public static String method_level = "standard";
	
	/**
	 * Get the external address for deposits and withdrawals
	 * 
	 * @deprecated
	 * @param method
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	public GetExtAddress(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			boolean get_for_active_user = input.getBoolean("get_for_active_user");
			
			String user_id;
			
			if (get_for_active_user) user_id = session.user_id();
			else user_id = db.get_id_for_username("internal_cash_register");
			
			JSONObject user = db.select_user("id", user_id);
			
			if (user != null)
				{
				String ext_address = user.getString("ext_address");
				
				if (ext_address == null) 
					{
					output.put("has_ext_address", "0");
					ext_address = "";
					}
				else output.put("has_ext_address", "1");
				
				output.put("ext_address", ext_address);
				output.put("status", "1");
				}
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}