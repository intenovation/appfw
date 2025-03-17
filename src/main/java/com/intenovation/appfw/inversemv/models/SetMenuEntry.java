package com.intenovation.appfw.inversemv.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.intenovation.appfw.inversemv.CheckboxModel;
import com.intenovation.appfw.inversemv.Model;
import com.intenovation.appfw.inversemv.ParentModel;
import com.intenovation.appfw.inversemv.ParentView;
import com.intenovation.appfw.inversemv.View;

public class SetMenuEntry<T> implements ParentModel, Set<T> {

	@Override
	public String toString() {
		StringBuffer erg = new StringBuffer();
		for (T entry : enumvalues) {
			erg.append(",");
			erg.append(entry.toString());

		}
		if (erg.length() > 1)
			return erg.toString().substring(1);
		else
			return erg.toString();
	}

	private List<BasicCheckboxMenuEntry> children;
	private String displayName;
	private Set<T> enumvalues;

	private static Logger log = Logger.getLogger(SetMenuEntry.class.getName());

	public SetMenuEntry(String displayName) {
		super();
		this.displayName = displayName;

		enumvalues = new HashSet<T>();
	}

	public String displayName(T entry) {
		return entry.toString().replace("_", ".");
	}

	private List getChildren() {
		if (children == null) {
			children = new ArrayList<BasicCheckboxMenuEntry>();
			for (T entry : enumvalues) {
				addToView(displayName(entry));

			}

		}
		return children;
	}

	public boolean add(T entry) {
		boolean adderg = enumvalues.add(entry);
		String displayNameEntry = displayName(entry);
		log.info("Adding " + entry + " " + displayNameEntry + " " + adderg);
		if (adderg && view != null) {
			addToView(displayNameEntry);
		}

		return adderg;
	}

	private void addToView(String entry) {
		BasicCheckboxMenuEntry basicCheckboxMenuEntry = new BasicCheckboxMenuEntry(
				entry);
		children.add(basicCheckboxMenuEntry);
		view.addChild(basicCheckboxMenuEntry);
	}

	private String getDisplayName() {

		return displayName;
	}

	public Set<T> getEnumvalues() {
		return enumvalues;
	}

	public boolean isLongRunningInit() {

		return false;
	}

	public void run() {

	}

	public void childHasChanged(Model o) {
		log.info("update" + o);
		BasicCheckboxMenuEntry checkbox = (BasicCheckboxMenuEntry) o;

		// view.notifyMyParent();

	}

	public void stop() {
	}

	private ParentView view;

	public void setView(ParentView view) {
		this.view = view;
		view.setName(getDisplayName());
		getChildren();
	}

	@Override
	public View getView() {

		return view;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return enumvalues.size();
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return enumvalues.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		// TODO Auto-generated method stub
		return enumvalues.contains(o);
	}

	@Override
	public Iterator<T> iterator() {
		// TODO Auto-generated method stub
		return enumvalues.iterator();
	}

	@Override
	public Object[] toArray() {
		// TODO Auto-generated method stub
		return enumvalues.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		// TODO Auto-generated method stub
		return enumvalues.toArray(a);
	}

	@Override
	public boolean remove(Object o) {
		for (BasicCheckboxMenuEntry check : children) {
			if (check.getDisplayName().equals(displayName((T) o))) {
				view.removeChild(check);
			}
		}

		return enumvalues.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return enumvalues.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		for (T a : c) {
			add(a);

		}
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return enumvalues.retainAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		for (Object a : c)
			remove(a);
		return false;
	}

	@Override
	public void clear() {
		HashSet<T> copy = new HashSet<T>();
		copy.addAll(enumvalues);

		for (T a : copy)
			remove(a);

	}

}
