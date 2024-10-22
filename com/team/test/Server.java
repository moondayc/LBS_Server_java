package com.team.test;

import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.io.DataInputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileInputStream;
public class Server {
    SP sp;
    HashMap<String, Object> params;
    byte[] params_all_bytes = null;
    ServerSocket serverSocket = null;
    byte[] paramHash = null;
    boolean Update = true; // false表示不更新参数
    public Server(int m, int n) throws IOException, ClassNotFoundException {
        sp = new SP();
        String File_name = String.valueOf(n) +"_"+ String.valueOf(n) + "_"+ "params_bytes.bin";
        String SK_File_name = String.valueOf(n) + "_"+ String.valueOf(n) + "_"+ "SK_bytes.bin";
        File file = new File(File_name);
        File sk_file = new File(SK_File_name);
        byte[] params_bytes;
        if (!Update && file.exists() && sk_file.exists()) {
            //读取公共参数且计算paramHash,并存储params_bytes
            params_bytes = Server.readBytesFromFile(File_name);
            byte[] SK_byes = Server.readBytesFromFile(SK_File_name);
            params = SP.setUP(params_bytes, SK_byes);
            System.out.println("Read public parameters successfully");
        }
        else {//生成公共参数且转换成bytes+4
            double startTime, endTime, all_time;
            startTime = System.currentTimeMillis();
            params = SP.setUP(m, n);
            endTime = System.currentTimeMillis();
            all_time = (endTime - startTime)/1000;
            System.out.println("系统初始化时间："+"m="+m+",n="+n+", time="+ all_time+" s");
            params_bytes = SP.hashMapToByteArray(params); // 假设params是定义好的
            // 存储公共参数和记录比特
            writeBytesToFile(params_bytes, File_name);
        }
        paramHash = SP.sha160(Arrays.toString(params_bytes));
        //构造生成params_all_bytes
        int params_bytes_lenght = params_bytes.length;
        ByteBuffer length_buffer = ByteBuffer.allocate(4);
        length_buffer.putInt(params_bytes_lenght); // 将整数写入缓冲区
        byte[] params_byteArray = length_buffer.array();
        System.out.println("length of params_bytes: " + (params_bytes_lenght/1024)+"KB");
        params_all_bytes = new byte[params_byteArray.length + params_bytes.length];
        System.arraycopy(params_byteArray, 0, params_all_bytes, 0, 4);
        System.arraycopy(params_bytes, 0, params_all_bytes, 4, params_bytes.length);
        System.out.println("m = " + m + ", n =" + n);

    }

    public HashMap<String, Object> process_message(HashMap<String, Object> pi)
            throws ClassNotFoundException, IOException {
        boolean piu_result = sp.verProofPIU(pi);
        HashMap<String, Object> pi2=null;
        if (piu_result) {
            pi2 = sp.genProofPI2(pi);
        }
        else {
            System.err.println("piu Error!!");
        }
        return pi2;
    }

    public void start(int port) {
        try {
            startServer(port); // 启动服务器
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    private void startServer(int port) throws ClassNotFoundException {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started, listening on port: " + port + ", waiting for connections...");

            // 循环接受多个客户端连接
            while (true) {
                Socket clientSocket = serverSocket.accept(); // 等待客户端连接
                System.out.println("Client connected!");

                // 为每个客户端连接创建一个新的线程
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Server stopped.");
        }
    }

    public static void writeBytesToFile(byte[] data, String filePath) {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(data);
            System.out.println( filePath +"finished!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static byte[] readBytesFromFile(String filePath) {
        File file = new File(filePath);
        byte[] data = new byte[(int) file.length()]; // 根据文件大小分配字节数组
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(data); // 将文件内容读入字节数组
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    // public static byte[] sha160(String input) {
    //     MessageDigest md;
    //     try {
    //         md = MessageDigest.getInstance("sha");
    //     } catch (NoSuchAlgorithmException e) {
    //         throw new RuntimeException(e);
    //     }
    //     return md.digest(input.getBytes());
    // }
    
    // 内部类处理客户端请求
    private byte[] convert_pi(HashMap<String, Object> pi)
        throws IOException {
        byte[] PI_bytes;
        PI_bytes = SP.hashMapToByteArray(pi);
        
        int PI_bytes_lenght = PI_bytes.length;
        ByteBuffer length_buffer = ByteBuffer.allocate(4);
        length_buffer.putInt(PI_bytes_lenght); // 将整数写入缓冲区
        byte[] PI_length_byteArray = length_buffer.array();
        byte[] PI_all_bytes = new byte[PI_length_byteArray.length + PI_bytes.length];
        //将长度和PI的字节数列整合到PI_all_bytes
        System.arraycopy(PI_length_byteArray, 0, PI_all_bytes, 0, 4);
        System.arraycopy(PI_bytes, 0, PI_all_bytes, 4, PI_bytes.length);
        return PI_all_bytes;
    }
    

    private class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }
        
        private byte[] received_message() throws IOException {
            DataInputStream inputStream = new DataInputStream(clientSocket.getInputStream());
            // 读取前四个字节，表示后续字节流的长度
            int dataLength = inputStream.readInt();

            System.out.println("压缩后PUI的字节数: " + ((float)dataLength / 1024) +" KB");

            // 创建一个足够大的字节数组来存储后续的字节流
            byte[] data = new byte[dataLength];

            // 从输入流中读取剩余的字节，并存储到字节数组中
            int bytesRead;
            int totalBytesRead = 0;

            while (totalBytesRead < dataLength) {
                bytesRead = inputStream.read(data, totalBytesRead, dataLength - totalBytesRead);
                if (bytesRead == -1) {
                    throw new IOException("Unexpected end of stream");
                }
                totalBytesRead += bytesRead;
            }

            // 确保读取到的字节数与预期的长度一致
            if (totalBytesRead != dataLength) {
                throw new IOException("Read " + totalBytesRead + " bytes, but expected " + dataLength + " bytes");
            }
            return data;
        }

        @Override
        public void run() {
            try (OutputStream outputStream = clientSocket.getOutputStream();
                    InputStream inputStream = clientSocket.getInputStream()) {
                    
                //连接后马上发送paramHash
                int hash_bytes_length = paramHash.length;
                ByteBuffer hash_buffer = ByteBuffer.allocate(4);
                hash_buffer.putInt(hash_bytes_length); // 将整数写入缓冲区
                byte[] hash_byteArray = hash_buffer.array();
                byte[] hash_all_bytes = new byte[hash_byteArray.length + paramHash.length];
                System.arraycopy(hash_byteArray, 0, hash_all_bytes, 0, 4);
                System.arraycopy(paramHash, 0, hash_all_bytes, 4, paramHash.length);
                outputStream.write(hash_all_bytes);
                System.out.println("length of server's params hash bytes: " + hash_bytes_length);
                
                //如果收到params，发送params，收到PI1，发送PI1,接收PI
                byte[] signal = new byte[6];
                while (inputStream.read(signal)!=-1) {
                    String receivedString = new String(signal, 0, 6, "UTF-8");
                    System.out.println("收到signal: " + receivedString);
                    if (receivedString.equals("PI1PI1")) {
                        System.out.println("signal == PI1");
                        HashMap<String, Object> pi1 = sp.genProofPI1();
                        System.out.println("PI1成功生成");
                        byte[] PI1_bytes = convert_pi(pi1);
                        // System.out.println("PI1成功转换");
                        outputStream.write(PI1_bytes);
                        outputStream.flush();
                        System.out.println("PI1 成功发送!!");
                        byte[] data = new byte[1024 * 100];
                        // 读取客户端发送的字节数据
                        System.out.println("开始接受PIU");
                        data = received_message();
                        try {
                            long t1, t2;
                            t1 = System.currentTimeMillis();
                            HashMap<String, Object> pi = SP.byteArrayToHashMap(data);
                            System.out.println("-----------------l="+ pi.get("l")+",k="+(pi.get("k"))+"-----------------");
                            HashMap<String, Object> pi2 = process_message(pi);
                            t2 = System.currentTimeMillis();
                            System.out.println("转换和验证PU,生成PI2的时间:"+(t2-t1)+" ms");
                            // 发送证明2
                            if (pi2 != null) {
                                byte[] pi2_all_bytes = convert_pi(pi2);
                                System.out.println("请求响应字节数: "+ (((float)(pi2_all_bytes.length - 4))/1024) + "KB");
                                outputStream.write(pi2_all_bytes);
                                outputStream.flush();
                                System.out.println("PI2 发送成功!!");
                            } else {
                                System.err.println("PIU Verification failed!");
                            }
                        } catch (Exception e) {
                            System.err.println(
                                    "Error occurred while converting byte array to HashMap: " + e.getMessage());
                        }
                    }
                    else if (receivedString.equals("Params")) {
                        // 发送公共参数
                        System.out.println("signal == Params");
                        System.out.println(params_all_bytes.length);
                        outputStream.write(params_all_bytes);
                        outputStream.flush();
                        System.out.println("param 发送成功!!");
                        signal = new byte[6];
                    }
                    else {
                        System.err.println("Signal Error!");
                        break;
                    }
                }
                
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Client disconnected.");
            }
        }
    }

    public static void saveStringToFile(String content, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);  // 将内容写入文件
            System.out.println("保存成功: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();  // 处理文件写入异常
        }
    }

    public static void main(String[] args) throws ClassNotFoundException, IOException {
        // int m = 10;
        // int n = 10;
        // for (int m = 400; m <= 500; m += 100) {
        //     new Server(m, m);
        //     System.out.println("-----------------------"+m+"-----------------------");
        // }
        int m = 100;
        int n = 100;
        Server s = new Server(m, n);
        int port = 2020; // 指定端口
        s.start(port);
        //子系统：172.26.208.43
    }
}
