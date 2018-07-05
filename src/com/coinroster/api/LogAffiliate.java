package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import org.json.JSONObject;

import com.coinroster.MethodInstance;
import com.coinroster.Utils;

public class LogAffiliate extends Utils
	{
	public static String method_level = "guest";
	public LogAffiliate(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Connection sql_connection = method.sql_connection;

		method : {
			
			String affiliate = input.getString("affiliate");

			if (affiliate == null || affiliate == ""){
				output.put("error", "couldn't find affiliate");
				Utils.log(output.get("error"));
				break method;
			}
			
		    PreparedStatement add_affiliate = sql_connection.prepareStatement("insert into affiliate (source) values(?)");				
		    add_affiliate.setString(1, affiliate);
			add_affiliate.execute();
		
            output.put("status", "1");
            
			} 
		method.response.send(output);
		}
	}