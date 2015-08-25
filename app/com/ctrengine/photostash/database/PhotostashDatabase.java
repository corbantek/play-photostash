package com.ctrengine.photostash.database;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import play.Logger;
import play.Logger.ALogger;

import com.arangodb.ArangoConfigure;
import com.arangodb.ArangoDriver;
import com.arangodb.ArangoException;
import com.arangodb.ArangoHost;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionOptions;
import com.arangodb.entity.CollectionType;
import com.arangodb.entity.IndexType;
import com.arangodb.entity.StringsResultEntity;
import com.arangodb.util.MapBuilder;
import com.ctrengine.photostash.conf.DatabaseConfiguration;
import com.ctrengine.photostash.models.AlbumDocument;
import com.ctrengine.photostash.models.Document;
import com.ctrengine.photostash.models.FileDocument;
import com.ctrengine.photostash.models.PhotographCacheDocument;
import com.ctrengine.photostash.models.PhotographDocument;
import com.ctrengine.photostash.models.RelateDocument;
import com.ctrengine.photostash.models.StoryDocument;

public enum PhotostashDatabase {
	INSTANCE;

	private static final ALogger LOGGER = Logger.of("database");

	private ArangoDriver photostashArangoDriver;
	private boolean genesisComplete;

	private PhotostashDatabase() {
		genesisComplete = false;
	}

	private void verifyDatabaseDriver() throws DatabaseException {
		if (photostashArangoDriver == null) {
			/**
			 * Need to create database connection
			 */
			createDatabaseDriver();
			if (photostashArangoDriver == null) {
				/**
				 * Database creation failure
				 */
				throw new DatabaseException("ERROR: Could not connect to database: " + DatabaseConfiguration.INSTANCE.toString());
			} else {
				if (!genesisComplete) {
					/**
					 * Need to perform Genesis
					 */
					genesisDatabase();
					if (!genesisComplete) {
						throw new DatabaseException("ERROR: Could not create default Photostash database architecture.");
					}
				}
			}
		}
	}

	private synchronized void createDatabaseDriver() {
		ArangoConfigure arangoConfigure = new ArangoConfigure();
		arangoConfigure.setArangoHost(new ArangoHost(DatabaseConfiguration.INSTANCE.getHost(), DatabaseConfiguration.INSTANCE.getPort()));
		arangoConfigure.setMaxPerConnection(50);
		arangoConfigure.setMaxTotalConnection(50);
		arangoConfigure.init();

		try {
			ArangoDriver systemArangoDriver = new ArangoDriver(arangoConfigure);
			StringsResultEntity stringsResultEntity = systemArangoDriver.getDatabases();
			if (!stringsResultEntity.getResult().contains(DatabaseConfiguration.INSTANCE.getDatabase())) {
				systemArangoDriver.createDatabase(DatabaseConfiguration.INSTANCE.getDatabase());
			}
		} catch (ArangoException e) {
			LOGGER.error("Cannot connect/create Photostash Database: " + e);
		}

		photostashArangoDriver = new ArangoDriver(arangoConfigure, DatabaseConfiguration.INSTANCE.getDatabase());
	}

	private synchronized void genesisDatabase() {
		try {
			/**
			 * Get List of Collections
			 */
			List<String> collections = new LinkedList<String>();
			for (CollectionEntity collectionEntity : photostashArangoDriver.getCollections(true).getCollections()) {
				collections.add(collectionEntity.getName());
			}
			/**
			 * Create AlbumDocument Collection
			 */
			if (!collections.contains(AlbumDocument.COLLECTION)) {
				photostashArangoDriver.createCollection(AlbumDocument.COLLECTION);
				photostashArangoDriver.createIndex(AlbumDocument.COLLECTION, IndexType.HASH, true, AlbumDocument.PATH);
			}
			/**
			 * Create StoryDocument Collection
			 */
			if (!collections.contains(StoryDocument.COLLECTION)) {
				photostashArangoDriver.createCollection(StoryDocument.COLLECTION);
				photostashArangoDriver.createIndex(StoryDocument.COLLECTION, IndexType.HASH, true, StoryDocument.PATH);
			}
			/**
			 * Create PhotographDocument Collection
			 */
			if (!collections.contains(PhotographDocument.COLLECTION)) {
				photostashArangoDriver.createCollection(PhotographDocument.COLLECTION);
				photostashArangoDriver.createIndex(PhotographDocument.COLLECTION, IndexType.HASH, true, PhotographDocument.PATH);
			}
			/**
			 * Create PhotographDocument Cache Collection
			 */
			if (!collections.contains(PhotographCacheDocument.COLLECTION)) {
				photostashArangoDriver.createCollection(PhotographCacheDocument.COLLECTION);
				photostashArangoDriver.createIndex(PhotographCacheDocument.COLLECTION, IndexType.SKIPLIST, false, PhotographCacheDocument.SIZE);
				photostashArangoDriver.createIndex(PhotographCacheDocument.COLLECTION, IndexType.SKIPLIST, false, PhotographCacheDocument.SQUARE_SIZE);
			}

			CollectionOptions edgeCollectionOptions = new CollectionOptions();
			edgeCollectionOptions.setType(CollectionType.EDGE);
			/**
			 * Create AlbumDocument->StoryDocument Graph/Edge
			 */
			if (!collections.contains(AlbumDocument.RELATE_COLLECTION)) {
				photostashArangoDriver.createCollection(AlbumDocument.RELATE_COLLECTION, edgeCollectionOptions);
			}
			if (!collections.contains(StoryDocument.RELATE_COLLECTION)) {
				photostashArangoDriver.createCollection(StoryDocument.RELATE_COLLECTION, edgeCollectionOptions);
			}
			if (!collections.contains(PhotographDocument.RELATE_COLLECTION)) {
				photostashArangoDriver.createCollection(PhotographDocument.RELATE_COLLECTION, edgeCollectionOptions);
			}
			genesisComplete = true;
		} catch (ArangoException e) {
			LOGGER.error("Photostash Database Genesis Failure: " + e);
		}
	}

	public List<AlbumDocument> getAlbums() throws DatabaseException {
		return getDocuments(AlbumDocument.COLLECTION, AlbumDocument.class);
	}

	public AlbumDocument getAlbum(String albumId) throws DatabaseException {
		return getDocument(AlbumDocument.COLLECTION, albumId, AlbumDocument.class);
	}

	public AlbumDocument findAlbum(String path) throws DatabaseException {
		return findPathDocument(AlbumDocument.COLLECTION, path, AlbumDocument.class);
	}

	public StoryDocument getStory(String storyId) throws DatabaseException {
		return getDocument(StoryDocument.COLLECTION, storyId, StoryDocument.class);
	}

	public List<StoryDocument> getStories() throws DatabaseException {
		return getDocuments(StoryDocument.COLLECTION, StoryDocument.class);
	}

	public StoryDocument findStory(String path) throws DatabaseException {
		return findPathDocument(StoryDocument.COLLECTION, path, StoryDocument.class);
	}

	public PhotographDocument getPhotograph(String photographId) throws DatabaseException {
		return getDocument(PhotographDocument.COLLECTION, photographId, PhotographDocument.class);
	}

	public List<PhotographDocument> getPhotographs() throws DatabaseException {
		return getDocuments(PhotographDocument.COLLECTION, PhotographDocument.class);
	}

	public PhotographDocument findPhotograph(String path) throws DatabaseException {
		return findPathDocument(PhotographDocument.COLLECTION, path, PhotographDocument.class);
	}

	public <D extends Document> D createDocument(D document) throws DatabaseException {
		verifyDatabaseDriver();
		try {
			return photostashArangoDriver.createDocument(document.getCollection(), document).getEntity();
		} catch (ArangoException e) {
			throw new DatabaseException(e);
		}
	}

	public <D extends Document> D updateDocument(D document) throws DatabaseException {
		verifyDatabaseDriver();
		try {
			/**
			 * TODO ArangoDB Driver Bug
			 */
			return (D) photostashArangoDriver.updateDocument(document.getCollection(), document.getKey(), document).getEntity();
		} catch (ArangoException e) {
			throw new DatabaseException(e);
		}
	}

	public <R extends RelateDocument, D extends Document> List<D> getRelatedDocuments(R relateDocument, Class<D> clazz) throws DatabaseException {
		return getRelatedDocuments(relateDocument, clazz, null);
	}
	
	public <R extends RelateDocument, D extends Document> List<D> getRelatedDocuments(R relateDocument, Class<D> clazz, List<DatabaseFilter> andFilterList) throws DatabaseException {
		return getRelatedDocuments(relateDocument, clazz, andFilterList, null);
	}

	public <R extends RelateDocument, D extends Document> List<D> getRelatedDocuments(R relateDocument, Class<D> clazz, List<DatabaseFilter> andFilterList, String sortByKey) throws DatabaseException {
		verifyDatabaseDriver();
		String queryRelate = "FOR a in NEIGHBORS(" + relateDocument.getCollection() + ", " + relateDocument.getRelateCollection() + ", @id, 'outbound') ";
		MapBuilder bindVars = new MapBuilder().put("id", relateDocument.getDocumentAddress());
		if (andFilterList != null && !andFilterList.isEmpty()) {
			queryRelate += "FILTER";
			boolean firstFilter = true;
			for (DatabaseFilter filter : andFilterList) {
				if (firstFilter) {
					queryRelate += " ";
					firstFilter = false;
				} else {
					queryRelate += "&& ";
				}
				queryRelate += "a.vertex." + filter.getKey() + " " + filter.getOperand() + " @" + filter.getKey() + " ";
				bindVars.put(filter.getKey(), filter.getValue());
			}
		}
		if(sortByKey != null){
			queryRelate += "SORT a.vertex."+sortByKey+" ";
		}
		queryRelate += "RETURN a.vertex";
		try {
			List<D> documentList = photostashArangoDriver.executeDocumentQuery(queryRelate, bindVars.get(), photostashArangoDriver.getDefaultAqlQueryOptions(), clazz).asEntityList();
			if(sortByKey == null){
				Collections.sort(documentList);
			}
			return documentList;
		} catch (ArangoException e) {
			throw new DatabaseException(queryRelate + " " + e);
		}
	}

	public <R extends RelateDocument, D extends Document> void relateDocumentToDocument(R relateDocument, D document) throws DatabaseException {
		verifyDatabaseDriver();
		try {
			LOGGER.debug(relateDocument.getRelateCollection() + ": " + relateDocument.getDocumentAddress() + " -> " + document.getDocumentAddress());
			photostashArangoDriver.createEdge(DatabaseConfiguration.INSTANCE.getDatabase(), relateDocument.getRelateCollection(), new Object(), relateDocument.getDocumentAddress(), document.getDocumentAddress(), false, false);
		} catch (ArangoException e) {
			throw new DatabaseException(e);
		}
	}

	private <D extends Document> List<D> getDocuments(String collection, Class<D> clazz) throws DatabaseException {
		verifyDatabaseDriver();
		try {
			List<D> documentList = photostashArangoDriver.executeSimpleAllDocuments(collection, 0, 0, clazz).asEntityList();
			Collections.sort(documentList);
			return documentList;
		} catch (ArangoException e) {
			throw new DatabaseException(e);
		}
	}

	private <D extends FileDocument> D findPathDocument(String collection, String path, Class<D> clazz) throws DatabaseException {
		verifyDatabaseDriver();
		final String QUERY_PATH = "FOR a IN " + collection + " FILTER a." + FileDocument.PATH + " == @path RETURN a";
		try {
			Map<String, Object> bindVars = new MapBuilder().put("path", path).get();
			List<D> documents = photostashArangoDriver.executeDocumentQuery(QUERY_PATH, bindVars, photostashArangoDriver.getDefaultAqlQueryOptions(), clazz).asEntityList();
			if (documents.size() > 0) {
				return documents.get(0);
			} else {
				return null;
			}
		} catch (ArangoException e) {
			throw new DatabaseException(e);
		}
	}

	private <D extends Document> D getDocument(String collection, String id, Class<D> clazz) throws DatabaseException {
		verifyDatabaseDriver();
		try {
			return photostashArangoDriver.getDocument(collection, id, clazz).getEntity();
		} catch (ArangoException e) {
			throw new DatabaseException(e);
		}
	}
}
