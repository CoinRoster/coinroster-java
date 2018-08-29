package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

public class UpdateDefaultDepositBonus extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public UpdateDefaultDepositBonus(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			double deposit_bonus_cap = input.getDouble("deposit_bonus_cap");
			int deposit_bonus_rollover_multiple = input.getInt("deposit_bonus_rollover_multiple");

            if (deposit_bonus_cap <= 0)
            	{
                output.put("error", "Deposit bonus cap must be greater than 0");
                break method;
            	}

            if (deposit_bonus_rollover_multiple <= 0)
            	{
                output.put("error", "Rollover must be greater than 0");
                break method;
            	}
            
            String 
            
            deposit_bonus_cap_string = Double.toString(deposit_bonus_cap),
            deposit_bonus_rollover_string = Integer.toString(deposit_bonus_rollover_multiple);
            
            PreparedStatement update_deposit_bonus_cap = sql_connection.prepareStatement("update control set value = ? where name = 'deposit_bonus_cap'");
            update_deposit_bonus_cap.setString(1, deposit_bonus_cap_string);
            update_deposit_bonus_cap.executeUpdate();

			Server.control.put("deposit_bonus_cap", deposit_bonus_cap_string);
			
            PreparedStatement update_deposit_bonus_rollover = sql_connection.prepareStatement("update control set value = ? where name = 'deposit_bonus_rollover_multiple'");
            update_deposit_bonus_rollover.setString(1, deposit_bonus_rollover_string);
            update_deposit_bonus_rollover.executeUpdate();

			Server.control.put("deposit_bonus_rollover_multiple", deposit_bonus_rollover_string);
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}