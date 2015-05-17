package com.ctrengine.photostash.models;

public class EdgeDocument {
	private String _key;
	private String _from;
	private String _to;
	
	public EdgeDocument(AbstractDocument abstractDocument){
		_from = abstractDocument.getDocumentAddress();
	}
}
