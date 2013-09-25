package ch.meemin.demo;

import java.util.Random;

import javax.servlet.annotation.WebServlet;

import ch.meemin.pmtable.PMTable.TableDragMode;
import ch.meemin.pmtable.PMTableHierarchicalContainer;
import ch.meemin.pmtable.PMTreeTable;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.data.Item;
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
import com.vaadin.shared.ui.dd.VerticalDropLocation;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractSelect.AbstractSelectTargetDetails;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

@Theme("demo")
@Title("PMTable Demo")
@SuppressWarnings("serial")
public class DemoUI extends UI implements Handler, DropHandler {

	@WebServlet(value = "/*", asyncSupported = true)
	@VaadinServletConfiguration(productionMode = false, ui = DemoUI.class, widgetset = "ch.meemin.demo.DemoWidgetSet")
	public static class Servlet extends VaadinServlet {}

	private final static Random rand = new Random();
	private final PMTreeTable pmTreeTable = new PMTreeTable();

	private Action actionRemove = new ShortcutAction("Remove", null, KeyCode.DELETE, new int[] { ModifierKey.CTRL });
	private Action actionChange = new ShortcutAction("Change");
	private Action[] actions = new Action[] { actionRemove, actionChange };

	@Override
	protected void init(VaadinRequest request) {

		pmTreeTable.addActionHandler(this);
		pmTreeTable.setDropHandler(this);
		pmTreeTable.setDragMode(TableDragMode.ROW);

		pmTreeTable.addContainerProperty(1, String.class, "foo");
		pmTreeTable.addContainerProperty(2, Label.class, null);
		for (int i = 0; i < 20; i++) {
			Object o = pmTreeTable.addItem();
			boolean b = rand.nextBoolean();
			Label l = new Label(b ? "foo<br />bar" : "foobar");
			if (b)
				l.setContentMode(ContentMode.HTML);
			pmTreeTable.getItem(o).getItemProperty(2).setValue(l);
		}
		pmTreeTable.setHeight(400, Unit.PIXELS);
		pmTreeTable.setWidth(400, Unit.PIXELS);

		final TextField stringField = new TextField();
		stringField.setInputPrompt("New String field");
		stringField.setNullRepresentation("");

		final TextField lableField = new TextField();
		lableField.setInputPrompt("New Label field");
		lableField.setNullRepresentation("");

		Button b = new Button("Add", new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				Object id = pmTreeTable.addItem();
				Item item = pmTreeTable.getItem(id);
				item.getItemProperty(1).setValue(stringField.getValue());
				item.getItemProperty(2).setValue(new Label(lableField.getValue()));
				stringField.setValue(null);
				lableField.setValue(null);
				pmTreeTable.setItemChanged(id);
			}
		});
		HorizontalLayout addLine = new HorizontalLayout(stringField, lableField, b);

		Label title = new Label("PMTreeTable Demo");
		title.setStyleName(Reindeer.LABEL_H1);
		Label description = new Label("Try Right-Click and Drag'n'Drop");

		final VerticalLayout layout = new VerticalLayout(title, description, pmTreeTable, addLine);
		layout.setWidth(100, Unit.PERCENTAGE);
		layout.addComponent(pmTreeTable);
		setContent(layout);
	}

	@Override
	public Action[] getActions(Object target, Object sender) {
		return actions;
	}

	@Override
	public void handleAction(Action action, Object sender, Object target) {
		if (actionRemove.equals(action)) {
			pmTreeTable.removeItem(target);
		} else if (actionChange.equals(action)) {
			pmTreeTable.getItem(target).getItemProperty(1).setValue(Integer.toString(rand.nextInt(99999)));
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
	}

	@Override
	public AcceptCriterion getAcceptCriterion() {
		return SourceIsTarget.get();
	}
}
