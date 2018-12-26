package com.coinroster;

import java.sql.Connection;
import org.json.JSONObject;

/**
 * Represents a method that is passed through API middleware.
 * 
 * @see com.coinroster.HttpRequest
 * @see com.coinroster.HttpResponse
 * @see com.coinroster.Session 
 */
public class MethodInstance 
	{
	public HttpRequest request;
	public HttpResponse response;
	
	// input object that has request attributes and output with response attributes
	public JSONObject input, output;
	public Session session;
	public Connection sql_connection;
	public boolean internal_caller = false;
	}
