package ch.meemin.demo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import javax.servlet.annotation.WebServlet;

import ch.meemin.pmtable.PMTable.ColumnCollapseEvent;
import ch.meemin.pmtable.PMTable.ColumnCollapseListener;
import ch.meemin.pmtable.PMTable.TableDragMode;
import ch.meemin.pmtable.PMTableHierarchicalContainer;
import ch.meemin.pmtable.PMTreeTable;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.data.util.converter.StringToFloatConverter;
import com.vaadin.data.validator.FloatRangeValidator;
import com.vaadin.event.Action;
import com.vaadin.event.Action.Handler;
import com.vaadin.event.DataBoundTransferable;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutAction.ModifierKey;
import com.vaadin.event.Transferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.event.dd.acceptcriteria.SourceIsTarget;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.ui.MultiSelectMode;
import com.vaadin.shared.ui.dd.VerticalDropLocation;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractSelect.AbstractSelectTargetDetails;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@Theme("demo")
@Title("PMTable Demo")
@SuppressWarnings("serial")
public class DemoUI extends UI implements Handler, DropHandler, ColumnCollapseListener, ValueChangeListener {

	@WebServlet(value = "/*", asyncSupported = true)
	@VaadinServletConfiguration(productionMode = false, ui = DemoUI.class, widgetset = "ch.meemin.demo.DemoWidgetSet")
	public static class Servlet extends VaadinServlet {}

	private final static Random rand = new Random();
	private final PMTreeTable pmTreeTable = new PMTreeTable();

	private class MyTreeTable extends TreeTable {

		public MyTreeTable() {
			super();
			alwaysRecalculateColumnWidths = true;
		}
	}

	private final MyTreeTable table = new MyTreeTable();

	private Action actionRemove = new ShortcutAction("Remove", null, KeyCode.DELETE, new int[] { ModifierKey.CTRL });
	private Action actionChange = new ShortcutAction("Change");
	private Action[] actions = new Action[] { actionRemove, actionChange };

	VerticalLayout mainLayout = new VerticalLayout();

	@Override
	protected void init(VaadinRequest request) {

		prepareTables();
		fillTables();

		pmTreeTable.setHeight(400, Unit.PIXELS);
		pmTreeTable.setWidth(100, Unit.PERCENTAGE);
		table.setHeight(400, Unit.PIXELS);
		table.setWidth(100, Unit.PERCENTAGE);

		Button selectB = prepareSelectButton();
		Button stopSelect = prepareStopSelectButton();

		final TextField factorField = prepareOffsetField();
		Button srollTo = prepareScrollButton();

		HorizontalLayout selectL = new HorizontalLayout(selectB, stopSelect);
		HorizontalLayout scrollL = new HorizontalLayout(factorField, srollTo);

		Button addB = new Button("Add", new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				addItems();
			}
		});

		Label title = new Label("PMTreeTable Demo");
		title.setStyleName(ValoTheme.LABEL_H3);
		Label description = new Label("Try Right-Click and Drag'n'Drop");

		mainLayout.addComponents(title, description, selectL, scrollL, addB);
		mainLayout.setWidth(100, Unit.PERCENTAGE);
		mainLayout.addComponent(pmTreeTable);
		mainLayout.addComponent(table);
		setContent(mainLayout);
	}

	private Button prepareScrollButton() {
		Button srollTo = new Button("Scroll to show Random", new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				ArrayList<?> ids = new ArrayList(pmTreeTable.getItemIds());
				Object id = ids.get(rand.nextInt(ids.size()));
				pmTreeTable.scrollToElement(id);
				Notification.show("Scrolling to: " + id);
			}
		});
		return srollTo;
	}

	private TextField prepareOffsetField() {
		final TextField factorField = new TextField();
		factorField.setInputPrompt("OffsetFactor");
		factorField.addValidator(new FloatRangeValidator("Must be between 0 and 1", 0f, 1f));
		factorField.setConverter(new StringToFloatConverter());
		factorField.addValueChangeListener(new ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				Float f = (Float) factorField.getConvertedValue();
				pmTreeTable.setScrollToElementOffsetFactor(f != null ? f : 0);
			}
		});
		return factorField;
	}

	private Button prepareStopSelectButton() {
		Button stopSelect = new Button("Stop Select", new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {

				pmTreeTable.clear();
				pmTreeTable.setSelectable(false);
				table.clear();
				table.setSelectable(false);

			}
		});
		return stopSelect;
	}

	private Button prepareSelectButton() {
		Button selectB = new Button("Select", new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				HashSet<Object> selects = new HashSet<Object>();
				for (Object id : pmTreeTable.getItemIds()) {
					if (rand.nextBoolean())
						selects.add(id);
				}
				pmTreeTable.setSelectable(true);
				pmTreeTable.setValue(selects);
				table.setSelectable(true);
				table.setValue(selects);
			}
		});
		return selectB;
	}

	private void fillTables() {
		for (int i = 0; i < 80; i++) {
			addItems();

		}
	}

	private void addItems() {
		Object o = pmTreeTable.addItem();
		boolean b = rand.nextBoolean();
		Label l = new Label();
		l.setContentMode(ContentMode.HTML);
		l.setValue(b ? "foo<br />bar<br />blub" : "foobar");
		pmTreeTable.getItem(o).getItemProperty(2).setValue(l);
		pmTreeTable.getItem(o).getItemProperty(1).setValue("ID: " + o);

		o = table.addItem();
		l = new Label();
		l.setValue(b ? "foo<br />bar<br />blub" : "foobar");
		l.setContentMode(ContentMode.HTML);
		table.getItem(o).getItemProperty(2).setValue(l);
		table.getItem(o).getItemProperty(1).setValue("ID: " + o);
	}

	private void prepareTables() {
		pmTreeTable.addActionHandler(this);
		pmTreeTable.setDropHandler(this);
		pmTreeTable.setDragMode(TableDragMode.ROW);
		pmTreeTable.setSelectable(true);
		pmTreeTable.setMultiSelect(true);
		pmTreeTable.setMultiSelectMode(MultiSelectMode.SIMPLE);
		pmTreeTable.setImmediate(true);
		pmTreeTable.addValueChangeListener(this);

		table.addActionHandler(this);
		table.setDropHandler(this);
		table.setDragMode(Table.TableDragMode.ROW);
		table.setSelectable(true);
		table.setMultiSelect(true);
		table.setMultiSelectMode(MultiSelectMode.SIMPLE);
		table.setImmediate(true);
		table.addValueChangeListener(this);

		pmTreeTable.addContainerProperty(1, String.class, "PMTable");
		pmTreeTable.addContainerProperty(2, Label.class, null);
		pmTreeTable.setColumnWidth(1, -1);
		pmTreeTable.setColumnWidth(2, -1);
		pmTreeTable.setColumnCollapsingAllowed(true);
		pmTreeTable.addColumnCollapseListener(this);

		table.addContainerProperty(1, String.class, "vaadinTable");
		table.addContainerProperty(2, Label.class, null);
		table.setColumnWidth(1, -1);
		table.setColumnWidth(2, -1);
	}

	@Override
	public Action[] getActions(Object target, Object sender) {
		return actions;
	}

	@Override
	public void handleAction(Action action, Object sender, Object target) {
		if (actionRemove.equals(action)) {
			if (sender.equals(pmTreeTable))
				pmTreeTable.removeItem(target);
			else
				table.removeItem(target);
		} else if (actionChange.equals(action)) {
			if (sender.equals(pmTreeTable))
				pmTreeTable.getItem(target).getItemProperty(2).setValue(Integer.toString(rand.nextInt(99999)));
			else
				table.getItem(target).getItemProperty(2).setValue(Integer.toString(rand.nextInt(99999)));
		}
	}

	@Override
	public void drop(DragAndDropEvent event) {
		Transferable t = event.getTransferable();
		AbstractSelectTargetDetails dropData = ((AbstractSelectTargetDetails) event.getTargetDetails());
		Object itemId = ((DataBoundTransferable) t).getItemId();
		Object targetItemId = dropData.getItemIdOver();
		VerticalDropLocation location = dropData.getDropLocation();
		if (itemId == null || targetItemId == null || itemId.equals(targetItemId))
			return;
		if (dropData.getTarget().equals(pmTreeTable)) {
			PMTableHierarchicalContainer container = (PMTableHierarchicalContainer) pmTreeTable.getContainerDataSource();
			if (location == VerticalDropLocation.MIDDLE) {
				if (pmTreeTable.setParent(itemId, targetItemId) && pmTreeTable.hasChildren(targetItemId))
					container.moveAfterSibling(itemId, null);
			} else if (location == VerticalDropLocation.TOP) {
				if (pmTreeTable.setParent(itemId, container.getParent(targetItemId))) {
					container.moveAfterSibling(itemId, targetItemId);
					container.moveAfterSibling(targetItemId, itemId);
					pmTreeTable.setCollapsed(targetItemId, false);
				}
			} else if (location == VerticalDropLocation.BOTTOM) {
				if (pmTreeTable.setParent(itemId, targetItemId))
					container.moveAfterSibling(itemId, targetItemId);
			}
		} else {
			HierarchicalContainer container = (HierarchicalContainer) table.getContainerDataSource();
			if (location == VerticalDropLocation.MIDDLE) {
				if (table.setParent(itemId, targetItemId) && table.hasChildren(targetItemId))
					container.moveAfterSibling(itemId, null);
			} else if (location == VerticalDropLocation.TOP) {
				if (table.setParent(itemId, container.getParent(targetItemId))) {
					container.moveAfterSibling(itemId, targetItemId);
					container.moveAfterSibling(targetItemId, itemId);
					table.setCollapsed(targetItemId, false);
				}
			} else if (location == VerticalDropLocation.BOTTOM) {
				if (table.setParent(itemId, targetItemId))
					container.moveAfterSibling(itemId, targetItemId);
			}
		}
	}

	@Override
	public AcceptCriterion getAcceptCriterion() {
		return SourceIsTarget.get();
	}

	@Override
	public void columnCollapseStateChange(ColumnCollapseEvent event) {
		final Object propertyId = event.getPropertyId();
		if (pmTreeTable.isColumnCollapsed(propertyId))
			Notification.show("Column collapsed: " + propertyId);
		else
			Notification.show("Column expanded: " + propertyId);
	}

	@Override
	public void valueChange(ValueChangeEvent event) {
		Property prop = event.getProperty();
		if (prop.equals(pmTreeTable))
			Notification.show("PMTable: " + prop.getValue());
		// else
		// Notification.show("Default Table: " + prop.getValue());
	}
}
