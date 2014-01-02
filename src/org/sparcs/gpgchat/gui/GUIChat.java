package org.sparcs.gpgchat.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.sparcs.gpgchat.gpg.GPG;
import org.sparcs.gpgchat.message.MessageReceiver;

public class GUIChat implements MessageReceiver{

	private JFrame frame;
	private GPG gpg;
	private SelectNetwork networkDialog;
	
	private JList<String> showList;
	private DefaultListModel<String> showData;
	private JTextField enterArea;
	private int lastMax = 0;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUIChat window = new GUIChat();
					window.gpg = GPG.getInstance(null);
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public GUIChat() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		final GUIChat me = this;
		frame = new JFrame();
		frame.setBounds(100, 100, 800, 600);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		networkDialog = new SelectNetwork("irc.ozinger.org", "6661", "#ella");
		
		JMenuItem certificateMenu = new JMenuItem("Certificate");
		certificateMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				SelectCertificate dialog = new SelectCertificate(gpg);
				dialog.setVisible(true);
				dialog.setModal(true);
			}
		});
		menuBar.add(certificateMenu);
		
		JMenuItem networkMenu = new JMenuItem("Network");
		networkMenu.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				networkDialog.setVisible(true);
				networkDialog.setModal(true);
			}
		});
		menuBar.add(networkMenu);
		
		
		showData = new DefaultListModel<>();
		showList = new JList<>(showData);
		showList.setAutoscrolls(true);
		JScrollPane scrollPane = new JScrollPane(showList);
		scrollPane.setPreferredSize(new Dimension(700, 500));
		scrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {  
		    public void adjustmentValueChanged(AdjustmentEvent e) {  
		    	int curMax = e.getAdjustable().getMaximum();
		    	if(curMax > me.lastMax)
		    	{
		    		me.lastMax = curMax;
		    		e.getAdjustable().setValue(curMax);
		    	}
		    }
		});
		
		frame.getContentPane().add(scrollPane, BorderLayout.NORTH);
		
		JPanel enter = new JPanel();
		enterArea = new JTextField(50);
		enterArea.addKeyListener(new KeyListener() {
			
			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub
				if(e.getKeyChar() == KeyEvent.VK_ENTER)
				{
					e.consume();
					String text = me.enterArea.getText();
					me.enterArea.setText("");
					me.receiveMessage(gpg.getDefaultKey().uid + ": " + text);
				}
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		JButton enterButton = new JButton("Enter");
		enterButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String text = me.enterArea.getText();
				me.enterArea.setText("");
				me.receiveMessage(gpg.getDefaultKey().uid + ": " + text);
			}
		});
		enter.add(enterArea);
		enter.add(enterButton);
		frame.getContentPane().add(enter, BorderLayout.SOUTH);
	}

	@Override
	public void receiveMessage(String message) {
		showData.addElement(message);
	}
}
