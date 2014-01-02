package org.sparcs.gpgchat.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.sparcs.gpgchat.message.MessageInterface;
import org.sparcs.gpgchat.message.MessageReceiver;
import org.sparcs.gpgchat.message.irc.IRCInterface;

public class SelectNetwork extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private final JPanel titlePanel = new JPanel();
	
	private String server;
	private String port;
	private String channel;
	private JTextField serverText;
	private JTextField portText;
	private JTextField channelText;

	/**
	 * Create the dialog.
	 */
	public SelectNetwork(String defaultServer, String defaultPort, String defaultChannel) {
		this.server = defaultServer;
		this.port = defaultPort;
		this.channel = defaultChannel;
		setBounds(100, 100, 400, 400);
		getContentPane().setLayout(new BorderLayout());
		titlePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		titlePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		JTextField text = new JTextField("IRC server");
		text.setEditable(false);
		text.setColumns(10);
		titlePanel.add(text);
		text = new JTextField("IRC port");
		text.setEditable(false);
		text.setColumns(10);
		titlePanel.add(text);
		text = new JTextField("IRC channel");
		text.setEditable(false);
		text.setColumns(10);
		titlePanel.add(text);
		
		getContentPane().add(titlePanel, BorderLayout.NORTH);
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 5, 5));
		serverText = new JTextField(defaultServer);
		serverText.setColumns(10);
		portText = new JTextField(defaultPort);
		portText.setColumns(10);
		channelText = new JTextField(defaultChannel);
		channelText.setColumns(10);
		
		contentPanel.add(serverText);
		contentPanel.add(portText);
		contentPanel.add(channelText);
		
		
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);
		final SelectNetwork me = this;
		{
			JButton okButton = new JButton("Active");
			okButton.setActionCommand("Active");
			okButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent arg0) {
					me.server = serverText.getText();
					me.port = portText.getText();
					me.channel = channelText.getText();
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
					me.dispose();
				}
			});
			buttonPane.add(cancelButton);
		}
	}
	
	public MessageInterface getInterface(MessageReceiver listener)
	{
		return IRCInterface.getInstance(this.server, Integer.parseInt(this.port), this.channel, null, listener);
	}
}
