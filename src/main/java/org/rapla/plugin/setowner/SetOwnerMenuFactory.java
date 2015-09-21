package org.rapla.plugin.setowner;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.rapla.RaplaResources;
import org.rapla.entities.Entity;
import org.rapla.entities.Named;
import org.rapla.entities.NamedComparator;
import org.rapla.entities.Ownable;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.AppointmentBlock;
import org.rapla.entities.domain.Reservation;
import org.rapla.facade.ClientFacade;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.MenuContext;
import org.rapla.gui.ObjectMenuFactory;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.TreeFactory;
import org.rapla.gui.internal.SwingPopupContext;
import org.rapla.gui.toolkit.DialogUI;
import org.rapla.gui.toolkit.RaplaMenuItem;
import org.rapla.gui.toolkit.RaplaTree;

public class SetOwnerMenuFactory implements ObjectMenuFactory
{
    SetOwnerResources setOwnerI18n;
    RaplaResources i18n;
    ClientFacade facade;
    TreeFactory treeFactory;
    RaplaGUIComponent old;
    @Inject
    public SetOwnerMenuFactory( SetOwnerResources isetOwnerI18n, RaplaResources i18n, ClientFacade facade, TreeFactory treeFactory, RaplaContext context)
    {
        this.setOwnerI18n = setOwnerI18n;
        old = new RaplaGUIComponent(context);
        this.i18n = i18n;
        this.facade = facade;
        this.treeFactory = treeFactory;
    }

    public RaplaMenuItem[] create( final MenuContext menuContext, final RaplaObject focusedObject )
    {
    	if (!old.isAdmin())
    	{
    		return RaplaMenuItem.EMPTY_ARRAY;
    	}
    	
    	Collection<Object> selectedObjects = new HashSet<Object>();
    	Collection<?> selected = menuContext.getSelectedObjects();
    	if ( selected.size() != 0)
    	{
    		selectedObjects.addAll( selected);
    	}
    	if ( focusedObject != null)
    	{
    		selectedObjects.add( focusedObject);
    	}
    		
    	final Collection<Entity<? extends Entity>> ownables = new HashSet<Entity<? extends Entity>>();
    	for ( Object obj: selectedObjects)
    	{
    		final Entity<? extends Entity> ownable;
    		if  ( obj instanceof AppointmentBlock)
    		{
    			ownable = ((AppointmentBlock) obj).getAppointment().getReservation(); 
    		}
    		else if ( obj instanceof Entity )
    		{
    			RaplaType raplaType = ((RaplaObject)obj).getRaplaType();
    	    	if ( raplaType == Appointment.TYPE )
    	        {
    	    		Appointment appointment = (Appointment) obj;
    	    	    		ownable = appointment.getReservation();
    	        }
    	    	else if ( raplaType ==  Reservation.TYPE)
    	    	{
    	    		ownable = (Reservation) obj;
    	    	}
    	    	else if ( raplaType ==  Allocatable.TYPE)
    	    	{
    	    		ownable = (Allocatable) obj;
    	    	}
    	    	else
    	    	{
    	    		ownable = null;
    	    	}
    		}
    		else
    		{
    			ownable  = null;
    		}
    		if ( ownable != null)
    		{
    			ownables.add( ownable);
    		}
    	}
    	
    	if ( ownables.size() == 0 )
    	{
    		return RaplaMenuItem.EMPTY_ARRAY;
    	}
        
        // create the menu entry
        final RaplaMenuItem setOwnerItem = new RaplaMenuItem("SETOWNER");
        setOwnerItem.setText(setOwnerI18n.getString("changeowner"));
        ImageIcon icon = old.getImages().getIconFromKey( i18n.getString("icon.tree.persons"));
        setOwnerItem.setIcon(icon);
        setOwnerItem.addActionListener( new ActionListener()
        {
            public void actionPerformed( ActionEvent e )
            {
                try 
                {
                	User newOwner = showAddDialog();
                	if(newOwner != null) {
                		ArrayList<Entity<?>> toStore = new ArrayList<Entity<?>>();
                		for ( Entity<? extends Entity> ownable: ownables)
                		{
                			Entity<?> editableOwnables = facade.edit( ownable);
	                		Ownable casted = (Ownable)editableOwnables;
							casted.setOwner(newOwner);
	                		toStore.add( editableOwnables);
                		}
	                		//((SimpleEntity) editableEvent).setLastChangedBy(newOwner);
                		facade.storeObjects(toStore.toArray(Entity.ENTITY_ARRAY));
                	}
                }
                catch (RaplaException ex )
                {
                    old.showException( ex, SwingPopupContext.extractParent(menuContext.getPopupContext()));
                } 
            }
         });

        return new RaplaMenuItem[] {setOwnerItem };
    }
    
    
    final private TreeFactory getTreeFactory() {
        return  treeFactory;
    }
    
    private User showAddDialog() throws RaplaException {
        final DialogUI dialog;
        RaplaTree treeSelection = new RaplaTree();
        treeSelection.setMultiSelect(true);
        treeSelection.getTree().setCellRenderer(getTreeFactory().createRenderer());
        
        DefaultMutableTreeNode userRoot = new DefaultMutableTreeNode("ROOT");
        //DefaultMutableTreeNode userRoot = TypeNode(User.TYPE, getString("users"));
        User[] userList = facade.getUsers();
        for (final User user: sorted(userList)) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode();
            node.setUserObject( user );
            userRoot.add(node); 
        }
        
        treeSelection.exchangeTreeModel(new DefaultTreeModel(userRoot));
        treeSelection.setMinimumSize(new java.awt.Dimension(300, 200));
        treeSelection.setPreferredSize(new java.awt.Dimension(400, 260));
        dialog = DialogUI.create(
                old.getContext()
                ,old.getMainComponent()
                ,true
                ,treeSelection
                ,new String[] { i18n.getString("apply"),i18n.getString("cancel")});
        dialog.setTitle(setOwnerI18n.getString("changeownerto"));
        dialog.getButton(0).setEnabled(false);
        
        final JTree tree = treeSelection.getTree(); 
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addMouseListener(new MouseAdapter() { 
            // End dialog when a leaf is double clicked
            public void mousePressed(MouseEvent e) {
                TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                if (selPath != null && e.getClickCount() == 2) {
                    final Object lastPathComponent = selPath.getLastPathComponent();
                    if (((TreeNode) lastPathComponent).isLeaf() ) {
                        dialog.getButton(0).doClick();
                        return;
                    }
                }
                else
                	if (selPath != null && e.getClickCount() == 1) {
                        final Object lastPathComponent = selPath.getLastPathComponent();
                        if (((TreeNode) lastPathComponent).isLeaf() ) {
                            dialog.getButton(0).setEnabled(true);
                            return;
                        }
                	}
                tree.removeSelectionPath(selPath);
            }
        });
        
        dialog.start(); 
        if (dialog.getSelectedIndex() != 0) {
        	return null;
        }
		return (User) treeSelection.getSelectedElement();
    }
    
    private <T extends Named> Collection<T> sorted(T[] allocatables) {
        TreeSet<T> sortedList = new TreeSet<T>(new NamedComparator<T>(old.getLocale()));
        sortedList.addAll(Arrays.asList(allocatables));
        return sortedList;
    }
    
}
