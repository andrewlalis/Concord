module concord_core {
	requires static lombok;

	exports nl.andrewl.concord_core.util to concord_server, concord_client;
	exports nl.andrewl.concord_core.msg to concord_server, concord_client;

	exports nl.andrewl.concord_core.msg.types to concord_server, concord_client;
	exports nl.andrewl.concord_core.msg.types.client_setup to concord_client, concord_server;
	exports nl.andrewl.concord_core.msg.types.chat to concord_client, concord_server;
	exports nl.andrewl.concord_core.msg.types.channel to concord_client, concord_server;
}