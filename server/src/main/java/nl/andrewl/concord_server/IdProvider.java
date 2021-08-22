package nl.andrewl.concord_server;

import java.util.UUID;

public interface IdProvider {
	UUID newId();
}
