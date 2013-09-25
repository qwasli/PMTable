package ch.meemin.pmtable;

import java.util.ArrayList;
import java.util.Collection;

import ch.meemin.pmtable.PMTable.PMTableItemSetChangeEvent;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;

@SuppressWarnings("serial")
public class PMTableIndexedContainer extends IndexedContainer {

	@Override
	protected void fireItemAdded(int position, Object itemId, Item item) {
		fireItemAdded(itemId);
	}

	protected void fireItemAdded(Object itemId) {
		fireItemSetChange(new ItemSetChangeEvent(this, itemId, null));
	}

	protected void fireItemsAdded(Collection<Object> itemIds) {
		fireItemSetChange(new ItemSetChangeEvent(this, itemIds, null));
	}

	@Override
	protected void fireItemSetChange() {
		fireItemSetChange(new ItemSetChangeEvent(this));
	}

	@Override
	protected void fireItemRemoved(int position, Object itemId) {
		fireItemsRemoved(itemId);
	}

	protected void fireItemsRemoved(Object itemId) {
		fireItemSetChange(new ItemSetChangeEvent(this, null, itemId));
	}

	protected void fireItemRemoved(Collection<Object> itemIds) {
		fireItemSetChange(new ItemSetChangeEvent(this, null, itemIds));
	}

	public static class ItemSetChangeEvent extends BaseItemSetChangeEvent implements PMTableItemSetChangeEvent {
		private Collection<Object> added, removed;
		private boolean reordered;

		private ItemSetChangeEvent(IndexedContainer source) {
			super(source);
		}

		private ItemSetChangeEvent(IndexedContainer source, boolean reordered) {
			super(source);
			this.reordered = reordered;
		}

		protected ItemSetChangeEvent(IndexedContainer source, Collection<Object> addedIds, Collection<Object> removedIds) {
			super(source);
			this.added = addedIds;
			this.removed = removedIds;
		}

		private ItemSetChangeEvent(IndexedContainer source, Object addedId, Object removedId) {
			super(source);
			if (addedId != null) {
				this.added = new ArrayList<Object>();
				added.add(addedId);
			}
			if (removedId != null) {
				this.removed = new ArrayList<Object>();
				removed.add(removedId);
			}
		}

		@Override
		public Collection<Object> getInsertedIds() {
			return added;
		}

		@Override
		public Collection<Object> getRemovedIds() {
			return removed;
		}

		@Override
		public boolean isReordered() {
			return reordered;
		}
	}
}
