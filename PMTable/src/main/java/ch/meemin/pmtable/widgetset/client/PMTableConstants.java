package ch.meemin.pmtable.widgetset.client;

import java.io.Serializable;

import com.vaadin.shared.ui.table.TableState;

public class PMTableConstants implements Serializable {
	/**
	 * Enum describing different sections of Table.
	 * 
	 * @since 7.6
	 */
	public enum Section {
		HEADER,
		BODY,
		FOOTER
	}

	public static final String ITEM_CLICK_EVENT_ID = "itemClick";
	public static final String HEADER_CLICK_EVENT_ID = "handleHeaderClick";
	public static final String FOOTER_CLICK_EVENT_ID = "handleFooterClick";
	public static final String COLUMN_RESIZE_EVENT_ID = "columnResize";
	public static final String COLUMN_REORDER_EVENT_ID = "columnReorder";
	public static final String COLUMN_COLLAPSE_EVENT_ID = "columnCollapse";

	@Deprecated
	public static final String ATTRIBUTE_PAGEBUFFER_FIRST = "pb-ft";
	@Deprecated
	public static final String ATTRIBUTE_PAGEBUFFER_LAST = "pb-l";
	/**
	 * Tell the client that old keys are no longer valid because the server has cleared its key map.
	 */
	@Deprecated
	public static final String ATTRIBUTE_KEY_MAPPER_RESET = "clearKeyMap";
	/**
	 * Default value for {@link TableState#collapseMenuContent}.
	 * 
	 * @since 7.6
	 */
	public static final PMTableCollapseMenuContent DEFAULT_COLLAPSE_MENU_CONTENT = PMTableCollapseMenuContent.ALL_COLUMNS;

}
