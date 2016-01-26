package ch.meemin.pmtable.widgetset.client;

import ch.meemin.pmtable.widgetset.client.PMTableConstants.Section;

import com.vaadin.shared.MouseEventDetails;
import com.vaadin.shared.communication.ServerRpc;

public interface PMTableServerRpc extends ServerRpc {

	/**
	 * Informs the server that a context click happened inside of Table
	 */
	public void contextClick(String rowKey, String colKey, Section section, MouseEventDetails details);

}
