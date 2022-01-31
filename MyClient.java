
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;



public class MyClient extends JFrame {
    private final String SERVER_ADDR = "127.0.0.1";
    private final int SERVER_PORT = 8189;
    private final int LOG_BUFFER = 100;
    private int logSize = 0;
    private JTextField msgInputField;
    private JTextArea chatArea;
    private JTextField loginField;
    private JTextField passField;
    private JButton btnSendAuth;
    private JComboBox nameList;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Boolean authorized;
    private String myNick;
    private JFrame changeNickFrame;
    private JButton newName;
    private JButton sendNewNick;
    private JTextField newNickField;
    private String logFileName;
    private Thread t;


    public MyClient() {
        prepareGUI();
        try {
            openConnection();
        } catch (IOException | InterruptedException e) {
            chatArea.append("Сервер не активен. Попробуйте подключиться позже");
            e.printStackTrace();
        }

    }

    private void setAuthorized(Boolean bol){
        authorized = bol;
    }
    public void openConnection() throws IOException, InterruptedException {
        socket = new Socket(SERVER_ADDR, SERVER_PORT);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        setAuthorized(false);
        t = new Thread(() -> {
            try {
                while (true) {
                    String strFromServer = in.readUTF();
                    if (strFromServer.startsWith("/authok")) {
                        myNick = strFromServer.split("\\s")[1];
                        setTitle(myNick);
                        setAuthorized(true);
                        newName.setEnabled(true);
                        btnSendAuth.setBackground(Color.GREEN);
                        btnSendAuth.setText("Выйти");
                        DBservices.dbStart();
                        logFileName = "log_" + DBservices.getLoginByName(myNick) + ".txt";
                        File logFile = new File(logFileName);

                        if (logFile.exists()) {
                            List<String> stringList = new ArrayList<>(Files.readAllLines(Paths.get(logFileName)));
                            logSize = stringList.size();


                        try (BufferedReader reader = new BufferedReader(new FileReader(logFileName))) {
                            String str;
                            Integer i = 0;

                            while ((str = reader.readLine()) != null) {
                                if (i > (logSize - LOG_BUFFER)) {
                                    chatArea.append(str + "\n");
                                }
                                i++;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                        break;
                    }
                    chatArea.append(strFromServer + "\n");
                }

                while (true) {
                        String strFromServer = in.readUTF();
                        if (strFromServer.equalsIgnoreCase("/end")) {
                            closeConnection();
                            break;
                        }
                        if(strFromServer.startsWith("/clients")){
                            String[] clientsFromServer = strFromServer.split("\\s");
                            nameList.removeAllItems();
                            nameList.addItem("Send to all");
                            for (int i = 1; i<clientsFromServer.length; i++){
                                if(!clientsFromServer[i].equals(myNick)) {
                                    nameList.addItem(clientsFromServer[i]);
                                }
                            }

                        }
                        if (strFromServer.startsWith("/changeNickOK")){
                            myNick = strFromServer.split("\\s")[1];
                            setTitle(myNick);
                            setEnabled(true);
                            changeNickFrame.dispose();

                        }
                        if (strFromServer.startsWith("/changeNickNotOK")){
                            changeNickFrame.setTitle("Выбранный nick занят");
                        }

                        chatArea.append(strFromServer);
                        chatArea.append("\n");
                       try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFileName, true))){
                            writer.write("\n");
                            writer.write(strFromServer);
                       }catch(IOException e){
                           e.printStackTrace();
                       }


                }
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                try {
                    setAuthorized(false);
                    socket.close();
                    myNick = "";
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });
        t.setDaemon(true);
        t.start();
    }

    public void onAuthClick() throws IOException, InterruptedException {
        if (socket.isClosed()) {
            chatArea.append("Сервер закрыл Ваше соединение\n");
        }else {

            try {
                if (!authorized) {
                    out.writeUTF("/auth" + " " + loginField.getText() + " " + passField.getText());
                    loginField.setText("");
                    passField.setText("");
                }else {
                    out.writeUTF("/end");
                    loginField.setText("");
                    passField.setText("");
                    btnSendAuth.setBackground(Color.GRAY);
                    btnSendAuth.setEnabled(false);

                    closeConnection();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void closeConnection() {

        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void sendMessage() {
        if (!msgInputField.getText().trim().isEmpty()&&!socket.isClosed()) {
            try {
                if(nameList.getSelectedItem().equals("Send to all")) {
                    out.writeUTF(msgInputField.getText());
                    msgInputField.setText("");
                    msgInputField.grabFocus();
                } else {
                    out.writeUTF("/w " + nameList.getSelectedItem() + " " + msgInputField.getText());
                    msgInputField.setText("");
                    msgInputField.grabFocus();
                }
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Ошибка отправки сообщения");
            }
        }
    }
    public void prepareGUI() {
// Параметры окна
        setBounds(600, 300, 500, 500);
        setTitle("Клиент");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
// Текстовое поле для вывода сообщений
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);
// Нижняя панель с полем для ввода сообщений и кнопкой отправки сообщений

        JPanel bottomPanel = new JPanel();
        JPanel upPanel = new JPanel();
        upPanel.setLayout(new BoxLayout(upPanel, BoxLayout.X_AXIS));
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));

        JButton btnSendMsg = new JButton("Отправить");
        btnSendAuth = new JButton("Авторизоваться");
        btnSendAuth.setBackground(Color.GRAY);
        newName = new JButton("Change Nick");
        newName.setEnabled(false);



        msgInputField = new JTextField();
        loginField = new JTextField();
        passField = new JTextField();

        nameList = new JComboBox();



        loginField.setText("Login");
        passField.setText("Password");

        upPanel.add(loginField);
        upPanel.add(passField);
        upPanel.add(btnSendAuth);
        upPanel.add(newName);

        bottomPanel.add(msgInputField);
        bottomPanel.add(btnSendMsg);
        bottomPanel.add(nameList);

        add(bottomPanel, BorderLayout.SOUTH);
        add(upPanel, BorderLayout.NORTH);

        newName.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeNick();
                setEnabled(false);
            }
        });

        btnSendMsg.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        btnSendAuth.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    onAuthClick();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });
        msgInputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        loginField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
               loginField.setText("");
            }
        });

        passField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                passField.setText("");
            }
        });
// Настраиваем действие на закрытие окна
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                        out.writeUTF("/end");
                        closeConnection();

                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        });
        setVisible(true);



    }
    public void changeNick(){
        changeNickFrame = new JFrame();
        changeNickFrame.setBounds(1250, 300, 400, 100);
        changeNickFrame.setTitle("Enter your new Nick");
        changeNickFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        sendNewNick = new JButton("Ok!                                            ");
        JButton exit = new JButton("Отмена                                        ");

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.add(exit);
        bottomPanel.add(sendNewNick);
        newNickField = new JTextField();
        newNickField.setText("Your new Nick");
        changeNickFrame.add(newNickField, BorderLayout.NORTH);
        changeNickFrame.add(bottomPanel, BorderLayout.SOUTH);
        changeNickFrame.setVisible(true);

        newNickField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
               newNickField.setText("");
            }
        });

        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeNickFrame.dispose();
                setEnabled(true);
            }
        });

        sendNewNick.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!newNickField.getText().isEmpty())
                try {
                    out.writeUTF("/changeNick" + " " + newNickField.getText());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

    }


}