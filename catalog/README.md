# Concord Catalog

The catalog is an HTTP server that is used as a "discovery" server that connects clients to the concord servers they might want to join. Clients will request a list of servers from the catalog, and servers are responsible for regularly sending their metadata to any catalogs they wish to be publicly visible in.
