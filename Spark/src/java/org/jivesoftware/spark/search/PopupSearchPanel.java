package org.jivesoftware.spark.search;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.jivesoftware.MainWindow;
import org.jivesoftware.Spark;
import org.jivesoftware.resource.Default;
import org.jivesoftware.resource.Res;
import org.jivesoftware.spark.SparkManager;
import org.jivesoftware.spark.Workspace;
import org.jivesoftware.spark.component.JContactItemField;
import org.jivesoftware.spark.ui.ContactGroup;
import org.jivesoftware.spark.ui.ContactItem;
import org.jivesoftware.spark.ui.ContactList;
import org.jivesoftware.spark.util.ModelUtil;

public class PopupSearchPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JContactItemField contactField;

	public PopupSearchPanel() {
        setLayout(new GridBagLayout());

        final Map<String, ContactItem> contactMap = new HashMap<String, ContactItem>();
        final List<ContactItem> contacts = new ArrayList<ContactItem>();

        final ContactList contactList = SparkManager.getWorkspace().getContactList();

        for (ContactGroup contactGroup : contactList.getContactGroups()) {
            contactGroup.clearSelection();
            for (ContactItem contactItem : contactGroup.getContactItems()) {
                if (!contactMap.containsKey(contactItem.getJID())) {
                    contacts.add(contactItem);
                    contactMap.put(contactItem.getJID(), contactItem);
                }
            }
        }

        // Sort
        Collections.sort(contacts, itemComparator);
        
        contactField = new JContactItemField(new ArrayList<ContactItem>(contacts));

        boolean hide = Default.getBoolean(Default.HIDE_PERSON_SEARCH_FIELD);
        if (!hide) {
        	if (Spark.isMac()) {
        		add(contactField, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 30), 0, 0));
        	}
        	else {
        		add(contactField, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
        	}
        }
        contactField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent keyEvent) {
                if (keyEvent.getKeyChar() == KeyEvent.VK_ENTER) {
                    if (ModelUtil.hasLength(contactField.getText())) {
                        ContactItem item = contactMap.get(contactField.getText());
                        if (item == null) {
                            item = contactField.getSelectedContactItem();
                        }
                        if (item != null) {
                            contactField.dispose();
                            SparkManager.getChatManager().activateChat(item.getJID(), item.getDisplayName());
                        }
                    }
                }
                else if (keyEvent.getKeyChar() == KeyEvent.VK_ESCAPE) {
                    contactField.dispose();
                }
            }
        });
        contactField.getList().addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
        	if(SwingUtilities.isRightMouseButton(e))
        	{
        	    contactField.setSelectetIndex(e);
        	    ContactItem item = contactField.getSelectedContactItem();
        	    MouseEvent exx = new MouseEvent((Component) e.getSource(),e.getID(), e.getWhen(),e.getModifiers(),e.getX()+20, e.getY(), e.getClickCount(), false);
        	    SparkManager.getContactList().setSelectedUser(item.getJID());
        	    SparkManager.getContactList().showPopup(contactField.getPopup(),exx,item);
        	}
        	
                if (e.getClickCount() == 2) {
                    if (ModelUtil.hasLength(contactField.getText())) {
                        ContactItem item = contactMap.get(contactField.getText());
                        if (item == null) {
                            item = contactField.getSelectedContactItem();
                        }
                        if (item != null) {
                            contactField.dispose();
                            SparkManager.getChatManager().activateChat(item.getJID(), item.getDisplayName());
                        }
                    }
                }
            }
        });
        contactField.getTextField().setToolTipText(Res.getString("message.search.for.contacts"));
        contactField.getTextField().setForeground((Color) UIManager.get("TextField.lightforeground"));
		contactField.getTextField().setText(Res.getString("self.searchText"));
        contactField.getTextField().addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				contactField.dispose();
				contactField.getTextField().setForeground((Color) UIManager.get("TextField.lightforeground"));
				contactField.getTextField().setText(Res.getString("self.searchText"));
			}
			@Override
			public void focusGained(FocusEvent e) {
				contactField.getTextField().setText("");
			}
		});
        Workspace workspace = SparkManager.getWorkspace();
        //FIXME modify 10 to 1 will change the search to the top  markedByLsq
        workspace.add(this, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        workspace.invalidate();
        workspace.validate();
        workspace.repaint();
        final MainWindow mainWindow = SparkManager.getMainWindow();
        mainWindow.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				contactField.grabFocus();//lose focus
				contactField.dispose();//prevent move the window and the contactlist will stay the same place			
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				contactField.grabFocus();
				contactField.dispose();
			}
		});
	}
	
    /**
     * Sorts ContactItems.
     */
    final Comparator<ContactItem> itemComparator = new Comparator<ContactItem>() {
        public int compare(ContactItem item1, ContactItem item2) {
            return item1.getDisplayName().toLowerCase().compareTo(item2.getDisplayName().toLowerCase());
        }
    };
}
