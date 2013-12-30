package org.sparcs.gpgchat.gui;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPopupMenu;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.sparcs.gpgchat.gpg.GPG;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class GUIChat {

	private JFrame frame;
	private GPG gpg;

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
		frame = new JFrame();
		frame.setBounds(100, 100, 800, 600);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
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
		menuBar.add(networkMenu);
	}
}
