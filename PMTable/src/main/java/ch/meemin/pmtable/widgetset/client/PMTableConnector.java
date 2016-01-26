package ch.meemin.pmtable.widgetset.client;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ch.meemin.pmtable.PMTable;
import ch.meemin.pmtable.widgetset.client.PMTableConstants.Section;
import ch.meemin.pmtable.widgetset.client.PMTableWidget.FooterCell;
import ch.meemin.pmtable.widgetset.client.PMTableWidget.HeaderCell;
import ch.meemin.pmtable.widgetset.client.PMTableWidget.PMTableWidgetBody.PMTableWidgetRow;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.BrowserInfo;
import com.vaadin.client.ComponentConnector;
import com.vaadin.client.ConnectorHierarchyChangeEvent;
import com.vaadin.client.ConnectorHierarchyChangeEvent.ConnectorHierarchyChangeHandler;
import com.vaadin.client.DirectionalManagedLayout;
import com.vaadin.client.HasChildMeasurementHintConnector;
import com.vaadin.client.HasComponentsConnector;
import com.vaadin.client.Paintable;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.TooltipInfo;
import com.vaadin.client.UIDL;
import com.vaadin.client.Util;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.ui.AbstractFieldConnector;
import com.vaadin.client.ui.PostLayoutListener;
import com.vaadin.shared.MouseEventDetails;
import com.vaadin.shared.ui.Connect;

// Connector binds client-side widget class to server-side component class
// Connector lives in the client and the @Connect annotation specifies the
// corresponding server-side component
@Connect(PMTable.class)
public class PMTableConnector extends AbstractFieldConnector implements HasComponentsConnector,
		ConnectorHierarchyChangeHandler, Paintable, DirectionalManagedLayout, PostLayoutListener,
		HasChildMeasurementHintConnector {

	private List<ComponentConnector> childComponents;

	public PMTableConnector() {
		addConnectorHierarchyChangeHandler(this);
	}

	@Override
	protected void init() {
		super.init();
		getWidget().init(getConnection());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.vaadin.client.ui.AbstractComponentConnector#onUnregister()
	 */
	@Override
	public void onUnregister() {
		super.onUnregister();
		getWidget().onUnregister();
	}

	@Override
	protected void sendContextClickEvent(MouseEventDetails details, EventTarget eventTarget) {

		if (!Element.is(eventTarget)) {
			return;
		}
		Element e = Element.as(eventTarget);

		Section section;
		String colKey = null;
		String rowKey = null;
		if (getWidget().tFoot.getElement().isOrHasChild(e)) {
			section = Section.FOOTER;
			FooterCell w = WidgetUtil.findWidget(e, FooterCell.class);
			colKey = w.getColKey();
		} else if (getWidget().tHead.getElement().isOrHasChild(e)) {
			section = Section.HEADER;
			HeaderCell w = WidgetUtil.findWidget(e, HeaderCell.class);
			colKey = w.getColKey();
		} else {
			section = Section.BODY;
			if (getWidget().scrollBody.getElement().isOrHasChild(e)) {
				PMTableWidgetRow w = getScrollTableRow(e);
				/*
				 * if w is null because we've clicked on an empty area, we will let rowKey and colKey be null too, which will
				 * then lead to the server side returning a null object.
				 */
				if (w != null) {
					rowKey = w.getKey();
					colKey = getWidget().tHead.getHeaderCell(getElementIndex(e, w.getElement())).getColKey();
				}
			}
		}

		getRpcProxy(PMTableServerRpc.class).contextClick(rowKey, colKey, section, details);

		WidgetUtil.clearTextSelection();
	}

	protected PMTableWidgetRow getScrollTableRow(Element e) {
		return WidgetUtil.findWidget(e, PMTableWidgetRow.class);
	}

	private int getElementIndex(Element e, com.google.gwt.user.client.Element element) {
		int i = 0;
		Element current = element.getFirstChildElement();
		while (!current.isOrHasChild(e)) {
			current = current.getNextSiblingElement();
			++i;
		}
		return i;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.vaadin.client.Paintable#updateFromUIDL(com.vaadin.client.UIDL, com.vaadin.client.ApplicationConnection)
	 */
	@Override
	public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
		getWidget().rendering = true;

		// If a row has an open context menu, it will be closed as the row is
		// detached. Retain a reference here so we can restore the menu if
		// required.
		PMTableWidget.ContextMenuDetails contextMenuBeforeUpdate = getWidget().contextMenu;

		if (uidl.hasAttribute(PMTableConstants.ATTRIBUTE_PAGEBUFFER_FIRST)) {
			getWidget().serverCacheFirst = uidl.getIntAttribute(PMTableConstants.ATTRIBUTE_PAGEBUFFER_FIRST);
			getWidget().serverCacheLast = uidl.getIntAttribute(PMTableConstants.ATTRIBUTE_PAGEBUFFER_LAST);
		} else {
			getWidget().serverCacheFirst = -1;
			getWidget().serverCacheLast = -1;
		}
		/*
		 * We need to do this before updateComponent since updateComponent calls this.setHeight() which will calculate a new
		 * body height depending on the space available.
		 */
		if (uidl.hasAttribute("colfooters")) {
			getWidget().showColFooters = uidl.getBooleanAttribute("colfooters");
		}

		getWidget().tFoot.setVisible(getWidget().showColFooters);

		if (!isRealUpdate(uidl)) {
			getWidget().rendering = false;
			return;
		}

		getWidget().enabled = isEnabled();

		if (BrowserInfo.get().isIE8() && !getWidget().enabled) {
			/*
			 * The disabled shim will not cover the table body if it is relative in IE8. See #7324
			 */
			getWidget().scrollBodyPanel.getElement().getStyle().setPosition(Position.STATIC);
		} else if (BrowserInfo.get().isIE8()) {
			getWidget().scrollBodyPanel.getElement().getStyle().setPosition(Position.RELATIVE);
		}

		getWidget().paintableId = uidl.getStringAttribute("id");
		getWidget().immediate = getState().immediate;

		getWidget().updateDragMode(uidl);

		// Update child measure hint
		int childMeasureHint = uidl.hasAttribute("measurehint") ? uidl.getIntAttribute("measurehint") : 0;
		getWidget().setChildMeasurementHint(ChildMeasurementHint.values()[childMeasureHint]);

		getWidget().updateSelectionProperties(uidl, getState(), isReadOnly());

		if (uidl.hasAttribute("alb")) {
			getWidget().bodyActionKeys = uidl.getStringArrayAttribute("alb");
		} else {
			// Need to clear the actions if the action handlers have been
			// removed
			getWidget().bodyActionKeys = null;
		}

		getWidget().recalcWidths = uidl.hasAttribute("recalcWidths");
		if (getWidget().recalcWidths) {
			if (getWidget().scrollBody != null)
				getWidget().scrollBody.resetCellWidths();
			getWidget().tHead.clear();
			getWidget().tFoot.clear();
		}

		getWidget().updateScrollTop(uidl);

		getWidget().showRowHeaders = uidl.getBooleanAttribute("rowheaders");
		getWidget().showColHeaders = uidl.getBooleanAttribute("colheaders");

		getWidget().updateSortingProperties(uidl);

		boolean keyboardSelectionOverRowFetchInProgress = getWidget().selectSelectedRows(uidl);

		getWidget().updateActionMap(uidl);

		getWidget().updateColumnProperties(uidl);

		UIDL ac = uidl.getChildByTagName("-ac");
		if (ac == null) {
			if (getWidget().dropHandler != null) {
				// remove dropHandler if not present anymore
				getWidget().dropHandler = null;
			}
		} else {
			if (getWidget().dropHandler == null) {
				getWidget().dropHandler = getWidget().new PMTableWidgetDropHandler();
			}
			getWidget().dropHandler.updateAcceptRules(ac);
		}

		UIDL partialRowAdditions = uidl.getChildByTagName("prows");
		UIDL partialRowUpdates = uidl.getChildByTagName("urows");
		if (partialRowUpdates != null || partialRowAdditions != null) {
			getWidget().postponeSanityCheckForLastRendered = true;
			final int scrollP = getWidget().scrollBodyPanel.getScrollPosition();
			getWidget().addAndRemoveRows(partialRowAdditions);
			getWidget().updateRowsInBody(partialRowUpdates);
			Scheduler.get().scheduleDeferred(new ScheduledCommand() {

				@Override
				public void execute() {
					// Window.alert("Scroll To: " + scrollP);
					getWidget().scrollBodyPanel.setScrollPosition(scrollP);
				}
			});

			if (uidl.hasAttribute("reorder")) {
				String[] order = uidl.getStringArrayAttribute("reorder");
				if (order != null)
					getWidget().reorderRowsInBody(order);
			}
			if (getWidget().recalcWidths)
				getWidget().scrollBody.resetCellWidths();

			// sanity check (in case the value has slipped beyond the total
			// amount of rows)
			getWidget().updateMaxIndent();
		} else {
			getWidget().postponeSanityCheckForLastRendered = false;
			UIDL rowData = uidl.getChildByTagName("rows");
			if (rowData != null) {
				if (!getWidget().recalcWidths && getWidget().initializedAndAttached) {
					getWidget().updateBody(rowData, uidl.getIntAttribute("rows"));
					if (getWidget().headerChangedDuringUpdate) {
						getWidget().triggerLazyColumnAdjustment(true);
					}
				} else {
					getWidget().initializeRows(uidl, rowData);
				}
			}
		}

		// If a row had an open context menu before the update, and after the
		// update there's a row with the same key as that row, restore the
		// context menu. See #8526.
		showSavedContextMenu(contextMenuBeforeUpdate);

		if (!getWidget().isSelectable()) {
			getWidget().scrollBody.addStyleName(getWidget().getStylePrimaryName() + "-body-noselection");
		} else {
			getWidget().scrollBody.removeStyleName(getWidget().getStylePrimaryName() + "-body-noselection");
		}

		getWidget().hideScrollPositionAnnotation();

		// selection is no in sync with server, avoid excessive server visits by
		// clearing to flag used during the normal operation
		if (!keyboardSelectionOverRowFetchInProgress) {
			getWidget().selectionChanged = false;
		}

		getWidget().multiselectPending = false;

		if (getWidget().focusedRow != null) {
			if (!getWidget().focusedRow.isAttached()) {
				// focused row has been orphaned, can't focus
				if (getWidget().selectedRowKeys.contains(getWidget().focusedRow.getKey())) {
					// if row cache was refreshed, focused row should be
					// in selection and exists with same index
					getWidget().setRowFocus(getWidget().getRenderedRowByKey(getWidget().focusedRow.getKey()));
				} else if (getWidget().selectedRowKeys.size() > 0) {
					// try to focus any row in selection
					getWidget().setRowFocus(getWidget().getRenderedRowByKey(getWidget().selectedRowKeys.iterator().next()));
				} else {
					// try to focus any row
					getWidget().focusRowFromBody();
				}
			}
		}

		/*
		 * If the server has (re)initialized the rows, our selectionRangeStart row will point to an index that the server
		 * knows nothing about, causing problems if doing multi selection with shift. The field will be cleared a little
		 * later when the row focus has been restored. (#8584)
		 */
		if (uidl.hasAttribute(PMTableConstants.ATTRIBUTE_KEY_MAPPER_RESET)
				&& uidl.getBooleanAttribute(PMTableConstants.ATTRIBUTE_KEY_MAPPER_RESET)
				&& getWidget().selectionRangeStart != null) {
			assert !getWidget().selectionRangeStart.isAttached();
			getWidget().selectionRangeStart = getWidget().focusedRow;
		}

		getWidget().tabIndex = getState().tabIndex;
		getWidget().setProperTabIndex();

		Scheduler.get().scheduleFinally(new ScheduledCommand() {

			@Override
			public void execute() {
				getWidget().resizeSortedColumnForSortIndicator();
			}
		});

		// Remember this to detect situations where overflow hack might be
		// needed during scrolling
		getWidget().lastRenderedHeight = getWidget().scrollBody.getOffsetHeight();

		getWidget().rendering = false;
		getWidget().headerChangedDuringUpdate = false;

		getWidget().collapsibleMenuContent = getState().collapseMenuContent;

		// getWidget().triggerLazyColumnAdjustment(true);
	}

	@Override
	public PMTableWidget getWidget() {
		return (PMTableWidget) super.getWidget();
	}

	@Override
	public void updateCaption(ComponentConnector component) {
		// NOP, not rendered
	}

	@Override
	public void layoutVertically() {
		getWidget().updateHeight();
	}

	@Override
	public void layoutHorizontally() {
		getWidget().updateWidth();
	}

	@Override
	public void postLayout() {
		PMTableWidget table = getWidget();
		if (table.sizeNeedsInit) {
			table.sizeInit();
			Scheduler.get().scheduleFinally(new ScheduledCommand() {
				@Override
				public void execute() {
					getLayoutManager().setNeedsMeasure(PMTableConnector.this);
					ServerConnector parent = getParent();
					if (parent instanceof ComponentConnector) {
						getLayoutManager().setNeedsMeasure((ComponentConnector) parent);
					}
					getLayoutManager().setNeedsVerticalLayout(PMTableConnector.this);
					getLayoutManager().layoutNow();
				}
			});
		}
	}

	@Override
	public boolean isReadOnly() {
		return super.isReadOnly() || getState().propertyReadOnly;
	}

	@Override
	public PMTableState getState() {
		return (PMTableState) super.getState();
	}

	/**
	 * Shows a saved row context menu if the row for the context menu is still visible. Does nothing if a context menu has
	 * not been saved.
	 * 
	 * @param savedContextMenu
	 */
	public void showSavedContextMenu(PMTableWidget.ContextMenuDetails savedContextMenu) {
		if (isEnabled() && savedContextMenu != null) {
			Iterator<Widget> iterator = getWidget().scrollBody.iterator();
			while (iterator.hasNext()) {
				Widget w = iterator.next();
				PMTableWidgetRow row = (PMTableWidgetRow) w;
				if (row.getKey().equals(savedContextMenu.rowKey)) {
					row.showContextMenu(savedContextMenu.left, savedContextMenu.top);
				}
			}
		}
	}

	@Override
	public TooltipInfo getTooltipInfo(Element element) {

		TooltipInfo info = null;

		if (element != getWidget().getElement()) {
			Object node = Util.findWidget((com.google.gwt.user.client.Element) element, PMTableWidgetRow.class);

			if (node != null) {
				PMTableWidgetRow row = (PMTableWidgetRow) node;
				info = row.getTooltip(element);
			}
		}

		if (info == null) {
			info = super.getTooltipInfo(element);
		}

		return info;
	}

	@Override
	public boolean hasTooltip() {
		/*
		 * Tooltips for individual rows and cells are not processed until updateFromUIDL, so we can't be sure that there are
		 * no tooltips during onStateChange when this method is used.
		 */
		return true;
	}

	@Override
	public void onConnectorHierarchyChange(ConnectorHierarchyChangeEvent connectorHierarchyChangeEvent) {
		// TODO Move code from updateFromUIDL to this method
	}

	@Override
	protected void updateComponentSize(String newWidth, String newHeight) {
		super.updateComponentSize(newWidth, newHeight);

		if ("".equals(newWidth)) {
			getWidget().updateWidth();
		}
		if ("".equals(newHeight)) {
			getWidget().updateHeight();
		}
	}

	@Override
	public List<ComponentConnector> getChildComponents() {
		if (childComponents == null) {
			return Collections.emptyList();
		}

		return childComponents;
	}

	@Override
	public void setChildComponents(List<ComponentConnector> childComponents) {
		this.childComponents = childComponents;
	}

	@Override
	public HandlerRegistration addConnectorHierarchyChangeHandler(ConnectorHierarchyChangeHandler handler) {
		return ensureHandlerManager().addHandler(ConnectorHierarchyChangeEvent.TYPE, handler);
	}

	@Override
	public void setChildMeasurementHint(ChildMeasurementHint hint) {
		getWidget().setChildMeasurementHint(hint);
	}

	@Override
	public ChildMeasurementHint getChildMeasurementHint() {
		return getWidget().getChildMeasurementHint();
	}
}
