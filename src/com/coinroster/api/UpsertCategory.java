package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.BuildLobby;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

public class UpsertCategory extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public UpsertCategory(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		method : {
			
//------------------------------------------------------------------------------------
		
            String
            
            code = input.getString("code"),
            description = input.getString("description");
           
            if (code.equals("")) 
            	{
                output.put("error", "Code cannot be empty");
            	break method;
            	}
            if (description.equals("")) 
	        	{
	            output.put("error", "Description cannot be empty");
	        	break method;
	        	}

			PreparedStatement create_category = sql_connection.prepareStatement("replace into category(code, description) values(?, ?)");				
			create_category.setString(1, code);
			create_category.setString(2, description);
			
			try {
				create_category.executeUpdate();
				}
			catch (MySQLIntegrityConstraintViolationException e)
				{
				output.put("error", "Category [" + code + "] already exists");
            	break method;
				}
			
			new BuildLobby(sql_connection);
            
            output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}