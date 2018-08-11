package com.coinroster.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.coinroster.ContestMethods;
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
	    	sub_category_wrapper_template = Utils.read_to_string(factory_path + "sub_category_wrapper_template.html"),
	    	sub_category_template = Utils.read_to_string(factory_path + "sub_category_template.html"),
	    	
	    	sitemap_template = Utils.read_to_string(factory_path + "sitemap_template.xml"),
	    	sitemap_url_template = Utils.read_to_string(factory_path + "sitemap_url_template.xml");
	    	
	    	StringBuilder 
	    	
	    	lobby_builder = new StringBuilder(),
	    	sitemap_builder = new StringBuilder();
	    	
	    	PreparedStatement select_categories = sql_connection.prepareStatement("select * from category order by position asc");
			ResultSet category_rs = select_categories.executeQuery();
						
			StringBuilder uncategorized_lobby = new StringBuilder();
			
			boolean 
			
			visible_categories = false,
			kill_section_header_padding = false; 

			while (category_rs.next())
				{
				boolean visible_sub_categories = false;
				
				String category_code = category_rs.getString(1);
				String category_description = category_rs.getString(2);
				
				StringBuilder category_html = new StringBuilder();
				
				String category_description_html = category_template;
				if (kill_section_header_padding) category_description_html = category_description_html.replace("section_header", "section_header no_padding_top");
				category_description_html = category_description_html.replace("<!-- factory:category_description -->", category_description);
				
				PreparedStatement select_sub_categories = sql_connection.prepareStatement("select * from sub_category where category = ? order by created asc");
				select_sub_categories.setString(1, category_code);
				ResultSet sub_category_rs = select_sub_categories.executeQuery();
				
				while (sub_category_rs.next())
					{
					String sub_category_code = sub_category_rs.getString(3);
					String sub_category_description = sub_category_rs.getString(4);
					int active_flag = sub_category_rs.getInt(5);
					String image_name = sub_category_rs.getString(6);
					

					
					if (active_flag == 0) continue;
					
					visible_sub_categories = true;
					
					String href = "/category.html?main=" + category_code.toLowerCase() + "&amp;sub=" + sub_category_code.toLowerCase(),
					
					sub_category_html = sub_category_template;
					sub_category_html = sub_category_html.replace("factory:category_href", href);
					sub_category_html = sub_category_html.replace("factory:image_path", "/img/lobby_tiles/" + image_name);
					sub_category_html = sub_category_html.replace("<!-- factory:sub_category_description -->", sub_category_description);
					
					String sitemap_url = sitemap_url_template;
					
					sitemap_url = sitemap_url.replace("<!-- factory:url -->", Server.host + href);
					sitemap_url = sitemap_url.replace("<!-- factory:changefreq -->", "daily");
					sitemap_url = sitemap_url.replace("<!-- factory:priority -->", "0.9");
					
					sitemap_builder.append(sitemap_url);
					category_html.append(sub_category_html);
					uncategorized_lobby.append(sub_category_html);
					}
				
				if (visible_sub_categories) 
					{
					String final_category_html = sub_category_wrapper_template.replace("<!-- factory:sub_categories -->", category_html.toString());
					lobby_builder.append(category_description_html);
					lobby_builder.append("\n");
					lobby_builder.append(final_category_html);
					lobby_builder.append("\n");
					visible_categories = true;
					kill_section_header_padding = true;
					}
				}
			
			if (!visible_categories)
				{
				lobby_builder.append(category_template.replace("<!-- factory:category_description -->", "Lobby"));
				lobby_builder.append("<table id=\"contest_report_table\" class=\"contest_table\"><tr><td style=\"text-align:left\">Contests will be posted in the next few days. Please check back soon!</td></tr></table>");
				}
			
			// ---------------------------------------------------------------------------------------

			PreparedStatement select_contests = sql_connection.prepareStatement("select id, status from contest");
			ResultSet result_set = select_contests.executeQuery();
			
			while (result_set.next())
				{
				int 
				
				contest_id = result_set.getInt(1),
				contest_status = result_set.getInt(2);
				
				String 
				
				changefreq = contest_status < 3 ? "daily" : "monthly",
				sitemap_url = sitemap_url_template;
				
				sitemap_url = sitemap_url.replace("<!-- factory:url -->", Server.host + "/contest.html?id=" + contest_id);
				sitemap_url = sitemap_url.replace("<!-- factory:changefreq -->", changefreq);
				sitemap_url = sitemap_url.replace("<!-- factory:priority -->", "0.8");
				
				sitemap_builder.append(sitemap_url);
				}
		    ContestMethods.checkCrowdContests();
		    new CloseContestRegistration();
			String 
			
			lobby_html = sub_category_wrapper_template.replace("<!-- factory:sub_categories -->", uncategorized_lobby.toString()),
			sitemap_xml = sitemap_template.replace("<!-- factory:urls -->", sitemap_builder.toString());
						
	        Utils.write_to_string(domain_directory + "/lobby.html", lobby_template.replace("<!-- factory:lobby_html -->", lobby_html));
	        Utils.write_to_string(domain_directory + "/sitemap.xml", sitemap_xml);
			}
		catch (Exception e)
			{
			Server.exception(e);
			}
		}
	}
