package nl.andrewl.concord_server.util;

import org.dizitart.no2.IndexOptions;
import org.dizitart.no2.IndexType;
import org.dizitart.no2.NitriteCollection;

import java.util.Map;

public class CollectionUtils {
	/**
	 * Ensures that the given nitrite collection has exactly the given set of
	 * indexes. It will remove any non-conforming indexes, and create new ones
	 * as necessary.
	 * @param collection The collection to operate on.
	 * @param indexMap A mapping containing keys referring to fields, and values
	 *                 that represent the type of index that should be on that
	 *                 field.
	 */
	public static void ensureIndexes(NitriteCollection collection, Map<String, IndexType> indexMap) {
		for (var index : collection.listIndices()) {
			var entry = indexMap.get(index.getField());
			if (entry == null || !index.getIndexType().equals(entry)) {
				collection.dropIndex(index.getField());
			}
		}
		for (var entry : indexMap.entrySet()) {
			if (!collection.hasIndex(entry.getKey())) {
				collection.createIndex(entry.getKey(), IndexOptions.indexOptions(entry.getValue()));
			}
		}
	}
}
