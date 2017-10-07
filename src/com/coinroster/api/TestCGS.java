package com.coinroster.api;

import java.sql.Connection;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.CallCGS;

public class TestCGS extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public TestCGS(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			JSONObject rpc_call = new JSONObject();
			
			rpc_call.put("jsonrpc", "2.0");
			rpc_call.put("id", 1);
			rpc_call.put("method", "newAccount");
			
			JSONObject rpc_method_params = new JSONObject();
			
			rpc_method_params.put("type", "btc");
			rpc_call.put("params", rpc_method_params);
			
			CallCGS call = new CallCGS(rpc_call);
			
			JSONObject response_obj = call.get_response();
			
			log("Response:");
			log(response_obj);
			
			output.put("message", "Hey!");
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}