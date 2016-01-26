package ch.meemin.pmtable.widgetset.client;

import com.vaadin.shared.ui.select.AbstractSelectState;

public class PMTableState extends AbstractSelectState {
	{
		primaryStyleName = "v-table";
	}

	public PMTableCollapseMenuContent collapseMenuContent = PMTableConstants.DEFAULT_COLLAPSE_MENU_CONTENT;
}