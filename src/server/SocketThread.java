/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.StringTokenizer;


public class SocketThread implements Runnable{
    
    Socket socket;
    MainForm main;
    DataInputStream dis;
    StringTokenizer st;
    String client, filesharing_username;
    
    private final int BUFFER_SIZE = 100;
    
    public SocketThread(Socket socket, MainForm main){
        this.main = main;
        this.socket = socket;
        
        try {
            dis = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            main.appendMessage("[SocketThreadIOException]: "+ e.getMessage());
        }
    }
    
    /*   This method will get the client socket in client socket list then stablish a connection    */
    private void createConnection(String receiver, String sender, String filename){
        try {
            main.appendMessage("[createConnection]: creating file sharing connection.");
            Socket s = main.getClientList(receiver);
            if(s != null){ // Client was exists
                main.appendMessage("[createConnection]: Socket OK");
                DataOutputStream dosS = new DataOutputStream(s.getOutputStream());
                main.appendMessage("[createConnection]: DataOutputStream OK");
                // Format:  CMD_FILE_XD [sender] [receiver] [filename]
                String format = "CMD_FILE_XD "+sender+" "+receiver +" "+filename;
                dosS.writeUTF(format);
                main.appendMessage("[createConnection]: "+ format);
            }else{// Client was not exist, send back to sender that receiver was not found.
                main.appendMessage("[createConnection]: Client was not found '"+receiver+"'");
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF("CMD_SENDFILEERROR "+ "Client '"+receiver+"' was not found in the list, make sure it is on the online list.!");
            }
        } catch (IOException e) {
            main.appendMessage("[createConnection]: "+ e.getLocalizedMessage());
        }
    }
    
    @Override
    public void run() {
        try {
            while(true){
                /** Get Client Data **/
                String data = dis.readUTF();
                st = new StringTokenizer(data);
                String CMD = st.nextToken();
                /** Check CMD **/
                switch(CMD){
                    case "CMD_JOIN":
                        /** CMD_JOIN [clientUsername] **/
                        String clientUsername = st.nextToken();
                        client = clientUsername;
                        main.setClientList(clientUsername);
                        main.setSocketList(socket);
                        main.addPlayer(clientUsername);
                        main.appendMessage("[Client]: "+ clientUsername +" memasuki chat room !");
                        break;
                    case "CMD_MOVE":
                        /** CMD_JOIN [clientUsername] **/
                        String username = st.nextToken();
                        String position = st.nextToken();
                        main.movePlayer(username, position);
                        main.sendStatus();
                        break;
                        
                     
                    case "CMD_CHAT":
                        /** CMD_CHAT [from] [sendTo] [message] **/
                        String from = st.nextToken();
                        String sendTo = st.nextToken();
                        String msg = "";
                        while(st.hasMoreTokens()){
                            msg = msg +" "+ st.nextToken();
                        }
                        Socket tsoc = main.getClientList(sendTo);
                        try {
                            DataOutputStream dos = new DataOutputStream(tsoc.getOutputStream());
                            /** CMD_MESSAGE **/
                            String content = from +": "+ msg;
                            dos.writeUTF("CMD_MESSAGE "+ content);
                            main.appendMessage("[Message]: From "+ from +" To "+ sendTo +" : "+ msg);
                        } catch (IOException e) {  main.appendMessage("[IOException]: tidak bisa mengirim pesan ke "+ sendTo); }
                        break;
                    
                    case "CMD_CHATALL":
                        /** CMD_CHATALL [from] [message] **/
                        String chatall_from = st.nextToken();
                        String chatall_msg = "";
                        while(st.hasMoreTokens()){
                            chatall_msg = chatall_msg +" "+st.nextToken();
                        }
                        String chatall_content = chatall_from +" "+ chatall_msg;
                        for(int x=0; x < main.clientList.size(); x++){
                            if(!main.clientList.elementAt(x).equals(chatall_from)){
                                try {
                                    Socket tsoc2 = (Socket) main.socketList.elementAt(x);
                                    DataOutputStream dos2 = new DataOutputStream(tsoc2.getOutputStream());
                                    dos2.writeUTF("CMD_MESSAGE "+ chatall_content);
                                } catch (IOException e) {
                                    main.appendMessage("[CMD_CHATALL]: "+ e.getMessage());
                                }
                            }
                        }
                        main.appendMessage("[CMD_CHATALL]: "+ chatall_content);
                        break;
                    
                        
                    default: 
                        main.appendMessage("[CMDException]: Unknown Command "+ CMD);
                    break;
                }
            }
        } catch (IOException e) {
            /*   this is for chatting client, remove if it is exists..   */
            System.out.println(client);
            System.out.println("File Sharing: " +filesharing_username);
            main.removeFromTheList(client);
            if(filesharing_username != null){
                main.removeClientFileSharing(filesharing_username);
            }
            main.appendMessage("[SocketThread]: Koneksi Client ditutup !.");
        }
    }
    
}
