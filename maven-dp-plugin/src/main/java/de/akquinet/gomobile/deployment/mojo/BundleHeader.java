package de.akquinet.gomobile.deployment.mojo;

/**
 * Represents a Bundle header added during packaging of deployment package.
 * 
 * @author Thomas Leveque
 *
 */
public class BundleHeader implements Header {

	private String m_value;
	
	private String m_headerName;
	
	public BundleHeader() {
		// do nothing
	}
	
	public BundleHeader(String headerName, String value) {
		m_headerName = headerName;
		m_value = value;
	}

	public String getName() {
		return m_headerName;
	}
	
	public void setName(String headerName) {
		m_headerName = headerName;
	}

	public String getValue() {
		return m_value;
	}

	public void setValue(String value) {
		m_value = value;
	}
	
	public Header clone() {
		return new BundleHeader(m_headerName, m_value);
	}
}
