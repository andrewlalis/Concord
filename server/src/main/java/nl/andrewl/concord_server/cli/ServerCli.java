package nl.andrewl.concord_server.cli;

import nl.andrewl.concord_server.ConcordServer;
import nl.andrewl.concord_server.cli.command.AddChannelCommand;
import nl.andrewl.concord_server.cli.command.ListClientsCommand;
import nl.andrewl.concord_server.cli.command.RemoveChannelCommand;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ServerCli implements Runnable {
	private final ConcordServer server;
	private final Map<String, ServerCliCommand> commands;

	public ServerCli(ConcordServer server) {
		this.server = server;
		this.commands = new HashMap<>();

		this.commands.put("list-clients", new ListClientsCommand());
		this.commands.put("add-channel", new AddChannelCommand());
		this.commands.put("remove-channel", new RemoveChannelCommand());
	}

	@Override
	public void run() {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String line;
		try {
			while (this.server.isRunning() && (line = reader.readLine()) != null) {
				if (!line.isBlank()) {
					String[] words = line.split("\\s+");
					String command = words[0];
					String[] args = Arrays.copyOfRange(words, 1, words.length);
					var cliCommand = this.commands.get(command.trim().toLowerCase());
					if (cliCommand != null) {
						try {
							cliCommand.handle(this.server, args);
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						System.err.println("Unknown command.");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}