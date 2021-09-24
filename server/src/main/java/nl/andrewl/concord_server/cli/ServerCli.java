package nl.andrewl.concord_server.cli;

import nl.andrewl.concord_server.ConcordServer;
import nl.andrewl.concord_server.cli.command.ChannelCommand;
import nl.andrewl.concord_server.cli.command.ListClientsCommand;
import nl.andrewl.concord_server.cli.command.StopCommand;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple command-line interface that's available when running the server. This
 * accepts commands and various space-separated arguments.
 */
public class ServerCli implements Runnable {
	private final ConcordServer server;
	private final Map<String, ServerCliCommand> commands;

	public ServerCli(ConcordServer server) {
		this.server = server;
		this.commands = new HashMap<>();

		this.commands.put("list-clients", new ListClientsCommand());
		this.commands.put("channel", new ChannelCommand());
		this.commands.put("stop", new StopCommand());

		this.commands.put("help", (s, args) -> {
			System.out.println("The following commands are available:");
			for (var key : commands.keySet().stream().sorted().toList()) {
				System.out.println("\t" + key);
			}
		});
	}

	@Override
	public void run() {
		System.out.println("Server command-line-interface initialized.\n\tType \"help\" for a list of available commands.\n\tType \"stop\" to stop the server.");
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
