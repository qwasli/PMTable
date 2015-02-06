package ch.meemin.pmtable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.meemin.pmtable.widgetset.client.PMTreeTableConstants;

import com.vaadin.data.Collapsible;
import com.vaadin.data.Container;
import com.vaadin.data.Container.Hierarchical;
import com.vaadin.data.Container.ItemSetChangeEvent;
import com.vaadin.data.util.ContainerHierarchicalWrapper;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.data.util.HierarchicalContainerOrderedWrapper;
import com.vaadin.server.PaintException;
import com.vaadin.server.PaintTarget;
import com.vaadin.server.Resource;
import com.vaadin.ui.Tree.CollapseEvent;
import com.vaadin.ui.Tree.CollapseListener;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.Tree.ExpandListener;

public class PMTreeTable extends PMTable implements Hierarchical {
	private interface ContainerStrategy extends Serializable {
		public int size();

		public boolean isNodeOpen(Object itemId);

		public int getDepth(Object itemId);

		public void toggleChildVisibility(Object itemId);

		public Object getIdByIndex(int index);

		public int indexOfId(Object id);

		public Object nextItemId(Object itemId);

		public Object lastItemId();

		public Object prevItemId(Object itemId);

		public boolean isLastId(Object itemId);

		public Collection<?> getItemIds();

		public void containerItemSetChange(ItemSetChangeEvent event);
	}

	private abstract class AbstractStrategy implements ContainerStrategy {

		/**
		 * Consider adding getDepth to {@link Collapsible}, might help scalability with some container implementations.
		 */

		@Override
		public int getDepth(Object itemId) {
			int depth = 0;
			Hierarchical hierarchicalContainer = getContainerDataSource();
			while (!hierarchicalContainer.isRoot(itemId)) {
				depth++;
				itemId = hierarchicalContainer.getParent(itemId);
			}
			return depth;
		}

		@Override
		public void containerItemSetChange(ItemSetChangeEvent event) {}

	}

	/**
	 * This strategy is used if current container implements {@link Collapsible} .
	 * 
	 * open-collapsed logic diverted to container, otherwise use default implementations.
	 */
	private class CollapsibleStrategy extends AbstractStrategy {

		private Collapsible c() {
			return (Collapsible) getContainerDataSource();
		}

		@Override
		public void toggleChildVisibility(Object itemId) {
			c().setCollapsed(itemId, !c().isCollapsed(itemId));
		}

		@Override
		public boolean isNodeOpen(Object itemId) {
			return !c().isCollapsed(itemId);
		}

		@Override
		public int size() {
			return PMTreeTable.super.size();
		}

		@Override
		public Object getIdByIndex(int index) {
			return PMTreeTable.super.getIdByIndex(index);
		}

		@Override
		public int indexOfId(Object id) {
			return PMTreeTable.super.indexOfId(id);
		}

		@Override
		public boolean isLastId(Object itemId) {
			// using the default impl
			return PMTreeTable.super.isLastId(itemId);
		}

		@Override
		public Object lastItemId() {
			// using the default impl
			return PMTreeTable.super.lastItemId();
		}

		@Override
		public Object nextItemId(Object itemId) {
			return PMTreeTable.super.nextItemId(itemId);
		}

		@Override
		public Object prevItemId(Object itemId) {
			return PMTreeTable.super.prevItemId(itemId);
		}

		@Override
		public Collection<?> getItemIds() {
			return PMTreeTable.super.getItemIds();
		}

	}

	/**
	 * Strategy for Hierarchical but not Collapsible container like {@link HierarchicalContainer}.
	 * 
	 * Store collapsed/open states internally, fool Table to use preorder when accessing items from container via
	 * Ordered/Indexed methods.
	 */
	private class HierarchicalStrategy extends AbstractStrategy {

		private final HashSet<Object> openItems = new HashSet<Object>();

		@Override
		public boolean isNodeOpen(Object itemId) {
			return openItems.contains(itemId);
		}

		@Override
		public int size() {
			return getPreOrder().size();
		}

		@Override
		public Collection<Object> getItemIds() {
			return Collections.unmodifiableCollection(getPreOrder());
		}

		@Override
		public boolean isLastId(Object itemId) {
			if (itemId == null) {
				return false;
			}

			return itemId.equals(lastItemId());
		}

		@Override
		public Object lastItemId() {
			if (getPreOrder().size() > 0) {
				return getPreOrder().get(getPreOrder().size() - 1);
			} else {
				return null;
			}
		}

		@Override
		public Object nextItemId(Object itemId) {
			int indexOf = getPreOrder().indexOf(itemId);
			if (indexOf == -1) {
				return null;
			}
			indexOf++;
			if (indexOf == getPreOrder().size()) {
				return null;
			} else {
				return getPreOrder().get(indexOf);
			}
		}

		@Override
		public Object prevItemId(Object itemId) {
			int indexOf = getPreOrder().indexOf(itemId);
			indexOf--;
			if (indexOf < 0) {
				return null;
			} else {
				return getPreOrder().get(indexOf);
			}
		}

		@Override
		public void toggleChildVisibility(Object itemId) {
			boolean removed = openItems.remove(itemId);
			HashSet<Object> changed = new HashSet<Object>();
			if (!removed) {
				openItems.add(itemId);
				fillChildIds(changed, itemId, true);
				getLogger().log(Level.FINEST, "Item {0} is now expanded", itemId);
			} else {
				fillChildIds(changed, itemId, false);
				getLogger().log(Level.FINEST, "Item {0} is now collapsed", itemId);
			}
			clearPreorderCache();

			if (!removed)
				for (Object id : changed)
					setItemInserted(id);
			else
				for (Object id : changed)
					setItemRemoved(id);

			PMTreeTable.super.setItemChanged(itemId);
		}

		private void fillChildIds(HashSet<Object> childIds, Object id, boolean onlyVisible) {
			if (!onlyVisible || isNodeOpen(id)) {
				Collection<?> children = getContainerDataSource().getChildren(id);
				if (children != null) {
					for (Object childId : children) {
						childIds.add(childId);
						fillChildIds(childIds, childId, onlyVisible);
					}
				}
			}
		}

		private void clearPreorderCache() {
			preOrder = null; // clear preorder cache
		}

		List<Object> preOrder;

		/**
		 * Preorder of ids currently visible
		 * 
		 * @return
		 */
		private List<Object> getPreOrder() {
			if (preOrder == null) {
				preOrder = new ArrayList<Object>();
				Collection<?> rootItemIds = getContainerDataSource().rootItemIds();
				for (Object id : rootItemIds) {
					preOrder.add(id);
					addVisibleChildTree(id);
				}
			}
			return preOrder;
		}

		private void addVisibleChildTree(Object id) {
			if (isNodeOpen(id)) {
				Collection<?> children = getContainerDataSource().getChildren(id);
				if (children != null) {
					for (Object childId : children) {
						preOrder.add(childId);
						addVisibleChildTree(childId);
					}
				}
			}

		}

		@Override
		public int indexOfId(Object id) {
			return getPreOrder().indexOf(id);
		}

		@Override
		public Object getIdByIndex(int index) {
			return getPreOrder().get(index);
		}

		@Override
		public void containerItemSetChange(ItemSetChangeEvent event) {
			// preorder becomes invalid on sort, item additions etc.
			clearPreorderCache();
			if (event instanceof PMTable.PMTableItemSetChangeEvent) {
				Collection<Object> inserted = ((PMTableItemSetChangeEvent) event).getInsertedIds();
				Collection<Object> removed = ((PMTableItemSetChangeEvent) event).getRemovedIds();
				if (inserted != null) {
					Collection<Object> ids = getItemIds();
					for (Iterator<Object> it = inserted.iterator(); it.hasNext();) {
						Object id = it.next();
						if (!ids.contains(id)) {
							if (removed == null) {
								fullRefresh(false);
								return;
							} else
								removed.add(id);
							it.remove();
						}
					}
				}
			}
			super.containerItemSetChange(event);
		}
	}

	/**
	 * Creates an empty TreeTable with a default container.
	 */
	public PMTreeTable() {
		super(null, new PMTableHierarchicalContainer());
	}

	/**
	 * Creates an empty TreeTable with a default container.
	 * 
	 * @param caption
	 *          the caption for the TreeTable
	 */
	public PMTreeTable(String caption) {
		this();
		setCaption(caption);
	}

	/**
	 * Creates a TreeTable instance with given captions and data source.
	 * 
	 * @param caption
	 *          the caption for the component
	 * @param dataSource
	 *          the dataSource that is used to list items in the component
	 */
	public PMTreeTable(String caption, Container dataSource) {
		super(caption, dataSource);
	}

	private ContainerStrategy cStrategy;
	private Object focusedRowId = null;
	private Object hierarchyColumnId;

	// private boolean animationsEnabled;
	private boolean clearFocusedRowPending;

	private ContainerStrategy getContainerStrategy() {
		if (cStrategy == null) {
			if (getContainerDataSource() instanceof Collapsible) {
				cStrategy = new CollapsibleStrategy();
			} else {
				cStrategy = new HierarchicalStrategy();
			}
		}
		return cStrategy;
	}

	@Override
	protected void paintRowAttributes(PaintTarget target, Object itemId) throws PaintException {
		super.paintRowAttributes(target, itemId);
		target.addAttribute("depth", getContainerStrategy().getDepth(itemId));
		if (getContainerDataSource().areChildrenAllowed(itemId)) {
			target.addAttribute("ca", true);
			target.addAttribute("open", getContainerStrategy().isNodeOpen(itemId));
		}
	}

	@Override
	protected void paintRowIcon(PaintTarget target, EnumMap<RowAttributes, Object> rowAtts) throws PaintException {
		// always paint if present (in parent only if row headers visible)
		if (getRowHeaderMode() == RowHeaderMode.HIDDEN) {
			Resource itemIcon = getItemIcon(rowAtts.get(RowAttributes.ITEMID));
			if (itemIcon != null) {
				target.addAttribute("icon", itemIcon);
			}
		} else
			super.paintRowIcon(target, rowAtts);
	}

	@Override
	protected boolean rowHeadersAreEnabled() {
		if (getRowHeaderMode() == RowHeaderMode.ICON_ONLY) {
			return false;
		}
		return super.rowHeadersAreEnabled();
	}

	@Override
	public void changeVariables(Object source, Map<String, Object> variables) {
		super.changeVariables(source, variables);

		if (variables.containsKey("toggleCollapsed")) {
			String object = (String) variables.get("toggleCollapsed");
			Object itemId = itemIdMapper.get(object);
			toggleChildVisibility(itemId);
			if (variables.containsKey("selectCollapsed")) {
				// ensure collapsed is selected unless opened with selection
				// head
				if (isSelectable()) {
					select(itemId);
				}
			}
		} else if (variables.containsKey("focusParent")) {
			String key = (String) variables.get("focusParent");
			Object refId = itemIdMapper.get(key);
			Object itemId = getParent(refId);
			focusParent(itemId);
		}
	}

	private void focusParent(Object itemId) {

		// Select the row if it is selectable.
		if (isSelectable()) {
			if (isMultiSelect()) {
				setValue(Collections.singleton(itemId));
			} else {
				setValue(itemId);
			}
		}
		setFocusedRow(itemId);
	}

	private void setFocusedRow(Object itemId) {
		focusedRowId = itemId;
		if (focusedRowId == null) {
			// Must still inform the client that the focusParent request has
			// been processed
			clearFocusedRowPending = true;
		}
		markAsDirty();
	}

	@Override
	public void paintContent(PaintTarget target) throws PaintException {
		if (focusedRowId != null) {
			target.addAttribute("focusedRow", itemIdMapper.key(focusedRowId));
			focusedRowId = null;
		} else if (clearFocusedRowPending) {
			// Must still inform the client that the focusParent request has
			// been processed
			target.addAttribute("clearFocusPending", true);
			clearFocusedRowPending = false;
		}
		// target.addAttribute("animate", animationsEnabled);
		if (hierarchyColumnId != null) {
			List<Object> visibleColumns2 = getVisibleColumns();
			for (int i = 0; i < visibleColumns2.size(); i++) {
				Object object = visibleColumns2.get(i);
				if (hierarchyColumnId.equals(object)) {
					target.addAttribute(PMTreeTableConstants.ATTRIBUTE_HIERARCHY_COLUMN_INDEX, i);
					break;
				}
			}
		}
		super.paintContent(target);
	}

	private void toggleChildVisibility(Object itemId) {
		getContainerStrategy().toggleChildVisibility(itemId);

		if (isCollapsed(itemId)) {
			fireCollapseEvent(itemId);
		} else {
			fireExpandEvent(itemId);
		}

		markAsDirty();
	}

	@Override
	public int size() {
		return getContainerStrategy().size();
	}

	@Override
	public Hierarchical getContainerDataSource() {
		return (Hierarchical) super.getContainerDataSource();
	}

	@Override
	public void setContainerDataSource(Container newDataSource) {
		cStrategy = null;

		if (!(newDataSource instanceof Hierarchical)) {
			newDataSource = new ContainerHierarchicalWrapper(newDataSource);
		}

		if (!(newDataSource instanceof Ordered)) {
			newDataSource = new HierarchicalContainerOrderedWrapper((Hierarchical) newDataSource);
		}

		super.setContainerDataSource(newDataSource);
	}

	@Override
	public void containerItemSetChange(com.vaadin.data.Container.ItemSetChangeEvent event) {
		getContainerStrategy().containerItemSetChange(event);
		super.containerItemSetChange(event);
	}

	@Override
	protected Object getIdByIndex(int index) {
		return getContainerStrategy().getIdByIndex(index);
	}

	@Override
	protected int indexOfId(Object itemId) {
		return getContainerStrategy().indexOfId(itemId);
	}

	@Override
	public Object nextItemId(Object itemId) {
		return getContainerStrategy().nextItemId(itemId);
	}

	@Override
	public Object lastItemId() {
		return getContainerStrategy().lastItemId();
	}

	@Override
	public Object prevItemId(Object itemId) {
		return getContainerStrategy().prevItemId(itemId);
	}

	@Override
	public boolean isLastId(Object itemId) {
		return getContainerStrategy().isLastId(itemId);
	}

	@Override
	public Collection<?> getItemIds() {
		return getContainerStrategy().getItemIds();
	}

	@Override
	public boolean areChildrenAllowed(Object itemId) {
		return getContainerDataSource().areChildrenAllowed(itemId);
	}

	@Override
	public Collection<?> getChildren(Object itemId) {
		return getContainerDataSource().getChildren(itemId);
	}

	@Override
	public Object getParent(Object itemId) {
		return getContainerDataSource().getParent(itemId);
	}

	@Override
	public boolean hasChildren(Object itemId) {
		return getContainerDataSource().hasChildren(itemId);
	}

	@Override
	public boolean isRoot(Object itemId) {
		return getContainerDataSource().isRoot(itemId);
	}

	@Override
	public Collection<?> rootItemIds() {
		return getContainerDataSource().rootItemIds();
	}

	@Override
	public boolean setChildrenAllowed(Object itemId, boolean areChildrenAllowed) throws UnsupportedOperationException {
		return getContainerDataSource().setChildrenAllowed(itemId, areChildrenAllowed);
	}

	@Override
	public boolean setParent(Object itemId, Object newParentId) throws UnsupportedOperationException {
		return getContainerDataSource().setParent(itemId, newParentId);
	}

	/**
	 * Sets the Item specified by given identifier as collapsed or expanded. If the Item is collapsed, its children are
	 * not displayed to the user.
	 * 
	 * @param itemId
	 *          the identifier of the Item
	 * @param collapsed
	 *          true if the Item should be collapsed, false if expanded
	 */
	public void setCollapsed(Object itemId, boolean collapsed) {
		if (isCollapsed(itemId) != collapsed)
			toggleChildVisibility(itemId);
	}

	/**
	 * Checks if Item with given identifier is collapsed in the UI.
	 * 
	 * <p>
	 * 
	 * @param itemId
	 *          the identifier of the checked Item
	 * @return true if the Item with given id is collapsed
	 * @see Collapsible#isCollapsed(Object)
	 */
	public boolean isCollapsed(Object itemId) {
		return !getContainerStrategy().isNodeOpen(itemId);
	}

	/**
	 * Explicitly sets the column in which the TreeTable visualizes the hierarchy. If hierarchyColumnId is not set, the
	 * hierarchy is visualized in the first visible column.
	 * 
	 * @param hierarchyColumnId
	 */
	public void setHierarchyColumn(Object hierarchyColumnId) {
		this.hierarchyColumnId = hierarchyColumnId;
	}

	/**
	 * @return the identifier of column into which the hierarchy will be visualized or null if the column is not
	 *         explicitly defined.
	 */
	public Object getHierarchyColumnId() {
		return hierarchyColumnId;
	}

	/**
	 * Adds an expand listener.
	 * 
	 * @param listener
	 *          the Listener to be added.
	 */
	public void addExpandListener(ExpandListener listener) {
		addListener(ExpandEvent.class, listener, ExpandListener.EXPAND_METHOD);
	}

	/**
	 * @deprecated As of 7.0, replaced by {@link #addExpandListener(ExpandListener)}
	 **/
	@Deprecated
	public void addListener(ExpandListener listener) {
		addExpandListener(listener);
	}

	/**
	 * Removes an expand listener.
	 * 
	 * @param listener
	 *          the Listener to be removed.
	 */
	public void removeExpandListener(ExpandListener listener) {
		removeListener(ExpandEvent.class, listener, ExpandListener.EXPAND_METHOD);
	}

	/**
	 * @deprecated As of 7.0, replaced by {@link #removeExpandListener(ExpandListener)}
	 **/
	@Deprecated
	public void removeListener(ExpandListener listener) {
		removeExpandListener(listener);
	}

	/**
	 * Emits an expand event.
	 * 
	 * @param itemId
	 *          the item id.
	 */
	protected void fireExpandEvent(Object itemId) {
		fireEvent(new ExpandEvent(this, itemId));
	}

	/**
	 * Adds a collapse listener.
	 * 
	 * @param listener
	 *          the Listener to be added.
	 */
	public void addCollapseListener(CollapseListener listener) {
		addListener(CollapseEvent.class, listener, CollapseListener.COLLAPSE_METHOD);
	}

	/**
	 * @deprecated As of 7.0, replaced by {@link #addCollapseListener(CollapseListener)}
	 **/
	@Deprecated
	public void addListener(CollapseListener listener) {
		addCollapseListener(listener);
	}

	/**
	 * Removes a collapse listener.
	 * 
	 * @param listener
	 *          the Listener to be removed.
	 */
	public void removeCollapseListener(CollapseListener listener) {
		removeListener(CollapseEvent.class, listener, CollapseListener.COLLAPSE_METHOD);
	}

	/**
	 * @deprecated As of 7.0, replaced by {@link #removeCollapseListener(CollapseListener)}
	 **/
	@Deprecated
	public void removeListener(CollapseListener listener) {
		removeCollapseListener(listener);
	}

	/**
	 * Emits a collapse event.
	 * 
	 * @param itemId
	 *          the item id.
	 */
	protected void fireCollapseEvent(Object itemId) {
		fireEvent(new CollapseEvent(this, itemId));
	}

	private static final Logger getLogger() {
		return Logger.getLogger(PMTreeTable.class.getName());
	}
}
