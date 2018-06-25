package com.coinroster.internal;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.coinroster.Server;
import com.coinroster.Utils;

public class Backup extends Utils
	{
	public Backup()
		{
		Connection sql_connection = null;
		
		try {
			sql_connection = Server.sql_connection();
			
			String 
			
			backup_directory_root = "/CR-backup",
			date_string = new SimpleDateFormat("yyyy-MMM-dd--HH-mm-ss").format(new Date());
			
			File directory_root = new File(backup_directory_root);
			if (!directory_root.exists()) 
				{
				log("Creating backup root directory: " + backup_directory_root);
				directory_root.mkdir();
				}
			
			String 
    		
			database_name = "coinroster",
    		output_file = backup_directory_root + "/" + date_string + ".sql",
    		dump_string = Server.sql_dump_string() + database_name + " -r " + output_file;

	    	Process process = Runtime.getRuntime().exec(dump_string);

	    	int exit_val = process.waitFor();
	    	
            if (exit_val == 0) log("Backup success: " + output_file);
            else log("Backup ERROR:");
			}
		catch (Exception e)
			{
			Server.exception(e);
			}
		finally
			{
			if (sql_connection != null)
				{
				try {sql_connection.close();} 
				catch (SQLException ignore) {}
				}
			}
		}
	}
