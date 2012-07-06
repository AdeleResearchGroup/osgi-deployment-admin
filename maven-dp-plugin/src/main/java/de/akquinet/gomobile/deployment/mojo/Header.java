package de.akquinet.gomobile.deployment.mojo;

/**
 * Allows to add extra headers to bundles which will be included in a deployment package.
 * 
 * @author Thomas Leveque
 *
 */
public interface Header {

	public String getName();
	
	public String getValue();
	
	public Header clone();
}
