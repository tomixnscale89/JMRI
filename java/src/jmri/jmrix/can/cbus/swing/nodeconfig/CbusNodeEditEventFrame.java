package jmri.jmrix.can.cbus.swing.nodeconfig;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.text.DefaultFormatter;
import jmri.jmrix.can.CanSystemConnectionMemo;
import jmri.jmrix.can.cbus.node.CbusNode;
import jmri.jmrix.can.cbus.node.CbusNodeEvent;
import jmri.jmrix.can.cbus.node.CbusNodeTableDataModel;
import jmri.jmrix.can.cbus.node.CbusNodeSingleEventTableDataModel;
import jmri.jmrix.can.cbus.CbusNameService;
import jmri.util.JmriJFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frame to control an instance of CBUS highlighter to highlight events.
 *
 * @author Steve Young Copyright (C) 2019
 */
public class CbusNodeEditEventFrame extends JmriJFrame implements TableModelListener {
   
    private CbusNodeSingleEventTableDataModel singleEVModel;
    private final CbusNodeTableDataModel nodeModel;
    
    private JSpinner numberSpinnerEv;
    private JSpinner numberSpinnernd;

    private JButton framedeletebutton;
    private JButton frameeditevbutton;
    private JButton frameCopyButton;
    private JTabbedPane tabbedPane;
    private CbusNodeEvent _ndEv;
    private JLabel ndEvNameLabel;
    private final NodeConfigToolPane mainpane;
    private jmri.util.swing.BusyDialog busy_dialog;
    
    private CanSystemConnectionMemo _memo;
    
    private JPanel infoPane;
    private JButton framenewevbutton;
    private JButton frameResetButton;
    private Boolean isNewEvent;
    
    /**
     * Create a new instance of CbusNodeEditEventFrame.
     * @param tp The main NodeConfigToolPane this is a part of
     */
    public CbusNodeEditEventFrame(NodeConfigToolPane tp) {
        super();
        mainpane = tp;
        nodeModel = mainpane.getNodeModel();
    }

    public void initComponents(CanSystemConnectionMemo memo, CbusNodeEvent ndEv) {
        _ndEv = ndEv;
        _memo = memo;
        singleEVModel = new CbusNodeSingleEventTableDataModel(memo, 5,
        CbusNodeSingleEventTableDataModel.MAX_COLUMN, _ndEv); // controller, row, column
        singleEVModel.addTableModelListener(this);
        
        screenInit();
    }
    
    private void screenInit() {
        
        if (infoPane != null ){ 
            infoPane.setVisible(false);
            infoPane = null;
        }
        
        if ( _ndEv == null ){
            return;
        }
        
        CbusNode _node = nodeModel.getNodeByNodeNum( _ndEv.getParentNn() );
        isNewEvent = _node!=null &&
            _node.getNodeEvent(_ndEv.getNn(), _ndEv.getEn()) == null;
        log.debug("isNewEvent {}", isNewEvent);

        frameeditevbutton = new JButton(Bundle.getMessage("EditEvent"));
        framenewevbutton = new JButton(Bundle.getMessage("NewEvent"));
        framedeletebutton = new JButton(Bundle.getMessage("ButtonDelete"));
        frameResetButton = new JButton(("Reset New Values"));
        frameCopyButton = new JButton(("Copy Event"));
        
        numberSpinnernd = new JSpinner(new SpinnerNumberModel(Math.max(0,_ndEv.getNn()), 0, 65535, 1));
        JComponent compNd = numberSpinnernd.getEditor();
        JFormattedTextField fieldNd = (JFormattedTextField) compNd.getComponent(0);
        DefaultFormatter formatterNd = (DefaultFormatter) fieldNd.getFormatter();
        formatterNd.setCommitsOnValidEdit(true);

        numberSpinnerEv = new JSpinner(new SpinnerNumberModel(Math.max(0,_ndEv.getEn()), 0, 65535, 1));
        JComponent compEv = numberSpinnerEv.getEditor();
        JFormattedTextField fieldEv = (JFormattedTextField) compEv.getComponent(0);
        DefaultFormatter formatterEv = (DefaultFormatter) fieldEv.getFormatter();
        formatterEv.setCommitsOnValidEdit(true);        
        
        infoPane = new JPanel();
        infoPane.setLayout(new BorderLayout() );        
        
        JPanel pageStartPanel = new JPanel();
        pageStartPanel.setLayout(new BoxLayout(pageStartPanel, BoxLayout.Y_AXIS));
        
        JPanel buttonPanel = new JPanel();
        
        buttonPanel.add(frameCopyButton);
        buttonPanel.add(frameResetButton);
        buttonPanel.add(framedeletebutton);
        
        JPanel spinnerPanel= new JPanel(); // row container

        spinnerPanel.add(new JLabel(Bundle.getMessage("CbusNode"), JLabel.RIGHT));
        spinnerPanel.add(numberSpinnernd);
        spinnerPanel.add(new JLabel(Bundle.getMessage("CbusEvent"), JLabel.RIGHT));
        spinnerPanel.add(numberSpinnerEv);
        
        spinnerPanel.add(framenewevbutton);
        spinnerPanel.add(frameeditevbutton);
        
        JPanel evNamePanel= new JPanel(); // row container
        ndEvNameLabel = new JLabel("");
        evNamePanel.add(ndEvNameLabel);
        
        pageStartPanel.add(spinnerPanel);
        pageStartPanel.add(evNamePanel);
        pageStartPanel.add( buttonPanel);
        pageStartPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        
        pageStartPanel.validate();
        
        JPanel generic = new JPanel();
        
        generic.setLayout( new BorderLayout() );
        
        CbusNodeSingleEventEditTablePane genericEVTable = new CbusNodeSingleEventEditTablePane(singleEVModel);
        genericEVTable.initComponents(_memo, mainpane);
        generic.add( genericEVTable );
        
        tabbedPane = new JTabbedPane();
        
        tabbedPane.addTab(("Template"), null);
        tabbedPane.addTab(("Generic"), generic);
        
        tabbedPane.setEnabledAt(0,false);
        tabbedPane.setSelectedIndex(1);
        
        infoPane.add(pageStartPanel, BorderLayout.PAGE_START);
        infoPane.add(tabbedPane, BorderLayout.CENTER);
        
        this.add(infoPane);
        
        this.setPreferredSize(new Dimension(500, 300));
        
        this.pack();
        this.setResizable(true);
        
        this.validate();
        this.repaint();
        
        this.setVisible(true);
        this.toFront();
        
        updateButtons();
        
        frameResetButton.addActionListener((ActionEvent e) -> {
            singleEVModel.resetnewEVs();
            numberSpinnernd.setValue(Math.max(0,_ndEv.getNn() ) );
            numberSpinnerEv.setValue(Math.max(1,_ndEv.getEn() ) );
        });
        
        
        ActionListener copyEvClicked = ae -> {
            log.debug("copy button");
            isNewEvent=true;
            updateButtons();
        };        
        frameCopyButton.addActionListener(copyEvClicked);
        
        // node will check for other nodes in learn mode
        ActionListener newEvClicked = ae -> {
            busy_dialog = new jmri.util.swing.BusyDialog(this, "Teaching Node", false);
            busy_dialog.start();
         //   setEnabled(false);
            jmri.util.ThreadingUtil.runOnLayout( ()->{
                singleEVModel.passNewEvToNode(this);
            });
        };
        framenewevbutton.addActionListener(newEvClicked);
        
        ActionListener deleteClicked = ae -> {
            if (_node == null ){
                return;
            }
            int response = JOptionPane.showConfirmDialog(
                null,
                ( Bundle.getMessage("NdDelEvConfrm",
                    new CbusNameService(_memo).getEventNodeString(_ndEv.getNn(), _ndEv.getEn() ),
                    _node.getNodeNumberName() ) ),
                (Bundle.getMessage("DelEvPopTitle")), 
                JOptionPane.YES_NO_OPTION,         
                JOptionPane.ERROR_MESSAGE);
            if (response == JOptionPane.YES_OPTION) {
            
                busy_dialog = new jmri.util.swing.BusyDialog(this, "Deleting Event", false);
                busy_dialog.start();
              //  setEnabled(false);
                jmri.util.ThreadingUtil.runOnLayout( ()->{
                    _node.deleteEvOnNode(_ndEv.getNn(), _ndEv.getEn(), this );
                });
            }
        };
        framedeletebutton.addActionListener(deleteClicked);
        
        ActionListener editEvClicked = ae -> {
            busy_dialog = new jmri.util.swing.BusyDialog(this, "Teaching Node", false);
            busy_dialog.start();
         //   setEnabled(false);
            jmri.util.ThreadingUtil.runOnLayout( ()->{
                singleEVModel.passEditEvToNode(this);
            });
        };
        frameeditevbutton.addActionListener(editEvClicked);        
        
        numberSpinnerEv.addChangeListener((ChangeEvent e) -> {
            updateButtons();
        });
        
        numberSpinnernd.addChangeListener((ChangeEvent e) -> {
            updateButtons();
        });        
        
    }
    
    public boolean spinnersDirty(){
        return ( _ndEv.getNn() != getNodeVal() ) || ( _ndEv.getEn() != getEventVal() );
    }
    
    public void notifyLearnEvoutcome( int feedback, String message) {
        
        _ndEv.setNn( getNodeVal() );
        _ndEv.setEn( getEventVal() );
        
        _ndEv.setEvArr( Arrays.copyOf(
            singleEVModel.newEVs,
            singleEVModel.newEVs.length) );
        
        updateButtons();
        
        singleEVModel.fireTableDataChanged();
        
        busy_dialog.finish();
        
        if (feedback < 0 ) {          
            JOptionPane.showMessageDialog(null, 
            Bundle.getMessage("NdEvVarWriteError"), Bundle.getMessage("WarningTitle"),
            JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void notifyDeleteEvoutcome( int feedback, String message) {
        busy_dialog.finish();
        busy_dialog = null;
        updateButtons();
        if (feedback < 0 ) {
            log.info("Frame notified of Teach event status: {} {}",feedback,message);            
            JOptionPane.showMessageDialog(null, 
            message, Bundle.getMessage("WarningTitle"),
            JOptionPane.ERROR_MESSAGE);
        }
        
        this.dispose();
    }    
    
    // called on startup + when number spinners changed
    private void updateButtons(){
       
        setTitle( getTitle() );
        
        ndEvNameLabel.setText("<html><div style='text-align: center;'>" + 
        new CbusNameService(_memo).getEventNodeString(getNodeVal(), getEventVal() )
        + "</div></html>");
        
        if ( singleEVModel.isTableDirty() || spinnersDirty() ){
            frameResetButton.setEnabled(true);
        }
        else {
            frameResetButton.setEnabled(false);
        }
        
        CbusNode _node = nodeModel.getNodeByNodeNum( _ndEv.getParentNn() );
        if (_node == null){
            return;
        }
        
        if (isNewEvent) {
            framenewevbutton.setVisible(true);
            frameeditevbutton.setVisible(false);
            framedeletebutton.setVisible(false);
            frameCopyButton.setVisible(false);
            if ( _node.getNodeEvent(getNodeVal(),getEventVal() ) == null ) {
                
                framenewevbutton.setEnabled(true);
                framenewevbutton.setToolTipText(null);
                
            } else {
                framenewevbutton.setEnabled(false);
                framenewevbutton.setToolTipText("Event Already on Node");
            }
            
        } 
        else {
            framenewevbutton.setVisible(false);
            frameeditevbutton.setVisible(true);
            framedeletebutton.setVisible(true);
            frameCopyButton.setVisible(true);
            if ( spinnersDirty() || singleEVModel.isTableDirty() ){
                if ( _node.getNodeEvent(getNodeVal(),getEventVal() ) == null ) {
                    frameeditevbutton.setEnabled(true);
                    frameeditevbutton.setToolTipText(null);
                } else {
                    if ( spinnersDirty() ) {
                        frameeditevbutton.setEnabled(false);
                        frameeditevbutton.setToolTipText("Event Already on Node");
                    }
                    else {
                        frameeditevbutton.setEnabled(true);
                        frameeditevbutton.setToolTipText(null);
                    }
                }
            } else {
                frameeditevbutton.setEnabled(false);
                frameeditevbutton.setToolTipText(null);
            }
            if ( spinnersDirty() ) {
                framedeletebutton.setEnabled(false);
                framedeletebutton.setToolTipText("Cannot Delete an edited event or node number");
            }
            else {
                framedeletebutton.setEnabled(true);
                framedeletebutton.setToolTipText(null);
            }
        }
    }
    
    public int getEventVal(){
        return (Integer) numberSpinnerEv.getValue();
    }

    public int getNodeVal(){
        return (Integer) numberSpinnernd.getValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTitle() {
        
        if (nodeModel==null){
            return Bundle.getMessage("NewEvent");
        }
        
        CbusNode _node = nodeModel.getNodeByNodeNum( _ndEv.getParentNn() );
        if (_node!=null) {
            StringBuilder title = new StringBuilder();
            if ( isNewEvent ) {
                title.append( Bundle.getMessage("NewEvent"));
                title.append(" ");
            } else {
                title.append( Bundle.getMessage("EditEvent"));
                title.append(
                new CbusNameService(_memo).getEventNodeString(_ndEv.getNn(), _ndEv.getEn() ));
            }
            title.append("on ");
            title.append( Bundle.getMessage("CbusNode"));
            title.append( _node.getNodeNumberName());
            return title.toString();
        } else {
            return Bundle.getMessage("NewEvent");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void tableChanged(TableModelEvent e) {
        updateButtons();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        
        if ( mainpane != null ){
            mainpane.clearEditEventFrame();
        }
        super.dispose();
    }

    private final static Logger log = LoggerFactory.getLogger(CbusNodeEditEventFrame.class);
}
