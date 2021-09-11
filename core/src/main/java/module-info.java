module concord_core {
	requires static lombok;

	exports nl.andrewl.concord_core.util to concord_server, concord_client;
	exports nl.andrewl.concord_core.msg to concord_server, concord_client;
	exports nl.andrewl.concord_core.msg.types to concord_server, concord_client;
}