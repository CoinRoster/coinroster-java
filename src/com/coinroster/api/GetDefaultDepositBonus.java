package com.coinroster.api;

import java.sql.Connection;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

public class GetDefaultDepositBonus extends Utils
	{
	public static String method_level = "guest";
	@SuppressWarnings("unused")
	public GetDefaultDepositBonus(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			double deposit_bonus_cap = Double.parseDouble(Server.control.get("deposit_bonus_cap"));
			
			int deposit_bonus_rollover_multiple = Integer.parseInt(Server.control.get("deposit_bonus_rollover_multiple"));

			output.put("deposit_bonus_cap", deposit_bonus_cap);
			output.put("deposit_bonus_rollover_multiple", deposit_bonus_rollover_multiple);
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}