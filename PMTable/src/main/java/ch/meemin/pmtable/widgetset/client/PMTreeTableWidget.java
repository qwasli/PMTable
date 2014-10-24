package ch.meemin.pmtable.widgetset.client;

import java.util.Iterator;
import java.util.LinkedList;

import ch.meemin.pmtable.widgetset.client.PMTreeTableWidget.PMTreeTableWidgetScrollBody.PMTreeTableWidgetRow;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.UIDL;
import com.vaadin.client.Util;
import com.vaadin.client.ui.Icon;

public class PMTreeTableWidget extends PMTableWidget {

	/** For internal use only. May be removed or replaced in the future. */
	public static class PendingNavigationEvent {
		public final int keycode;
		public final boolean ctrl;
		public final boolean shift;

		public PendingNavigationEvent(int keycode, boolean ctrl, boolean shift) {
			this.keycode = keycode;
			this.ctrl = ctrl;
			this.shift = shift;
		}

		@Override
		public String toString() {
			String string = "Keyboard event: " + keycode;
			if (ctrl) {
				string += " + ctrl";
			}
			if (shift) {
				string += " + shift";
			}
			return string;
		}
	}

	/** For internal use only. May be removed or replaced in the future. */
	public boolean collapseRequest;

	private boolean selectionPending;

	/** For internal use only. May be removed or replaced in the future. */
	public int colIndexOfHierarchy;

	/** For internal use only. May be removed or replaced in the future. */
	public String collapsedRowKey;

	/** For internal use only. May be removed or replaced in the future. */
	public PMTreeTableWidgetScrollBody scrollBody;

	/** For internal use only. May be removed or replaced in the future. */
	public LinkedList<PendingNavigationEvent> pendingNavigationEvents = new LinkedList<PendingNavigationEvent>();

	/** For internal use only. May be removed or replaced in the future. */
	public boolean focusParentResponsePending;

	@Override
	protected PMTreeTableWidgetScrollBody createScrollBody() {
		scrollBody = new PMTreeTableWidgetScrollBody();
		return scrollBody;
	}

	@Override
	protected int getHierarchyColumnIndex() {
		return colIndexOfHierarchy + (showRowHeaders ? 1 : 0);
	}

	public class PMTreeTableWidgetScrollBody extends PMTableWidget.PMTableWidgetBody {
		private int indentWidth = -1;
		private int maxIndent = 0;

		PMTreeTableWidgetScrollBody() {
			super();
		}

		@Override
		protected PMTreeTableWidgetRow createRow(UIDL uidl, char[] aligns2) {
			if (uidl.hasAttribute("gen_html")) {
				// This is a generated row.
				return new PMTreeTableWidgetGeneratedRow(uidl, aligns2);
			}
			return new PMTreeTableWidgetRow(uidl, aligns2);
		}

		public class PMTreeTableWidgetRow extends PMTableWidget.PMTableWidgetBody.PMTableWidgetRow {

			private boolean isTreeCellAdded = false;
			private SpanElement treeSpacer;
			private boolean open;
			private int depth;
			private boolean canHaveChildren;
			protected Widget widgetInHierarchyColumn;

			public PMTreeTableWidgetRow(UIDL uidl, char[] aligns2) {
				super(uidl, aligns2);
			}

			@Override
			public void addCell(UIDL rowUidl, String text, char align, String style, boolean textIsHTML, boolean isSorted,
					String description) {
				super.addCell(rowUidl, text, align, style, textIsHTML, isSorted, description);

				addTreeSpacer(rowUidl);
			}

			protected boolean addTreeSpacer(UIDL rowUidl) {
				if (cellShowsTreeHierarchy(getElement().getChildCount() - 1)) {
					Element container = (Element) getElement().getLastChild().getFirstChild();

					if (rowUidl.hasAttribute("icon")) {
						Icon icon = client.getIcon(rowUidl.getStringAttribute("icon"));
						icon.setAlternateText("icon");
						container.insertFirst(icon.getElement());
					}

					String classname = "v-treetable-treespacer";
					if (rowUidl.getBooleanAttribute("ca")) {
						canHaveChildren = true;
						open = rowUidl.getBooleanAttribute("open");
						classname += open ? " v-treetable-node-open" : " v-treetable-node-closed";
					}

					treeSpacer = Document.get().createSpanElement();

					treeSpacer.setClassName(classname);
					container.insertFirst(treeSpacer);
					depth = rowUidl.hasAttribute("depth") ? rowUidl.getIntAttribute("depth") : 0;
					setIndent();
					isTreeCellAdded = true;
					return true;
				}
				return false;
			}

			private boolean cellShowsTreeHierarchy(int curColIndex) {
				if (isTreeCellAdded) {
					return false;
				}
				return curColIndex == getHierarchyColumnIndex();
			}

			@Override
			public void onBrowserEvent(Event event) {
				if (event.getEventTarget().cast() == treeSpacer && treeSpacer.getClassName().contains("node")) {
					if (event.getTypeInt() == Event.ONMOUSEUP) {
						sendToggleCollapsedUpdate(getKey());
					}
					return;
				}
				super.onBrowserEvent(event);
			}

			@Override
			public void addCell(UIDL rowUidl, Widget w, char align, String style, boolean isSorted, String description) {
				super.addCell(rowUidl, w, align, style, isSorted, description);
				if (addTreeSpacer(rowUidl)) {
					widgetInHierarchyColumn = w;
				}

			}

			private void setIndent() {
				if (getIndentWidth() > 0) {
					treeSpacer.getParentElement().getStyle().setPaddingLeft(getIndent(), Unit.PX);
					treeSpacer.getStyle().setWidth(getIndent(), Unit.PX);
					int colWidth = getColWidth(getHierarchyColumnIndex());
					if (colWidth > 0 && getIndent() > colWidth) {
						PMTreeTableWidget.this.setColWidth(getHierarchyColumnIndex(), getIndent(), false);
					}
				}
			}

			@Override
			protected void onAttach() {
				super.onAttach();
				if (getIndentWidth() < 0) {
					detectIndent(this);
					// If we detect indent here then the size of the hierarchy
					// column is still wrong as it has been set when the indent
					// was not known.
					int w = getCellWidthFromDom(getHierarchyColumnIndex());
					if (w >= 0) {
						setColWidth(getHierarchyColumnIndex(), w);
					}
				}
			}

			private int getCellWidthFromDom(int cellIndex) {
				final Element cell = DOM.getChild(getElement(), cellIndex);
				String w = cell.getStyle().getProperty("width");
				if (w == null || "".equals(w) || !w.endsWith("px")) {
					return -1;
				} else {
					return Integer.parseInt(w.substring(0, w.length() - 2));
				}
			}

			private int getHierarchyAndIconWidth() {
				int consumedSpace = treeSpacer.getOffsetWidth();
				if (treeSpacer.getParentElement().getChildCount() > 2) {
					// icon next to tree spacer
					consumedSpace += ((com.google.gwt.dom.client.Element) treeSpacer.getNextSibling()).getOffsetWidth();
				}
				return consumedSpace;
			}

			@Override
			protected void setCellWidth(int cellIx, int width) {
				if (cellIx == getHierarchyColumnIndex()) {
					// take indentation padding into account if this is the
					// hierarchy column
					int indent = getIndent();
					if (indent != -1) {
						width = Math.max(width - indent, 0);
					}
				}
				super.setCellWidth(cellIx, width);
			}

			private int getIndent() {
				return (depth + 1) * getIndentWidth();
			}
		}

		protected class PMTreeTableWidgetGeneratedRow extends PMTreeTableWidgetRow {
			private boolean spanColumns;
			private boolean htmlContentAllowed;

			public PMTreeTableWidgetGeneratedRow(UIDL uidl, char[] aligns) {
				super(uidl, aligns);
				addStyleName("v-table-generated-row");
			}

			public boolean isSpanColumns() {
				return spanColumns;
			}

			@Override
			protected void initCellWidths() {
				if (spanColumns) {
					setSpannedColumnWidthAfterDOMFullyInited();
				} else {
					super.initCellWidths();
				}
			}

			private void setSpannedColumnWidthAfterDOMFullyInited() {
				// Defer setting width on spanned columns to make sure that
				// they are added to the DOM before trying to calculate
				// widths.
				Scheduler.get().scheduleDeferred(new ScheduledCommand() {

					@Override
					public void execute() {
						if (showRowHeaders) {
							setCellWidth(0, tHead.getHeaderCell(0).getWidthWithIndent());
							calcAndSetSpanWidthOnCell(1);
						} else {
							calcAndSetSpanWidthOnCell(0);
						}
					}
				});
			}

			@Override
			protected boolean isRenderHtmlInCells() {
				return htmlContentAllowed;
			}

			@Override
			protected void addCellsFromUIDL(UIDL uidl, char[] aligns, int col, int visibleColumnIndex) {
				htmlContentAllowed = uidl.getBooleanAttribute("gen_html");
				spanColumns = uidl.getBooleanAttribute("gen_span");

				final Iterator<?> cells = uidl.getChildIterator();
				if (spanColumns) {
					int colCount = uidl.getChildCount();
					if (cells.hasNext()) {
						final Object cell = cells.next();
						if (cell instanceof String) {
							addSpannedCell(uidl, cell.toString(), aligns[0], "", htmlContentAllowed, false, null, colCount);
						} else {
							addSpannedCell(uidl, (Widget) cell, aligns[0], "", false, colCount);
						}
					}
				} else {
					super.addCellsFromUIDL(uidl, aligns, col, visibleColumnIndex);
				}
			}

			private void addSpannedCell(UIDL rowUidl, Widget w, char align, String style, boolean sorted, int colCount) {
				TableCellElement td = DOM.createTD().cast();
				td.setColSpan(colCount);
				initCellWithWidget(w, align, style, sorted, td);
				if (addTreeSpacer(rowUidl)) {
					widgetInHierarchyColumn = w;
				}
			}

			private void addSpannedCell(UIDL rowUidl, String text, char align, String style, boolean textIsHTML,
					boolean sorted, String description, int colCount) {
				// String only content is optimized by not using Label widget
				final TableCellElement td = DOM.createTD().cast();
				td.setColSpan(colCount);
				initCellWithText(text, align, style, textIsHTML, sorted, description, td);
				addTreeSpacer(rowUidl);
			}

			@Override
			protected void setCellWidth(int cellIx, int width) {
				if (isSpanColumns()) {
					if (showRowHeaders) {
						if (cellIx == 0) {
							super.setCellWidth(0, width);
						} else {
							// We need to recalculate the spanning TDs width for
							// every cellIx in order to support column resizing.
							calcAndSetSpanWidthOnCell(1);
						}
					} else {
						// Same as above.
						calcAndSetSpanWidthOnCell(0);
					}
				} else {
					super.setCellWidth(cellIx, width);
				}
			}

			private void calcAndSetSpanWidthOnCell(final int cellIx) {
				int spanWidth = 0;
				for (int ix = (showRowHeaders ? 1 : 0); ix < tHead.getVisibleCellCount(); ix++) {
					spanWidth += tHead.getHeaderCell(ix).getOffsetWidth();
				}
				Util.setWidthExcludingPaddingAndBorder((com.google.gwt.user.client.Element) getElement().getChild(cellIx),
						spanWidth, 13, false);
			}
		}

		private int getIndentWidth() {
			return indentWidth;
		}

		@Override
		protected int getMaxIndent() {
			return maxIndent;
		}

		@Override
		protected void calculateMaxIndent() {
			int maxIndent = 0;
			Iterator<Widget> iterator = iterator();
			while (iterator.hasNext()) {
				PMTreeTableWidgetRow next = (PMTreeTableWidgetRow) iterator.next();
				maxIndent = Math.max(maxIndent, next.getIndent());
			}
			this.maxIndent = maxIndent;
		}

		private void detectIndent(PMTreeTableWidgetRow PMTreeTableWidgetRow) {
			indentWidth = PMTreeTableWidgetRow.treeSpacer.getOffsetWidth();
			if (indentWidth == 0) {
				indentWidth = -1;
				return;
			}
			Iterator<Widget> iterator = iterator();
			while (iterator.hasNext()) {
				PMTreeTableWidgetRow next = (PMTreeTableWidgetRow) iterator.next();
				next.setIndent();
			}
			calculateMaxIndent();
		}

	}

	/**
	 * Icons rendered into first actual column in TreeTable, not to row header cell
	 */
	@Override
	protected String buildCaptionHtmlSnippet(UIDL uidl) {
		if (uidl.getTag().equals("column")) {
			return super.buildCaptionHtmlSnippet(uidl);
		} else {
			String s = uidl.getStringAttribute("caption");
			return s;
		}
	}

	/** For internal use only. May be removed or replaced in the future. */
	@Override
	public boolean handleNavigation(int keycode, boolean ctrl, boolean shift) {
		if (collapseRequest || focusParentResponsePending) {
			// Enqueue the event if there might be pending content changes from
			// the server
			if (pendingNavigationEvents.size() < 10) {
				// Only keep 10 keyboard events in the queue
				PendingNavigationEvent pendingNavigationEvent = new PendingNavigationEvent(keycode, ctrl, shift);
				pendingNavigationEvents.add(pendingNavigationEvent);
			}
			return true;
		}

		PMTreeTableWidgetRow focusedRow = (PMTreeTableWidgetRow) getFocusedRow();
		if (focusedRow != null) {
			if (focusedRow.canHaveChildren
					&& ((keycode == KeyCodes.KEY_RIGHT && !focusedRow.open) || (keycode == KeyCodes.KEY_LEFT && focusedRow.open))) {
				if (!ctrl) {
					client.updateVariable(paintableId, "selectCollapsed", true, false);
				}
				sendSelectedRows(false);
				sendToggleCollapsedUpdate(focusedRow.getKey());
				return true;
			} else if (keycode == KeyCodes.KEY_RIGHT && focusedRow.open) {
				// already expanded, move selection down if next is on a deeper
				// level (is-a-child)
				PMTreeTableWidgetScrollBody body = (PMTreeTableWidgetScrollBody) focusedRow.getParent();
				Iterator<Widget> iterator = body.iterator();
				PMTreeTableWidgetRow next = null;
				while (iterator.hasNext()) {
					next = (PMTreeTableWidgetRow) iterator.next();
					if (next == focusedRow) {
						next = (PMTreeTableWidgetRow) iterator.next();
						break;
					}
				}
				if (next != null) {
					if (next.depth > focusedRow.depth) {
						selectionPending = true;
						return super.handleNavigation(getNavigationDownKey(), ctrl, shift);
					}
				} else {
					// Note, a minor change here for a bit false behavior if
					// cache rows is disabled + last visible row + no childs for
					// the node
					selectionPending = true;
					return super.handleNavigation(getNavigationDownKey(), ctrl, shift);
				}
			} else if (keycode == KeyCodes.KEY_LEFT) {
				// already collapsed move selection up to parent node
				// do on the server side as the parent is not necessary
				// rendered on the client, could check if parent is visible if
				// a performance issue arises

				client.updateVariable(paintableId, "focusParent", focusedRow.getKey(), true);

				// Set flag that we should enqueue navigation events until we
				// get a response to this request
				focusParentResponsePending = true;

				return true;
			}
		}
		return super.handleNavigation(keycode, ctrl, shift);
	}

	private void sendToggleCollapsedUpdate(String rowKey) {
		collapsedRowKey = rowKey;
		collapseRequest = true;
		client.updateVariable(paintableId, "toggleCollapsed", rowKey, true);
	}

	@Override
	public void onBrowserEvent(Event event) {
		super.onBrowserEvent(event);
		if (event.getTypeInt() == Event.ONKEYUP && selectionPending) {
			sendSelectedRows();
		}
	}

	@Override
	protected void sendSelectedRows(boolean immediately) {
		super.sendSelectedRows(immediately);
		selectionPending = false;
	}

	@Override
	protected void reOrderColumn(String columnKey, int newIndex) {
		super.reOrderColumn(columnKey, newIndex);
		// current impl not intelligent enough to survive without visiting the
		// server to redraw content
		client.sendPendingVariableChanges();
	}

	@Override
	public void setStyleName(String style) {
		super.setStyleName(style + " v-treetable");
	}
}