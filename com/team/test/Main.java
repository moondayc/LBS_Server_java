package com.team.test;

import it.unisa.dia.gas.jpbc.Element;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Main {
    // public static boolean compareHashmap(HashMap<String,Object>mp1,HashMap<String,Object>mp2){
    //     String[] intIterm=new String[]{"i", "j", "l", "k"};
    //     String[] dim1=new String[]{"Theta", "c", "z","Gamma1","Gamma2"};
    //     String[] dim2=new String[]{"K", "L", "gamma1", "gamma2","gamma3","c1","c2","z1","z2","_harr","LPrime","A","B","C","D"};
    //     if(mp1.size()!=mp2.size()){
    //         return false;
    //     }
    //     for(HashMap.Entry<String, Object> entry : mp1.entrySet()){
    //         String key = entry.getKey();
    //         if (Arrays.asList(dim1).contains(key)){
    //             Element[] v1= (Element[]) mp1.get(key);
    //             Element[] v2= (Element[]) mp2.get(key);
    //             if(!Arrays.equals(v2,v1)){
    //                 return false;
    //             }
    //         }
    //         else if (Arrays.asList(dim2).contains(key)){
    //             Element[][] v1= (Element[][]) mp1.get(key);
    //             Element[][] v2= (Element[][]) mp2.get(key);
    //             if(!Arrays.deepEquals(v2, v1)){
    //                 return false;
    //             }
    //         }
    //         else if (Arrays.asList(intIterm).contains(key)){
    //             int v1= (int) mp1.get(key);
    //             int v2= (int) mp2.get(key);
    //             if(v1!=v2){
    //                 return false;
    //             }
    //         }
    //         else {
    //             Element v1= (Element) mp1.get(key);
    //             Element v2= (Element) mp2.get(key);
    //             if(!v1.isEqual(v2)){
    //                 return false;
    //             }
    //         }
    //     }
    //     return true;
    // }

    public static void mapSendToSP(Socket socket, HashMap<String,Object> userMap) throws IOException {
        OutputStream out=socket.getOutputStream();
        byte[] msg;
        int msgLen;
        try {
            msg=User.hashMapToByteArray(userMap);
            msgLen=msg.length;
        } catch (IOException e) {
            System.err.println("(sendToSP)Error occurred while converting byte array to HashMap: " + e.getMessage());
            throw new RuntimeException(e);
        }
        try {
            ByteBuffer length_buffer = ByteBuffer.allocate(4);
            length_buffer.putInt(msgLen);
            byte[] PI_length_byteArray = length_buffer.array();
            // System.out.println("send to sp,msgLen: " + msgLen);
            byte[] PI_all_bytes = new byte[PI_length_byteArray.length + msgLen];
            //将长度和PI的字节数列整合到PI_all_bytes
            System.arraycopy(PI_length_byteArray, 0, PI_all_bytes, 0, 4);
            System.arraycopy(msg, 0, PI_all_bytes, 4, msgLen);
            out.write(PI_all_bytes);
        } catch (IOException e) {
            System.err.println("Error occurred while send msg to sp: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static byte[] bytesFromSP(Socket socket) throws IOException {
        //----------------新方法---------------------
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        // 读取前四个字节，表示后续字节流的长度
        int dataLength = inputStream.readInt();

        System.out.println("Received data length: " + dataLength);

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

    public static HashMap<String,Object> toMapFromSP(Socket socket) throws IOException {
        byte[] data = bytesFromSP(socket);
        HashMap<String, Object> spMap = null;
        try {
            // long t1, t2;
            // t1 = System.currentTimeMillis();
            spMap = User.byteArrayToHashMap(data);
            // t2 = System.currentTimeMillis();
            // System.out.println("byteArrayToHashMap的时间:"+(t2-t1)+" ms");
        } catch (Exception e) {
            System.err.println("(recFromSP)Error occurred while converting byte array to HashMap: " + e.getMessage());
        }
        return spMap;
    }

    public static void updateParams(Socket socket) throws IOException, ClassNotFoundException {
        byte[] recParamsHash=bytesFromSP(socket);
        byte[] localParams=User.readBytesFromFile();
        byte[] localParamsHash=User.sha160(Arrays.toString(localParams));

        HashMap<String,Object> params;
        if(!Arrays.equals(recParamsHash,localParamsHash)){//本地的公共参数哈希与收到的哈希不相等
            String signal = "Params";
            byte[] signalBytes = signal.getBytes(StandardCharsets.UTF_8);
            OutputStream out = socket.getOutputStream();
            out.write(signalBytes);

            //接收公共参数
            byte[] recParasBytes=bytesFromSP(socket);
            params= User.byteArrayToHashMap(recParasBytes);
//            System.out.println("params: " + params);
            User.writeBytesToFile(recParasBytes);
            System.out.println("Params updated.");
        }
        else {
            System.out.println("Params didn't need to update.");
            params= User.byteArrayToHashMap(localParams);
        }
        User.readParams(params);
    }

    public static void interact(String ip,int port) throws ClassNotFoundException{
        try (Socket socket = new Socket(ip, port)) {
            updateParams(socket);

            System.out.println("已连接到服务器");
            String signal = "PI1PI1";
            byte[] signalBytes = signal.getBytes(StandardCharsets.UTF_8);
            OutputStream out = socket.getOutputStream();
            out.write(signalBytes);

            User u1=new User();
            //接收pi1
            HashMap<String,Object> pi1= toMapFromSP(socket);
            boolean pi1Res=u1.verProof1(pi1);
            System.out.println("pi1Res: " + pi1Res);

            //O=(i,j) S=l*k
            long time1 = 0, time2, startTime, endTime;
            long duration1=0, duration2,duration3;
            
            int iPoint=3;
            int jPoint=4;
            int lRow=5;
            int kCol=5;
            if(pi1Res){
                //发送piu
                time1 = System.currentTimeMillis();
                HashMap<String, Object> piu = u1.genProofPIU(iPoint, jPoint, lRow, kCol);
                endTime = System.currentTimeMillis();

                mapSendToSP(socket, piu);
                duration1 = endTime - time1;
                System.out.println("客户端生成PU的时间: " + "l=" + lRow + ",k=" + lRow + ", time=" + duration1 + " ms");
            }
            //接收pi2
            
            HashMap<String, Object> pi2 = toMapFromSP(socket);
            startTime = System.currentTimeMillis();
            boolean pi2Res = u1.verProofPI2(lRow, kCol, pi2);
            endTime = System.currentTimeMillis();
            duration2 = (endTime - startTime);
            System.out.println("客户端验证PI2的时间: "+ duration2 + "ms");
            System.out.println("pi2Res: " + pi2Res);

            if(pi2Res){
                //计算服务M
                startTime = System.currentTimeMillis();
                Element[][] M = u1.computeM(iPoint, jPoint, lRow, kCol, pi2);
                
                //想输出的服务
                String wantSer="Bank";
                byte[] wantSerBytes = wantSer.getBytes();
                Element m=User.Gt.newElementFromHash(wantSerBytes,0,wantSerBytes.length);
                // System.out.println("m: "+m);

                ArrayList<int[]> pos=new ArrayList<>();
                int Mlen=0;
                System.out.println("-----------------M-----------------");
                for (int i = 0; i < M.length; i++) {
                    for (int j = 0; j < M[i].length; j++) {
                        // System.out.println(M[i][j]);
                        if (m.isEqual(M[i][j])) {
                            //S区域内有多少个这样的服务
                            pos.add(new int[] { iPoint + i + 1, jPoint + j + 1 });
                        }
                        Mlen++;
                    }
                }
                endTime = System.currentTimeMillis();
                duration3 = (endTime - startTime);
                System.out.println("客户端计算M的时间: " + "l=" + lRow + ",k=" + lRow + ", time=" + duration3 + " ms");
                time2 = System.currentTimeMillis();
                long d = time2 - time1;
                System.out.println("length of M: "+Mlen);

                System.out.println("length of arrayList: "+pos.size());
                for (int[] po : pos) {
                    System.out.println(po[0] + " " + po[1]);
                }
                //生成PIU，验证PI2，计算M
                System.out.println("客户端解析响应的时间: " + (duration1+duration2+ duration3) + "ms");
                System.out.println("一次查询总时间："+d+" ms");
            }

        } catch (IOException e) {
            System.out.println("服务器连接失败！");
            throw new RuntimeException(e);
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte aByte : bytes) {
            result.append(String.format("%02x", aByte));
            // upper case
            // result.append(String.format("%02X", aByte));
        }
        return result.toString();
    }

    public static void main(String[] args) throws ClassNotFoundException {
        //String ip = "10.192.2.3";
        String ip = "172.26.208.43";
        int port = 2020;
        interact(ip, port);

        // String ip = "172.26.208.43";
        // int port = 2020;
        // List<Integer> ls = List.of(5,10,10,15,15,20);
        // List<Integer> lk = List.of(5,10,15,10,15,20);
        // for (int i = 0; i < ls.size(); i++) {
        //     int l = ls.get(i);
        //     int k = lk.get(i);
        //     interact(ip, port, l, k);
        //     System.out.println("------------l=" + l + ",k=" + k + "-------------------");
        // }
        
    }
}
