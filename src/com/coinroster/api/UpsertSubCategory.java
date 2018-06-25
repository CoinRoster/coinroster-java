package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONObject;

import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.BuildLobby;

public class UpsertSubCategory extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public UpsertSubCategory(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		method : {
			
//------------------------------------------------------------------------------------

            String
            
            category = input.getString("category"),
            code = input.getString("code"),
            id = category + "_" + code,
             
            description = input.getString("description"),
            
            image_base64 = input.getString("image_base64"),
            image_extension = input.getString("image_extension");
      
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
 
            PreparedStatement select_category = sql_connection.prepareStatement("select * from category where code = ?");
            select_category.setString(1, category);
            ResultSet rs = select_category.executeQuery();
            
            if (rs.next())
            	{
        		PreparedStatement select_sub_category = sql_connection.prepareStatement("select created, image_name from sub_category where id = ?");
        		select_sub_category.setString(1, id);
                ResultSet sub_category_rs = select_sub_category.executeQuery();
                
                boolean sub_category_exists = sub_category_rs.next();

                Long created = System.currentTimeMillis();
                String image_name = null;
                
                if (sub_category_exists) // if sub_category exists we want to preserve created timestamp and image_name
	            	{
	            	created = sub_category_rs.getLong(1);
	            	image_name = sub_category_rs.getString(2);
	            	}
            	
            	if (!image_extension.equals("")) // an image has been provided - we save image to disk and will write image_name to DB
            		{
            		image_name = id + "_" + System.currentTimeMillis() + "." + image_extension;
            		String image_path = Server.html_path + "/img/lobby_tiles/" + image_name;
            		write_bytes(image_path, base64_to_bytearray(image_base64));
            		}
            	else if (!sub_category_exists) // no image has been provided - we can only move forward if the sub_category already exists / has an image
	            	{
	            	output.put("error", "You must provide an image");
	            	break method;
	            	}

    			PreparedStatement create_sub_category = sql_connection.prepareStatement("replace into sub_category(id, category, code, description, active_flag, image_name, created) values(?, ?, ?, ?, ?, ?, ?)");
    			create_sub_category.setString(1, id);
    			create_sub_category.setString(2, category);
    			create_sub_category.setString(3, code);
    			create_sub_category.setString(4, description);
    			create_sub_category.setInt(5, active_flag);
    			create_sub_category.setString(6, image_name);
    			create_sub_category.setLong(7, created);
    			create_sub_category.executeUpdate();
            	}
            else
            	{
            	output.put("error", "Category does not exist!");
            	break method;
            	}
   
    		new BuildLobby(sql_connection);
    		
            output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}