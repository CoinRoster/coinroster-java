package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class CreateProgressive extends Utils
	{
	public static String method_level = "admin";
	public CreateProgressive(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);
		
		method : {
			
//------------------------------------------------------------------------------------
		
            String
            
            category = input.getString("category"),
            sub_category = input.getString("sub_category"),
            code = input.getString("code"),
            payout_info = input.getString("payout_info");
           
            if (code.equals("")) 
            	{
                output.put("error", "Code cannot be empty");
            	break method;
            	}

            if (code.length() > 20) 
            	{
                output.put("error", "Code cannot be more than 20 chars");
            	break method;
            	}
            
            if (db.select_progressive(code) != null)
            	{
                output.put("error", "Code has already been used");
            	break method;
            	}
            
            if (payout_info.equals("")) 
	        	{
	            output.put("error", "Payout info cannot be empty");
	        	break method;
	        	}

            if (payout_info.length() > 500) 
	        	{
	            output.put("error", "Payout info cannot be more than 500 chars");
	        	break method;
	        	}
            log("test1");
            PreparedStatement select_category = sql_connection.prepareStatement("select * from category where code = ?");
            select_category.setString(1, category);
            ResultSet category_rs = select_category.executeQuery();

            if (!category_rs.next())
            	{
	            output.put("error", "Invalid category");
	        	break method;
            	}
            log("test2");

            PreparedStatement select_sub_category = sql_connection.prepareStatement("select * from sub_category where code = ?");
            select_sub_category.setString(1, sub_category);
            ResultSet sub_category_rs = select_sub_category.executeQuery();

            if (!sub_category_rs.next())
            	{
	            output.put("error", "Invalid sub-category");
	        	break method;
            	}
            log("test3");

            PreparedStatement create_progressive = sql_connection.prepareStatement("insert into progressive(created, created_by, category, sub_category, code, payout_info) values(?, ?, ?, ?, ?, ?)");
			create_progressive.setLong(1, System.currentTimeMillis());
			create_progressive.setString(2, session.user_id());
			create_progressive.setString(3, category);
			create_progressive.setString(4, sub_category);
			create_progressive.setString(5, code);
			create_progressive.setString(6, payout_info);
			create_progressive.executeUpdate();

            log("test4");
            output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}