package org.sparcs.gpgchat.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.JList;

import org.sparcs.gpgchat.gpg.GPG;
import org.sparcs.gpgchat.gpg.Key;

public class SelectCertificate extends JDialog {

	private final JPanel contentPanel = new JPanel();
	private final JPanel titlePanel = new JPanel();
	private List<Key> secretKeys;
	private List<Key> publicKeys;
	private boolean confirm = false;
	private JList<String> publicList;
	private JList<String> privateList;
	
	private Object lock = new Object();

	/**
	 * Create the dialog.
	 */
	public SelectCertificate(final GPG gpg) {
		setBounds(100, 100, 400, 400);
		getContentPane().setLayout(new BorderLayout());
		titlePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		titlePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		JTextField text = new JTextField("Private key");
		text.setEditable(false);
		titlePanel.add(text);
		text = new JTextField("Public key");
		text.setEditable(false);
		titlePanel.add(text);
		
		getContentPane().add(titlePanel, BorderLayout.NORTH);
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		{
			secretKeys = gpg.getSecretKeys();
			String[] data = new String[secretKeys.size()];
			for(int k=0; k<secretKeys.size(); k++)
			{
				Key key = secretKeys.get(k);
				data[k] = "" +  key.uid + " <" + key.email + "> " + key.keyID;
			}
			
			privateList = new JList<>(data);
			privateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			JScrollPane pane = new JScrollPane(privateList);
			pane.setPreferredSize(new Dimension(150, 250));
			contentPanel.add(pane);
		}
		{
			publicKeys = gpg.getAllKeys();
			String[] data = new String[publicKeys.size()];
			for(int k=0; k<publicKeys.size(); k++)
			{
				Key key = publicKeys.get(k);
				data[k] = "" +  key.uid + " <" + key.email + "> " + key.keyID;
			}
			
			publicList = new JList<>(data);
			publicList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			JScrollPane pane = new JScrollPane(publicList);
			pane.setPreferredSize(new Dimension(150, 250));
			contentPanel.add(pane);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			final SelectCertificate me = this;
			{
				JButton okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				okButton.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent arg0) {
						me.confirm = true;
						Key privateKey = secretKeys.get(me.privateList.getSelectedIndex());
						gpg.setDefaultKey(privateKey);
						
						List<Key> trusts = new LinkedList<>();
						for(int idx : me.publicList.getSelectedIndices())
						{
							trusts.add(publicKeys.get(idx));
						}
						gpg.setTrustedKey(trusts);
						me.dispose();
					}
				});
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setActionCommand("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						me.confirm = false;
						me.dispose();
					}
				});
				buttonPane.add(cancelButton);
			}
			
			this.addWindowStateListener(new WindowStateListener() {
				
				@Override
				public void windowStateChanged(WindowEvent arg0) {
					if(arg0.getNewState() == WindowEvent.WINDOW_CLOSED)
					{
						synchronized(me.lock)
						{
							me.lock.notifyAll();
						}
					}
						
				}
			});
		}
	}
	 

	public boolean waitResult() throws InterruptedException
	{
		synchronized(lock)
		{
			lock.wait();
		}
		return confirm;
	}
}
