package com.team.test;

import java.util.HashMap;
import java.util.Map;
import java.io.*;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.jpbc.Field;

public class GetServiceM {
    private static final Pairing pairing = PairingFactory.getPairing("a.properties");
    private static final Field<Element> gt = pairing.getGT();

    // 将字符串矩阵根据HashMap映射进行替换
    public static Element[][] mapMatrix(String[][] matrix, Map<String, Element> newmap) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        Element[][] mappedMatrix = new Element[rows][cols];

        // 遍历矩阵并替换元素
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                String originalValue = matrix[i][j];
                Element mappedValue = newmap.get(originalValue); // 根据映射获取值
                mappedMatrix[i][j] = (mappedValue != null) ? mappedValue : null; // 若找不到映射，则保留原值
            }
        }
        return mappedMatrix;
    }

    // 打印矩阵
    public static void printMatrix(Element[][] matrix) {
        for (Element[] row : matrix) {
            for (Element element : row) {
                System.out.print(element + "\t");
            }
            System.out.println();
        }
    }

    public static void saveGTMatrixToFile(Element[][] gtMatrix, String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            // 写入矩阵的维度
            oos.writeInt(gtMatrix.length); // 行数
            oos.writeInt(gtMatrix[0].length); // 列数
            
            // 写入矩阵的每个元素
            for (Element[] row : gtMatrix) {
                for (Element element : row) {
                    oos.writeObject(element.toBytes()); // 将GT元素转换为字节并写入
                }
            }
            System.out.println("GT Matrix saved to file: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

   public static Element[][] loadGTMatrixFromFile(String filename) {
        Element[][] gtMatrix = null;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            // 读取矩阵的维度
            int rows = ois.readInt();
            int cols = ois.readInt();
            gtMatrix = new Element[rows][cols];

            // 读取矩阵的每个元素
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    byte[] elementBytes = (byte[]) ois.readObject(); // 读取字节
                    gtMatrix[i][j] = gt.newElementFromBytes(elementBytes); // 从字节恢复GT元素
                }
            }
            System.out.println("GT Matrix loaded from file: " + filename);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return gtMatrix;
    }

    // 生成一张 GT: positions的映射表
    public HashMap<Element, String> mapGTToString(String[] positions) {
        HashMap<Element, String> map = new HashMap<>();
        for (String str : positions) {
            byte[] str_byte = str.getBytes();
            Element GT_Element = gt.newElementFromHash(str_byte, 0, str_byte.length);
            map.put(GT_Element, str);
        }
        return map;
    }

    // 生成一张 position: GTElement的映射表
    public HashMap<String, Element> mapStringToGT(String[] positions) {
        HashMap<String, Element> map = new HashMap<>();
        for (String str : positions) {
            byte[] position_byte = str.getBytes();
            Element GT_Element = gt.newElementFromHash(position_byte, 0, position_byte.length);
            // String GT_Element_String = GT_Element.toString();
            // String GT_Element_String = GTElementMapper.bytesToHex(GT_Element.toBytes());
            map.put(str, GT_Element);
        }
        return map;
    }

    // 将HashMap保存为二进制文件
    public void saveMapToBinaryFile(HashMap<String, Element> map, String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            for (Map.Entry<String, Element> entry : map.entrySet()) {
                // 将字符串和GT元素的字节数组写入文件
                oos.writeObject(entry.getKey());
                oos.writeObject(entry.getValue().toBytes());
            }
            System.out.println("Mapping saved to binary file: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 从二进制文件中读取HashMap
    public HashMap<String, Element> loadMapFromBinaryFile(String filename) {
        HashMap<String, Element> map = new HashMap<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            while (true) {
                String key = (String) ois.readObject();
                byte[] elementBytes = (byte[]) ois.readObject();
                Element element = gt.newElementFromBytes(elementBytes);
                map.put(key, element);
            }
        } catch (EOFException e) {
            // End of file reached, normal behavior
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("Mapping loaded from binary file: " + filename);
        return map;
    }

    // 打印结果
    public static void printGTtoString(HashMap<Element, String> map) {
        for (Map.Entry<Element, String> entry : map.entrySet()) {
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
    }

    public static void printStringToGT(HashMap<String, Element> map) {
        for (Map.Entry<String, Element> entry : map.entrySet()) {
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
    }

    public static void main(String[] args) {
        // 服务名矩阵
        String[][] matrix = StringMatrixCSV.loadMatrixFromCSV("ServiceM.csv");

        // ServiceTOGTmap 服务名:GT运算映射表
        GetServiceM mapper = new GetServiceM();
        HashMap<String, Element> ServiceTOGTmap = new HashMap<>();
        // // 假设有一个字符串集合
        String[] ServiceList = { "Bank", "Restaurant", "Hospital", "School", "Library" };
        ServiceTOGTmap = mapper.mapStringToGT(ServiceList);
        System.out.println(ServiceTOGTmap);
        
        // // 替换矩阵元素
        Element[][] mappedMatrix = mapMatrix(matrix, ServiceTOGTmap);
        //保存替换矩阵
        GetServiceM.saveGTMatrixToFile(mappedMatrix, "ServiceM.dat");
        
        // 打印替换后的矩阵
        // System.out.println("替换后的矩阵：");
        // printMatrix(mappedMatrix);
        //打印重新下载的矩阵
        // Element[][] SMatrix = GetServiceM.loadGTMatrixFromFile("ServiceM.dat");
        // printMatrix(SMatrix);

    }
}
