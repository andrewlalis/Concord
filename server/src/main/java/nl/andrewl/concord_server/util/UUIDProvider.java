package nl.andrewl.concord_server.util;

import java.util.UUID;

public class UUIDProvider implements IdProvider {
	@Override
	public UUID newId() {
		return UUID.randomUUID();
	}
}
