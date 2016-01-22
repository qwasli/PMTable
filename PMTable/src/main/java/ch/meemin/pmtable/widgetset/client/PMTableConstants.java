package ch.meemin.pmtable.widgetset.client;

import java.io.Serializable;

public class PMTableConstants implements Serializable {
	public static final String ITEM_CLICK_EVENT_ID = "itemClick";
	public static final String HEADER_CLICK_EVENT_ID = "handleHeaderClick";
	public static final String FOOTER_CLICK_EVENT_ID = "handleFooterClick";
	public static final String COLUMN_RESIZE_EVENT_ID = "columnResize";
	public static final String COLUMN_REORDER_EVENT_ID = "columnReorder";
	public static final String COLUMN_COLLAPSING_EVENT_ID = "columnCollapsing";

	@Deprecated
	public static final String ATTRIBUTE_PAGEBUFFER_FIRST = "pb-ft";
	@Deprecated
	public static final String ATTRIBUTE_PAGEBUFFER_LAST = "pb-l";
	/**
	 * Tell the client that old keys are no longer valid because the server has cleared its key map.
	 */
	@Deprecated
	public static final String ATTRIBUTE_KEY_MAPPER_RESET = "clearKeyMap";

}
