package com.coinroster.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.coinroster.Server;
import com.coinroster.Utils;

public class BuildLobby extends Utils
	{
	public BuildLobby(Connection sql_connection)
		{
		try {
			log("Building lobby");
			
			String 
			
			domain_directory = Server.html_path,
			factory_path = domain_directory + "/factory/";

	    	String
	    	
	    	lobby_template = Utils.read_to_string(factory_path + "lobby_template.html"),
	    	category_template = Utils.read_to_string(factory_path + "category_template.html"),
	    	sub_category_template = Utils.read_to_string(factory_path + "sub_category_template.html");
	    	
	    	StringBuilder lobby_builder = new StringBuilder();
	    	
	    	PreparedStatement select_categories = sql_connection.prepareStatement("select * from category order by position asc");
			ResultSet category_rs = select_categories.executeQuery();
			
			boolean visible_categories = false;

			while (category_rs.next())
				{
				boolean visible_sub_categories = false;
				
				String category_code = category_rs.getString(1);
				String category_description = category_rs.getString(2);
				
				StringBuilder category_html = new StringBuilder();
				category_html.append(category_template.replace("<!-- factory:category_description -->", category_description));
				
				PreparedStatement select_sub_categories = sql_connection.prepareStatement("select * from sub_category where category = ? order by created asc");
				select_sub_categories.setString(1, category_code);
				ResultSet sub_category_rs = select_sub_categories.executeQuery();
				
				while (sub_category_rs.next())
					{
					String sub_category_code = sub_category_rs.getString(3);
					String sub_category_description = sub_category_rs.getString(4);
					int active_flag = sub_category_rs.getInt(5);
					String image_name = sub_category_rs.getString(6);
					
					PreparedStatement count_contests = sql_connection.prepareStatement("select status, count(*) from contest where category = ? and sub_category = ? group by status");
					count_contests.setString(1, category_code);
					count_contests.setString(2, sub_category_code);
					ResultSet count_contests_rs = count_contests.executeQuery();

					int 
					
					open_contests = 0,
					in_play_contests = 0;
					
					while (count_contests_rs.next())
						{
						int 
						
						contest_status = count_contests_rs.getInt(1),
						number_of_contests = count_contests_rs.getInt(2);
						
						switch (contest_status)
			                {
			                case 1: // Reg open
			                	open_contests = number_of_contests;
			                    break;
			                case 2: // In play
			                	in_play_contests = number_of_contests;
			                    break;
			                }
						}
					
					if (active_flag == 0) continue;
					
					visible_sub_categories = true;
					
					String sub_category_html = sub_category_template;
					
					sub_category_html = sub_category_html.replace("factory:category_href", "/lobby.html#category=" + category_code + "&amp;sub_category=" + sub_category_code);
					sub_category_html = sub_category_html.replace("factory:image_path", "/img/lobby_tiles/" + image_name);
					sub_category_html = sub_category_html.replace("<!-- factory:sub_category_description -->", sub_category_description);
					
					String open_contests_string = " open contests";
					if (open_contests == 1) open_contests_string = " open contest";
					sub_category_html = sub_category_html.replace("<!-- factory:open_contests -->", open_contests + open_contests_string);
					if (open_contests > 0) sub_category_html = sub_category_html.replace("open_contests_class", "orange");
					
					String in_play_contests_string = "";
					if (in_play_contests > 0) in_play_contests_string = in_play_contests + " in play";
					sub_category_html = sub_category_html.replace("<!-- factory:in_play_contests -->", in_play_contests_string);
					
					category_html.append(sub_category_html);
					}
				
				if (visible_sub_categories) 
					{
					lobby_builder.append(category_html);
					visible_categories = true;
					}
				}
			
			if (!visible_categories)
				{
				lobby_builder.append(category_template.replace("<!-- factory:category_description -->", "Lobby"));
				lobby_builder.append("<table id=\"contest_report_table\" class=\"contest_table\"><tr><td style=\"text-align:left\">Contests will be posted in the next few days. Please check back soon!</td></tr></table>");
				}
			
			// ---------------------------------------------------------------------------------------
	    	
			String lobby_html = lobby_builder.toString();
			
	        Utils.write_to_string(domain_directory + "/lobby.html", lobby_template.replace("<!-- factory:lobby_html -->", lobby_html));
			}
		catch (Exception e)
			{
			Server.exception(e);
			}
		}
	}
