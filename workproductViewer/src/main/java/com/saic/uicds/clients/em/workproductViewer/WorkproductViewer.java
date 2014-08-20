package com.saic.uicds.clients.em.workproductViewer;
 
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
//import java.awt.event.MouseEvent;
//import java.awt.Color;
import java.awt.Event;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.awt.Dimension;

import javax.swing.SwingConstants;
//import javax.swing.SwingUtilities;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import java.awt.Point;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JMenuItem;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JTable;
//import javax.swing.JTextArea;
import javax.swing.JTextField;
//import javax.swing.UIManager;

//import java.awt.Dimension;
import java.io.File;

import javax.swing.JList;
import javax.swing.JScrollPane;
//import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
//import javax.swing.event.TableModelEvent;
//import javax.swing.event.TableModelListener;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.saic.uicds.clients.em.workproductViewer.WorkproductAccess;

public class WorkproductViewer implements ActionListener {

	private JFrame jFrame = null;
	private JMenuBar jJMenuBar = null;
	private JMenu fileMenu = null;
	private JMenu editMenu = null;
	private JMenu helpMenu = null;
	private JMenuItem exitMenuItem = null;
	private JMenuItem aboutMenuItem = null;
	private JMenuItem cutMenuItem = null;
	private JMenuItem copyMenuItem = null;
	private JMenuItem pasteMenuItem = null;
	private JMenuItem saveMenuItem = null;
	private JDialog aboutDialog = null;
	private JPanel aboutContentPane = null;
	private JLabel aboutVersionLabel = null;
	
	private JList sampleJList;
	private JTextField valueField;
	private JTextField fileNameValueField;
	private JFileChooser fc;
	private JButton openButton;
	private JButton openWPButton;
	private JButton refreshButton;
	private JButton callSvcButton;
	private	JTable table;
	private File file;
	private static WorkproductAccess wpa;
	private String shpUploadStatus = "";  //  @jve:decl-index=0:
	String dataValues[][];  //  @jve:decl-index=0:
	private static String[] argsInternal;
	String columnNames[] = { "Incident Name","Workproduct MIME Type","Workproduct Name"};
	TableModel tableModel;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
      // Get the spring context and then the WorkproductAccess object that was configured in it
		
      //ApplicationContext context = new ClassPathXmlApplicationContext(
		//                                new String[] { "contexts/async-context.xml" });
      
	  argsInternal = args;
      ApplicationContext context = new FileSystemXmlApplicationContext(
    		  new String[] { "async-context.xml" });
      
      //WorkproductAccess wpa = (WorkproductAccess) context.getBean("workproductAccess");
      wpa = (WorkproductAccess) context.getBean("workproductAccessBean");
	  if (wpa == null) {
        System.err.println("Could not instantiate workproductAccess");
	  }
	  wpa.init(argsInternal);
	  WorkproductViewer application = new WorkproductViewer();
	  application.getJFrame().setVisible(true);
	}

	/**
	 * This method initializes jFrame
	 * 
	 * @return javax.swing.JFrame
	 */
	private JFrame getJFrame() {		
		if (jFrame == null) {
			jFrame = new JFrame();
			jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			jFrame.setJMenuBar(getJJMenuBar());
			jFrame.setSize(1050, 800);
			jFrame.setTitle("MIME Workproduct Viewer");
			
		    Font displayFont = new Font("Times New Roman",Font.PLAIN, 14);
			Container content=jFrame.getContentPane();
			
			//call getIncidentList on core		
			dataValues = wpa.getListOfIncidents();

			tableModel = new DefaultTableModel(dataValues, columnNames);
			table = new JTable (tableModel);
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.getSelectionModel().addListSelectionListener(new RowListener());
			table.setShowGrid(false);
			Dimension d = table.getPreferredSize();
			d.setSize(950,650);
			// Make changes to d if you like...
			table.setPreferredScrollableViewportSize(d);
			
			JScrollPane listPane = new JScrollPane(table);
		    JPanel listPanel = new JPanel();
		    Border listPanelBorder =
		      BorderFactory.createTitledBorder("UICDS Core Incident List");
		    listPanel.setBorder(listPanelBorder);
		    listPanel.add(listPane);
		    content.add(listPanel, BorderLayout.CENTER);
		  
		    JPanel valuePanel = new JPanel();
		    openWPButton = new JButton("Open");
		    openWPButton.addActionListener(this);
		    valuePanel.add(openWPButton);
		    
		    refreshButton = new JButton("Refresh");
		    refreshButton.addActionListener(this);
		    valuePanel.add(refreshButton);
		    content.add(valuePanel, BorderLayout.SOUTH);
		    
		    valueField = new JTextField("", 7);
		    
		    jFrame.setVisible(true);
		}
		return jFrame;
	}

	/**
	 * This method initializes jJMenuBar	
	 * 	
	 * @return javax.swing.JMenuBar	
	 */
	private JMenuBar getJJMenuBar() {
		if (jJMenuBar == null) {
			jJMenuBar = new JMenuBar();
			jJMenuBar.add(getFileMenu());
			//jJMenuBar.add(getEditMenu());
			jJMenuBar.add(getHelpMenu());
		}
		return jJMenuBar; 
	}

	/**
	 * This method initializes jMenu	
	 * 	
	 * @return javax.swing.JMenu	
	 */
	private JMenu getFileMenu() {
		if (fileMenu == null) {
			fileMenu = new JMenu();
			fileMenu.setText("File");
			//fileMenu.add(getSaveMenuItem());
			fileMenu.add(getExitMenuItem());
		}
		return fileMenu;
	}

	/**
	 * This method initializes jMenu	
	 * 	
	 * @return javax.swing.JMenu	
	 */
	private JMenu getEditMenu() {
		if (editMenu == null) {
			editMenu = new JMenu();
			editMenu.setText("Edit");
			editMenu.add(getCutMenuItem());
			editMenu.add(getCopyMenuItem());
			editMenu.add(getPasteMenuItem());
		}
		return editMenu;
	}

	/**
	 * This method initializes jMenu	
	 * 	
	 * @return javax.swing.JMenu	
	 */
	private JMenu getHelpMenu() {
		if (helpMenu == null) {
			helpMenu = new JMenu();
			helpMenu.setText("Help");
			helpMenu.add(getAboutMenuItem());
		}
		return helpMenu;
	}

	/**
	 * This method initializes jMenuItem	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getExitMenuItem() {
		if (exitMenuItem == null) {
			exitMenuItem = new JMenuItem();
			exitMenuItem.setText("Exit");
			exitMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					System.exit(0);
				}
			});
		}
		return exitMenuItem;
	}

	/**
	 * This method initializes jMenuItem	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getAboutMenuItem() {
		if (aboutMenuItem == null) {
			aboutMenuItem = new JMenuItem();
			aboutMenuItem.setText("About");
			aboutMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JDialog aboutDialog = getAboutDialog();
					aboutDialog.pack();
					Point loc = getJFrame().getLocation();
					loc.translate(20, 20);
					aboutDialog.setLocation(loc);
					aboutDialog.setVisible(true);
				}
			});
		}
		return aboutMenuItem;
	}

	/**
	 * This method initializes aboutDialog	
	 * 	
	 * @return javax.swing.JDialog
	 */
	private JDialog getAboutDialog() {
		if (aboutDialog == null) {
			aboutDialog = new JDialog(getJFrame(), true);
			aboutDialog.setTitle("About");
			aboutDialog.setContentPane(getAboutContentPane());
		}
		return aboutDialog;
	}

	/**
	 * This method initializes aboutContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getAboutContentPane() {
		if (aboutContentPane == null) {
			aboutContentPane = new JPanel();
			aboutContentPane.setLayout(new BorderLayout());
			aboutContentPane.add(getAboutVersionLabel(), BorderLayout.CENTER);
		}
		return aboutContentPane;
	}

	/**
	 * This method initializes aboutVersionLabel	
	 * 	
	 * @return javax.swing.JLabel	
	 */
	private JLabel getAboutVersionLabel() {
		if (aboutVersionLabel == null) {
			aboutVersionLabel = new JLabel();
			aboutVersionLabel.setText("UICDS Workproduct Viewer Version 1.0");
			aboutVersionLabel.setHorizontalAlignment(SwingConstants.CENTER);
		}
		return aboutVersionLabel;
	}

	/**
	 * This method initializes jMenuItem	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getCutMenuItem() {
		if (cutMenuItem == null) {
			cutMenuItem = new JMenuItem();
			cutMenuItem.setText("Cut");
			cutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,
					Event.CTRL_MASK, true));
		}
		return cutMenuItem;
	}

	/**
	 * This method initializes jMenuItem	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getCopyMenuItem() {
		if (copyMenuItem == null) {
			copyMenuItem = new JMenuItem();
			copyMenuItem.setText("Copy");
			copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
					Event.CTRL_MASK, true));
		}
		return copyMenuItem;
	}

	/**
	 * This method initializes jMenuItem	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getPasteMenuItem() {
		if (pasteMenuItem == null) {
			pasteMenuItem = new JMenuItem();
			pasteMenuItem.setText("Paste");
			pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V,
					Event.CTRL_MASK, true));
		}
		return pasteMenuItem;
	}

	/**
	 * This method initializes jMenuItem	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getSaveMenuItem() {
		if (saveMenuItem == null) {
			saveMenuItem = new JMenuItem();
			saveMenuItem.setText("Save");
			saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
					Event.CTRL_MASK, true));
		}
		return saveMenuItem;
	}
	
	private void loadJFileChooser() {
		fc = new JFileChooser();
	}
	
	  public void actionPerformed(ActionEvent e) {

		    //Handle open button action.
		    if (e.getSource() == openButton) {
		      int returnVal = fc.showOpenDialog(jFrame);
		      if (returnVal == JFileChooser.APPROVE_OPTION) {
		        file = fc.getSelectedFile();
		        //This is where a real application would open the file.
		        fileNameValueField.setText(file.getName());
		        //fileNameValueField.append("Opening: " + file.getName() + "." + newline);
		      } else {
		    	fileNameValueField.setText("");
		        //log.append("Open command cancelled by user." + newline);
		      }
		      //log.setCaretPosition(log.getDocument().getLength());
		      
		    } else if (e.getSource() == refreshButton) {
		    	wpa.init(argsInternal);
				dataValues = wpa.getListOfIncidents();
				((DefaultTableModel)tableModel).setDataVector(dataValues,columnNames);
				((DefaultTableModel)tableModel).fireTableDataChanged();

		    } else if (e.getSource() == openWPButton) {
		    	String message="Do you want to open the work product: "+valueField.getText()+"?";
		    	if (valueField.getText().length()>4){ 
		    		int response = JOptionPane.showConfirmDialog(null, message, "Confirm",
		    		        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		    		if (response == JOptionPane.NO_OPTION) {
		    		    	//JOptionPane.showMessageDialog(jFrame, "User aborted Open.");
		    		} else if (response == JOptionPane.YES_OPTION) {
		    		    	//JOptionPane.showMessageDialog(jFrame, "Processing WP:"+valueField.getText());
		    		    	wpa.openTheWorkproduct(valueField.getText());
		    		} else if (response == JOptionPane.CLOSED_OPTION) {
		    		    	//JOptionPane.showMessageDialog(jFrame, "JPane closed.");
		    		}
		    	} else {
		    		JOptionPane.showMessageDialog(jFrame, "No WP was selected.");
		    	}
		    			    
	        } else if (e.getSource() == callSvcButton) {
		    	if (fileNameValueField.getText().length()>4 && valueField.getText().length()>4){
		    		shpUploadStatus = wpa.assocShpToIncidentUsingDOM(valueField.getText(), file);
		    		if (shpUploadStatus.equalsIgnoreCase("Accepted"))
		    			JOptionPane.showMessageDialog(jFrame, "Shapefile was uploaded.");
		    		else
		    			JOptionPane.showMessageDialog(jFrame, "There was a problem with the Shapefile. File not uploaded.");
		    	  //} else if (answer == JOptionPane.NO_OPTION) {
		    	      // User clicked NO.
		    	  //}
		    	} else {
		    	  JOptionPane.showMessageDialog(jFrame, "Please select a .shp file and a UICDS Incident.");
		    	}
		    }
		  }

	  private class ValueReporter implements ListSelectionListener {
		    public void valueChanged(ListSelectionEvent event) {
		      if (!event.getValueIsAdjusting()) 
		        valueField.setText(sampleJList.getSelectedValue().toString());
		    }
	  }	
	  
	    private class RowListener implements ListSelectionListener {
	        public void valueChanged(ListSelectionEvent event) {
	            if (event.getValueIsAdjusting()) {
	                return;
	            }
	            System.out.println("Selected Row="+table.getSelectedRow());
	            System.out.println("selected cell="+(String) table.getModel().getValueAt(table.getSelectedRow(), 2));
	            valueField.setText((String) table.getModel().getValueAt(table.getSelectedRow(), 2));
	        
	        }
	    }
}
