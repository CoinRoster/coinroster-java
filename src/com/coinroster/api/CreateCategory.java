package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONObject;

import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class CreateCategory extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public CreateCategory(MethodInstance method) throws Exception 
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
            
            int active_flag = input.getInt("active_flag");
            
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
            if (active_flag != 0 && active_flag != 1)
            	{
            	output.put("error", "Active flag must be 0 or 1");
            	break method;
        		}
            
            PreparedStatement select_category = sql_connection.prepareStatement("select * from category where code = ? or description = ?");
            select_category.setString(1, code);
            select_category.setString(2, description);
            ResultSet rs = select_category.executeQuery();
            
            if (rs.next())
            	{
            	output.put("error", "Duplicate code or description!");
            	break method;
            	}
            
			PreparedStatement create_category = sql_connection.prepareStatement("insert into category(code, description, active_flag) values(?, ?, ?)");				
			create_category.setString(1, code);
			create_category.setString(2, description);
			create_category.setInt(3, active_flag);
			create_category.executeUpdate();
            
            output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}