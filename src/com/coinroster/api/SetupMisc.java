package com.coinroster.api;

import java.lang.reflect.Constructor;
import java.sql.Connection;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

public class SetupMisc extends Utils{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public SetupMisc(MethodInstance method) throws Exception {
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;
		DB db = new DB(sql_connection);

		method : {	
//------------------------------------------------------------------------------------
			try{
				
				JSONObject data = input.getJSONObject("data");
				data.put("category", "USERGENERATED");
				data.put("sub_category", "USERGENERATED");
				data.put("contest_type", "PARI-MUTUEL");
				data.put("progressive", "");
				data.put("rake", 5);
				
				JSONArray option_table = new JSONArray();
				int index = 1;
				for(int i = 0; i < data.getJSONArray("pari_mutuel_options").length(); i++){
					JSONObject option = new JSONObject();
					if(data.has("risk")) {
						option.put("id", index);
						option.put("description", data.getJSONArray("pari_mutuel_options").getJSONObject(i).getString("description"));
						option.put("odds", data.getJSONArray("pari_mutuel_options").getJSONObject(i).getDouble("odds"));
						option_table.put(option);
					} else {
						option.put("id", index);
						option.put("description", data.getJSONArray("pari_mutuel_options").getString(i));
						option_table.put(option);
					}
					index++;
				}
				data.put("option_table", option_table);
				data.remove("pari_mutuel_options");
				if(data.has("risk")) {
					JSONObject prop_data = new JSONObject();
					prop_data.put("risk", data.getDouble("risk"));
					data.put("prop_data", prop_data);
					log(prop_data.toString());
				}
				
				MethodInstance prop_method = new MethodInstance();
				JSONObject prop_output = new JSONObject("{\"status\":\"0\"}");
				prop_method.input = data;
				prop_method.output = prop_output;
				prop_method.session = method.session;
				prop_method.internal_caller = true;
				prop_method.sql_connection = sql_connection;
				
				try{
					Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
					c.newInstance(prop_method);
					output = prop_method.output;
					output.put("status", "1");

				}
				catch(Exception e){
					output = prop_method.output;
					log(output.toString());
					Server.exception(e);
				}
				
				
			}catch(Exception e){
				Server.exception(e);
				output.put("error", e.toString());
				output.put("status", "0");	
			}
		}
		method.response.send(output);
	}
}