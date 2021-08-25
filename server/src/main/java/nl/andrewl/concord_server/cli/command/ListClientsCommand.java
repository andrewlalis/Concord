package nl.andrewl.concord_server.cli.command;

import nl.andrewl.concord_server.ConcordServer;
import nl.andrewl.concord_server.cli.ServerCliCommand;

public class ListClientsCommand implements ServerCliCommand {
	@Override
	public void handle(ConcordServer server, String[] args) throws Exception {
		var users = server.getClients();
		if (users.isEmpty()) {
			System.out.println("There are no connected clients.");
		} else {
			StringBuilder sb = new StringBuilder("Online Users:\n");
			for (var userData : users) {
				sb.append("\t").append(userData.getName()).append(" (").append(userData.getId()).append(")\n");
			}
			System.out.print(sb);
		}
	}
}
