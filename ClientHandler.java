import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class ClientHandler {
    private MyServer myServer;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String name;
    private long startTime;

    public String getName() {
        return name;
    }

    public ClientHandler(MyServer myServer, Socket socket, long startTime) {
        try {
            this.startTime = startTime;
            this.myServer = myServer;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    authentication();
                    if(!socket.isClosed()) {
                        readMessages();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }
            }).start();
        } catch (IOException e) {
            throw new RuntimeException("Проблемы при создании обработчика клиента");
        }
    }

    public void authentication() throws IOException {
        while (true) {
            long checkTime = System.currentTimeMillis()/1000;
            String str = in.readUTF();
            if((checkTime-startTime)<10) {
                try {
                    if (str.startsWith("/auth")) {
                        String[] parts = str.split("\\s");
                        String nick = DBservices.getNickByLoginAndPass(parts[1], parts[2]);
                        if (nick != null) {
                            if (!myServer.isNickBusy(nick)) {
                                sendMsg("/authok " + nick);
                                name = nick;
                                myServer.broadcastMsg(name + " зашел в чат");
                                myServer.subscribe(this);
                                return;
                            } else {
                                sendMsg("Учетная запись уже используется");
                            }
                        } else {
                            sendMsg("Неверные логин/пароль");
                        }
                    }
                }catch (ArrayIndexOutOfBoundsException e){
                    sendMsg("Заполните поля Login/Password");
                }


            }else{
                sendMsg("Время авторизации истекло");
                closeConnection();
                return;
            }

        }
    }



    public void readMessages() throws IOException {
        while (true) {
            String str = in.readUTF();
            if (str.startsWith("/")) {
                if (str.equals("/end")) {
                    break;
                }
                if (str.startsWith("/w ")) {
                    String[] tokens = str.split("\\s");
                    String nick = tokens[1];
                    String msg = str.substring(4 + nick.length());
                    myServer.sendMsgToClient(this, nick, msg);
                }
                if (str.startsWith("/changeNick")) {
                    String[] parts = str.split("\\s");
                    String newNick = parts[1];
                    if (DBservices.isNickBusy(newNick) == null) {
                        sendMsg("/changeNickOK" + " " + newNick);
                        myServer.broadcastMsg(name + " заменил nick на " + newNick);
                        DBservices.updateNick(name, newNick);
                        myServer.unsubscribe(this);
                        name = newNick;
                        myServer.subscribe(this);
                        myServer.broadcastClientsList();
                    }else {
                        sendMsg("/changeNickNotOK");
                    }
                }
                continue;
            }
            myServer.broadcastMsg(name + ": " + str);
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        if(name!=null) {
            myServer.unsubscribe(this);
            myServer.broadcastMsg(name + " вышел из чата");
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
