package com.coinroster;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class CommandHandler extends Utils
	{
	public CommandHandler(String command, Socket socket) throws Exception
		{
		InputStream in = socket.getInputStream();
		OutputStream out = socket.getOutputStream();
		
		PrintWriter console = new PrintWriter(out, true);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));

//------------------------------------------------------------------------------------
		
		int tail_log_lines = 40;
		
		if (command.equals(""))
			{
			console.printf("%-20.20s  %-30.70s%n", "version", "Prints out version info");
			console.printf("%-20.20s  %-30.70s%n", "restart", "Halts and re-starts the server; calls shutdown_hook");
			console.printf("%-20.20s  %-30.70s%n", "halt", "Halts the server, calls shutdown_hook");
			console.printf("%-20.20s  %-30.70s%n", "test mail", "Sends test email to developer email address");
			console.printf("%-20.20s  %-30.70s%n", "tail log {n}", "Prints out last {n} lines of log file; default is " + tail_log_lines + " lines");
			console.printf("%-20.20s  %-30.70s%n", "clear log", "Clears the log file");
			console.println("");
			console.println("enter command:");
			
			command = reader.readLine();
			}

		log("Command: " + command);

		console.println("");

//------------------------------------------------------------------------------------
			
		// special handling for "tail log"
		
		if (command.contains("tail log "))
			{
			try {
				tail_log_lines = Integer.parseInt(command.split(" ")[2]);
				}
			catch (Exception ignore) {}
			command = "tail log";
			}

//------------------------------------------------------------------------------------
			
		switch (command)
			{
			case "version":
				
				console.printf("%-20.20s  %-30.70s%n", "version:", Server.version);
				console.printf("%-20.20s  %-30.70s%n", "started:", Server.start_time);
				if (Server.log_file_active) console.printf("%-20.20s  %-30.70s%n", "log file:", Server.java_out_path);
				break;
				
			case "restart":
				
				console.println("restarting...");
				Server.pre_exit();
				console.println("");
				Server.exit(true);
				break;
				
			case "halt":
				
				console.println("halting...");
				Server.pre_exit();
				console.println("");
				Server.exit(false);
				break;
				
			case "test mail":
				console.println("testing email...");
				Server.send_mail(Server.developer_email_address, Server.developer_name, "Mail is working", "");
				break;
				
			case "tail log":
				
				if (Server.log_file_active) 
					{
					List<String> log_file = Server.read(Server.java_out_path);

					int
					
					end_line = log_file.size(),
					start_line = end_line - tail_log_lines;
					
					if (start_line < 0) start_line = 0;
					
					for (int line=start_line; line<end_line; line++) console.println(log_file.get(line));
					}
				else console.println("log file is not active");
				break;

			case "clear log":
				
				if (Server.log_file_active)
					{
					try {
						PrintWriter writer = new PrintWriter(Server.java_out_path);
						writer.print("");
						writer.close();
						}
					catch (Exception ignore) {};
					console.println("log has been cleared");
					log("Log has been cleared");
					}
				else console.println("log file is not active");
				break;
				
			default:
				
				out.close();
				return;					
			}

		console.println("");

		out.close();
		}
	}
