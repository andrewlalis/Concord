package nl.andrewl.concord_server.cli.command;

import nl.andrewl.concord_server.ConcordServer;
import nl.andrewl.concord_server.cli.ServerCliCommand;

/**
 * This command forcibly stops the server, disconnecting any clients.
 */
public class StopCommand implements ServerCliCommand {
	@Override
	public void handle(ConcordServer server, String[] args) throws Exception {
		server.stop();
	}
}
