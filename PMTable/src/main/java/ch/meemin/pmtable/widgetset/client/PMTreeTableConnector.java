package ch.meemin.pmtable.widgetset.client;

import ch.meemin.pmtable.PMTreeTable;
import ch.meemin.pmtable.widgetset.client.PMTableWidget.PMTableWidgetBody.PMTableWidgetRow;
import ch.meemin.pmtable.widgetset.client.PMTreeTableWidget.PMTreeTableWidgetScrollBody.PMTreeTableWidgetRow;
import ch.meemin.pmtable.widgetset.client.PMTreeTableWidget.PendingNavigationEvent;

import com.google.gwt.dom.client.Element;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.TooltipInfo;
import com.vaadin.client.UIDL;
import com.vaadin.client.Util;
import com.vaadin.client.ui.FocusableScrollPanel;
import com.vaadin.shared.ui.Connect;

@Connect(PMTreeTable.class)
public class PMTreeTableConnector extends PMTableConnector {

	@Override
	public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
		FocusableScrollPanel widget = null;
		int scrollPosition = 0;
		if (getWidget().collapseRequest) {
			widget = (FocusableScrollPanel) getWidget().getWidget(1);
			scrollPosition = widget.getScrollPosition();
		}
		getWidget().colIndexOfHierarchy = uidl.hasAttribute(PMTreeTableConstants.ATTRIBUTE_HIERARCHY_COLUMN_INDEX) ? uidl
				.getIntAttribute(PMTreeTableConstants.ATTRIBUTE_HIERARCHY_COLUMN_INDEX) : 0;
		int oldTotalRows = (getWidget().scrollBody != null) ? getWidget().scrollBody.size() : 0;
		super.updateFromUIDL(uidl, client);
		// super.updateFromUIDL set rendering to false, even though we continue
		// rendering here. Set it back to true.
		getWidget().rendering = true;

		if (getWidget().collapseRequest) {
			if (getWidget().collapsedRowKey != null && getWidget().scrollBody != null) {
				PMTableWidgetRow row = getWidget().getRenderedRowByKey(getWidget().collapsedRowKey);
				if (row != null) {
					getWidget().setRowFocus(row);
					getWidget().focus();
				}
			}

			int scrollPosition2 = widget.getScrollPosition();
			if (scrollPosition != scrollPosition2) {
				widget.setScrollPosition(scrollPosition);
			}

			// check which rows are needed from the server and initiate a
			// deferred fetch
			getWidget().onScroll(null);
		}
		// Recalculate table size if collapse request, or if page length is zero
		// (not sent by server) and row count changes (#7908).
		if (getWidget().collapseRequest && getWidget().scrollBody.size() != oldTotalRows) {
			/*
			 * Ensure that possibly removed/added scrollbars are considered. Triggers row calculations, removes cached rows
			 * etc. Basically cleans up state. Be careful if touching this, you will break pageLength=0 if you remove this.
			 */
			getWidget().triggerLazyColumnAdjustment(true);

			getWidget().collapseRequest = false;
		}
		if (uidl.hasAttribute("focusedRow")) {
			String key = uidl.getStringAttribute("focusedRow");
			getWidget().setRowFocus(getWidget().getRenderedRowByKey(key));
			getWidget().focusParentResponsePending = false;
		} else if (uidl.hasAttribute("clearFocusPending")) {
			// Special case to detect a response to a focusParent request that
			// does not return any focusedRow because the selected node has no
			// parent
			getWidget().focusParentResponsePending = false;
		}

		while (!getWidget().collapseRequest && !getWidget().focusParentResponsePending
				&& !getWidget().pendingNavigationEvents.isEmpty()) {
			// Keep replaying any queued events as long as we don't have any
			// potential content changes pending
			PendingNavigationEvent event = getWidget().pendingNavigationEvents.removeFirst();
			getWidget().handleNavigation(event.keycode, event.ctrl, event.shift);
		}
		getWidget().rendering = false;
	}

	@Override
	public PMTreeTableWidget getWidget() {
		return (PMTreeTableWidget) super.getWidget();
	}

	@Override
	public PMTreeTableState getState() {
		return (PMTreeTableState) super.getState();
	}

	@Override
	public TooltipInfo getTooltipInfo(Element element) {

		TooltipInfo info = null;

		if (element != getWidget().getElement()) {
			Object node = Util.findWidget((com.google.gwt.user.client.Element) element, PMTreeTableWidgetRow.class);

			if (node != null) {
				PMTreeTableWidgetRow row = (PMTreeTableWidgetRow) node;
				info = row.getTooltip(element);
			}
		}

		if (info == null) {
			info = super.getTooltipInfo(element);
		}

		return info;
	}
}