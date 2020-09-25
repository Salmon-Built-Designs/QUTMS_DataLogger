
import com.fazecast.jSerialComm.SerialPort;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class MainWindow extends JFrame implements CANDataTransmission.IReceiveCANCallback{
    private final String windowTitle = "QUTMS CAN Logger";
    private final int MAX_DATA = 8;

    // max value of 14 bits
    private final int MAX_EXTRA_ID = 0x3FFF;

    JSpinner spinnerDataLength;
    JSpinner[] spinnerDataBytes;

    String[] priority = {"Error", "Heartbeat", "Normal"};
    String[] sourceID = {"External", "Chassis Controller", "AMS", "BMS",
            "Shutdown", "Shutdown - BPSD", "Shutdown - Current", "Shutdown - IMD",
            "PDM", "Steering Wheel", "Charger", "Sensors"};
    String[] autonomous = {"No", "Yes"};
    String[] messageType = {"Heartbeat", "Error Detected", "Data Receive", "Data Transmit"};
    String[] serialPorts;

    SerialPort[] sp = SerialPort.getCommPorts();

    JComboBox comboMsgPriority;
    JComboBox comboMsgSourceID;
    JComboBox comboMsgAutonomous;
    JComboBox comboMsgType;

    JSpinner spinnerMsgExtra;

    JComboBox comboSerialPort;
    JSpinner BAUDSpeed;

    JTextArea consoleLog;

    CANMessageTableModel CANTableModel;

    CANDataTransmission cdt;



    public MainWindow() {
        setupGUI();
    }

    /**
     * Initializes and creates the GUI
     *
     */
    private void setupGUI() {
        this.setTitle(windowTitle);

        // set close operation
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                MainWindow.this.setVisible(false);
                MainWindow.this.dispose();
            }
        });

        this.setPreferredSize(new Dimension(1000,600));
        this.setMinimumSize(new Dimension(900,600));
        this.setLayout(new GridLayout(3,1));

        JPanel sendMessagePanel = new JPanel();
        JPanel recievedMessagePanel = new JPanel();

        // send message panel
        JPanel messageHeaderPanel = new JPanel();
        JPanel messageContentsPanel = new JPanel();

        // message header panel
        messageHeaderPanel.setLayout(new GridLayout(1, 5));

        String[] headerLabels = {"Priority", "ID", "Autonomous", "Message Type", "Extra ID"};

        //Comm panel
        JPanel commPanel = new JPanel();
        commPanel.setLayout(new GridLayout(1,2));

        // add data fields

        comboMsgPriority = new JComboBox(priority);
        comboMsgSourceID = new JComboBox(sourceID);
        comboMsgAutonomous = new JComboBox(autonomous);
        comboMsgType = new JComboBox(messageType);
        spinnerMsgExtra = new JSpinner(new SpinnerNumberModel(0,0,MAX_EXTRA_ID, 1));

        JPanel msgInfoWrapper = new JPanel();
        JLabel msgInfoLabel = new JLabel(headerLabels[0]);
        msgInfoWrapper.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        msgInfoWrapper.setLayout(new BorderLayout());
        msgInfoWrapper.add(comboMsgPriority, BorderLayout.CENTER);
        msgInfoWrapper.add(msgInfoLabel, BorderLayout.NORTH);

        messageHeaderPanel.add(msgInfoWrapper);

        msgInfoWrapper = new JPanel();
        msgInfoLabel = new JLabel(headerLabels[1]);
        msgInfoWrapper.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        msgInfoWrapper.setLayout(new BorderLayout());
        msgInfoWrapper.add(comboMsgSourceID, BorderLayout.CENTER);
        msgInfoWrapper.add(msgInfoLabel, BorderLayout.NORTH);

        messageHeaderPanel.add(msgInfoWrapper);

        msgInfoWrapper = new JPanel();
        msgInfoLabel = new JLabel(headerLabels[2]);
        msgInfoWrapper.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        msgInfoWrapper.setLayout(new BorderLayout());
        msgInfoWrapper.add(comboMsgAutonomous, BorderLayout.CENTER);
        msgInfoWrapper.add(msgInfoLabel, BorderLayout.NORTH);

        messageHeaderPanel.add(msgInfoWrapper);

        msgInfoWrapper = new JPanel();
        msgInfoLabel = new JLabel(headerLabels[3]);
        msgInfoWrapper.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        msgInfoWrapper.setLayout(new BorderLayout());
        msgInfoWrapper.add(comboMsgType, BorderLayout.CENTER);
        msgInfoWrapper.add(msgInfoLabel, BorderLayout.NORTH);

        messageHeaderPanel.add(msgInfoWrapper);

        msgInfoWrapper = new JPanel();
        msgInfoLabel = new JLabel(headerLabels[4]);
        msgInfoWrapper.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        msgInfoWrapper.setLayout(new BorderLayout());
        msgInfoWrapper.add(spinnerMsgExtra, BorderLayout.CENTER);
        msgInfoWrapper.add(msgInfoLabel, BorderLayout.NORTH);

        messageHeaderPanel.add(msgInfoWrapper);

        cdt = new CANDataTransmission(this);

        //commPanel
        serialPorts = cdt.getPortDescriptionLists();
        comboSerialPort = new JComboBox(serialPorts);
        comboSerialPort.setSelectedIndex(serialPorts.length - 1);

        comboSerialPort.addActionListener(new ActionListener() {
                                              @Override
                                              public void actionPerformed(ActionEvent e) {

                                                  cdt.setupSelectedPort(sp[comboSerialPort.getSelectedIndex()], (Integer) BAUDSpeed.getValue());

                                              }
                                          }
        );

        comboSerialPort.setMaximumSize( comboSerialPort.getPreferredSize());

        BAUDSpeed = new JSpinner(new SpinnerNumberModel(115200, 0, 1000000,1));

        cdt.setupSelectedPort(sp[comboSerialPort.getSelectedIndex()], (Integer) BAUDSpeed.getValue());

        msgInfoWrapper = new JPanel();
        msgInfoLabel = new JLabel("COM");
        msgInfoWrapper.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        msgInfoWrapper.setLayout(new BorderLayout());
        msgInfoWrapper.add(comboSerialPort, BorderLayout.CENTER);
        msgInfoWrapper.add(msgInfoLabel, BorderLayout.NORTH);

        commPanel.add(msgInfoWrapper);

        msgInfoWrapper = new JPanel();
        msgInfoLabel = new JLabel("BAUDSpeed");
        msgInfoWrapper.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        msgInfoWrapper.setLayout(new BorderLayout());
        msgInfoWrapper.add(BAUDSpeed, BorderLayout.CENTER);
        msgInfoWrapper.add(msgInfoLabel, BorderLayout.NORTH);

        commPanel.add(msgInfoWrapper);

        this.add(commPanel);

        // message contents panel
        messageContentsPanel.setLayout(new GridLayout(1, MAX_DATA+2));

        String[] contentsLabels = {"Length", "Byte0", "Byte1", "Byte2", "Byte3", "Byte4", "Byte5", "Byte6", "Byte7", ""};

        spinnerDataLength = new JSpinner(new SpinnerNumberModel(0,0,8, 1));
        JLabel dataLengthLabel = new JLabel(contentsLabels[0]);

        JPanel dataLengthWrapper = new JPanel();
        dataLengthWrapper.setBorder(BorderFactory.createEmptyBorder(50,20,50,20));
        dataLengthWrapper.setLayout(new BorderLayout());
        dataLengthWrapper.add(spinnerDataLength, BorderLayout.CENTER);
        dataLengthWrapper.add(dataLengthLabel, BorderLayout.NORTH);
        messageContentsPanel.add(dataLengthWrapper);

        spinnerDataBytes = new JSpinner[MAX_DATA];

        for (int i = 0; i < MAX_DATA; i++) {
            JLabel textLabel = new JLabel(contentsLabels[i+1]);

            SpinnerNumberModel dataModel = new SpinnerNumberModel(0,0,0xFF, 1);
            spinnerDataBytes[i] = new JSpinner(dataModel);


            // pack data field into a panel for formatting
            JPanel dataWrapper = new JPanel();
            dataWrapper.setBorder(BorderFactory.createEmptyBorder(50,20,50,20));
            dataWrapper.setLayout(new BorderLayout());

            dataWrapper.add(textLabel, BorderLayout.NORTH);
            dataWrapper.add(spinnerDataBytes[i], BorderLayout.CENTER);

            messageContentsPanel.add(dataWrapper);
        }

        // enable / disable data bytes based on whats selected
        spinnerDataLength.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int dataLength = (int)spinnerDataLength.getValue();
                for (int i = 0; i < MAX_DATA; i++) {
                    spinnerDataBytes[i].setEnabled(i < dataLength);
                }
            }
        });
        spinnerDataLength.setValue(1);

        JButton btnSendMessage = new JButton("Send");
        btnSendMessage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SendMessage();
            }
        });
        messageContentsPanel.add(btnSendMessage);



        sendMessagePanel.setLayout(new GridLayout(2,1));
        sendMessagePanel.add(messageHeaderPanel);
        sendMessagePanel.add(messageContentsPanel);



        // recieved message panel
        JPanel messageTablePanel = new JPanel();
        JPanel consolePanel = new JPanel();

        // message table
        CANTableModel = new CANMessageTableModel();
        CANTableModel.AddMessage(new CANMessage(LocalDateTime.now(), "Test", "source", "auto", "type", 2, 5, new int[]{1, 2, 3, 4, 5, 6, 7, 8}));

        JTable messageTable = new JTable(CANTableModel);
        messageTable.getTableHeader().setReorderingAllowed(false);
        messageTable.setRowSelectionAllowed(true);
        messageTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


        JScrollPane tableScrollPane = new JScrollPane(messageTable);

        messageTablePanel.setLayout(new BorderLayout());
        messageTablePanel.add(tableScrollPane, BorderLayout.CENTER);

        consoleLog = new JTextArea();

        consolePanel.setLayout(new BorderLayout());
        consolePanel.add(consoleLog, BorderLayout.CENTER);

        messageTablePanel.setBackground(Color.red);
        consolePanel.setBackground(Color.green);

        recievedMessagePanel.setLayout(new GridLayout(2,1));
        recievedMessagePanel.add(messageTablePanel);
        recievedMessagePanel.add(consolePanel);

        // add panels to GUI and pack
        this.add(sendMessagePanel);
        this.add(recievedMessagePanel);
        this.pack();

    }

    /**
     * Called when a CAN message is received over serial
     *
     */
    public void ReceiveMessage() {
        long extID = cdt.getExtID();
        byte[] rawData = cdt.getData();
        int dlc = cdt.getLength();
        int[] data;
        if (dlc == 0) {
            data = null;
        } else {
            data = new int[dlc];
            for (int i = 0; i < data.length; i++) {
                data[i] = rawData[i];
            }
        }


        CANMessage message = new CANMessage(LocalDateTime.now(), "", "", "", "", 0, dlc, data);
        CANTableModel.AddMessage(message);
    }

    /**
     * Called Send button is clicked
     * Creates a CAN message out of the current fields
     */
    public void SendMessage() {
        // grab data length and packets
        int dataLength = (int)spinnerDataLength.getValue();
        int[] dataPackets = new int[dataLength];
        for (int i = 0; i < dataLength; i++) {
            dataPackets[i] = (int)spinnerDataBytes[i].getValue();
        }

        System.out.println(Arrays.toString(dataPackets));

        String priority = (String)comboMsgPriority.getSelectedItem();
        String sourceID = (String)comboMsgSourceID.getSelectedItem();
        String autonomous = (String)comboMsgAutonomous.getSelectedItem();
        String messageType = (String)comboMsgType.getSelectedItem();
        int extraID = (int)spinnerMsgExtra.getValue();

        CANMessage message = new CANMessage(LocalDateTime.now(), priority, sourceID, autonomous, messageType, extraID, dataLength, dataPackets);
        AddToLog("Send Message");


        // TODO: replace this with serial sending code
        //RecveMessage();
    }

    public void AddToLog(String message) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formatDateTime = LocalDateTime.now().format(formatter);
        consoleLog.append(formatDateTime + "\t" + message + "\n");
    }
}
