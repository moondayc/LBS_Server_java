package com.team.test;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.Point;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class SP {
    public static final Pairing bp = PairingFactory.getPairing("a.properties");
    public static final Field G1 = bp.getG1();
    public static final Field Gt = bp.getGT();
    public static final Field Zr = bp.getZr();

    private static HashMap<String, Object> SK = new HashMap<>();

    public static Element _g, g1, g2, h1, h2, H, W1, W2, W1Prime, W2Prime;
    public static Element[] Gamma1; // m个
    public static Element[] Gamma2; // n个
    public static Element[][] A; // m*n
    public static Element[][] B; // m*n
    public static Element[][] C; // m*3
    public static Element[][] D; // n*3
    public static int m, n;
    public static Element[][] serviceM;
    // public static HashMap<String, Object> publicParams;
    public HashMap<String, Object> PI2 = new HashMap<>();
    public HashMap<String, Object> PI1 = new HashMap<>();

    public static byte[] ElementtoBytesCompressed(Element element) {
        Point point = (Point) element.duplicate(); // 创建新的Point对象
        byte[] compressed_bytes = point.toBytesCompressed();
        return compressed_bytes;
    }

    // 压缩比特变成Element (和elemnt同一类型)
    public static Element BytesCompressedToElement(byte[] compressed_bytes) {
        Point re_point = (Point) G1.newElement();
        re_point.setFromBytesCompressed(compressed_bytes);
        Element element = (Element) re_point;
        return element;
    }

    public static byte[] hashMapToByteArray(HashMap<String, Object> map) throws IOException {
        String[] intIterm = new String[] { "i", "j", "l", "k","m","n" };
        String[] dim1 = new String[] { "Theta", "c", "z", "Gamma1", "Gamma2" };
        String[] dim2 = new String[] { "K", "L", "gamma1", "gamma2", "gamma3", "c1", "c2", "z1", "z2", "_harr",
                "LPrime", "A", "B", "C", "D" };
        String[] G1_item = { "g1", "g2", "h1", "h2", "_g", "W1", "W2", "W1Prime", "W2Prime", "_hHat", "E1", "E2", "F1",
                "F2", "J1",
                "J2", "I1", "I2", "I3", "I4", "E1Prime", "E2Prime", "_h" };
        String[] G1_items = { "Gamma1", "Gamma2" };
        String[] G1_itemss = { "A", "K", "gamma1", "_harr" };

        HashMap<String, Object> newMap = new HashMap<>();
        // for (String key : map.keySet())
        map.forEach((key, value) -> {
            if (Arrays.asList(intIterm).contains(key)) { // int
                newMap.put(key, value);
            } else if (Arrays.asList(dim1).contains(key)) {// Element[] to byte[][]
                Element[] tempValue = (Element[]) value;
                byte[][] tempBytes = new byte[tempValue.length][];
                // 如果是G1元素
                if (Arrays.asList(G1_items).contains(key)) {
                    for (int i = 0; i < tempValue.length; i++) {
                        // tempBytes[i] = tempValue[i].toBytes();
                        tempBytes[i] = ElementtoBytesCompressed(tempValue[i]);
                    }
                } else {
                    for (int i = 0; i < tempValue.length; i++) {
                        tempBytes[i] = tempValue[i].toBytes();
                    }
                }
                newMap.put(key, tempBytes);
            } else if (Arrays.asList(dim2).contains(key)) {// Element[][] to byte[][][]
                Element[][] tempValue = (Element[][]) value;
                byte[][][] tempBytes = new byte[tempValue.length][][];
                for (int i = 0; i < tempValue.length; i++) {
                    tempBytes[i] = new byte[tempValue[i].length][];
                    // 如果是G1
                    if (Arrays.asList(G1_itemss).contains(key)) {
                        for (int j = 0; j < tempValue[i].length; j++) {
                            tempBytes[i][j] = ElementtoBytesCompressed(tempValue[i][j]);
                        }
                    } else {
                        for (int j = 0; j < tempValue[i].length; j++) {
                            tempBytes[i][j] = tempValue[i][j].toBytes();
                        }
                    }

                }

                newMap.put(key, tempBytes);
            } else { // Element
                if (Arrays.asList(G1_item).contains(key)) {
                    newMap.put(key, ElementtoBytesCompressed((Element) value));
                } else {
                    newMap.put(key, ((Element) value).toBytes());
                }
            }
        });
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(newMap);
        }
        byte[] compressed_bytes = byteArrayOutputStream.toByteArray();
        // 双重压缩
        System.out.println("压缩前:"+((float)compressed_bytes.length)/1024);
        compressed_bytes = SP.compress(compressed_bytes);
        System.out.println("压缩后:" + ((float)compressed_bytes.length)/1024);
        return compressed_bytes;
    }

    public static HashMap<String, Object> byteArrayToHashMap(byte[] bytes) throws IOException, ClassNotFoundException {
        // 先解压一层
        System.out.println("压缩前:" + ((float) bytes.length) / 1024);
        bytes = SP.decompress(bytes);
        System.out.println("压缩后:" + ((float) bytes.length) / 1024);

        String[] normal_item = { "i", "j", "l", "k","m","n" };
        String[] G1_item = { "g1", "g2", "h1", "h2", "_g", "W1", "W2", "W1Prime", "W2Prime", "_hHat", "E1", "E2", "F1",
                "F2", "J1",
                "J2", "I1", "I2", "I3", "I4", "E1Prime", "E2Prime", "_h" };
        String[] Zr_item = { "cc", "alpha1", "alpha2", "beta1", "beta2", "x", "y", "c0", "MSP1", "MU", "M2SP" };
        String[] GT_item = { "H", "HPrime", "tildeH" };

        // 一维数组
        String[] Zr_items = { "c", "z" }; // 一维的Zr
        String[] GT_items = { "Theta" }; // 一维的GT
        String[] G1_items = { "Gamma1", "Gamma2" };

        // 二维数组
        String[] Zr_itemss = { "c1", "c2", "z1", "z2" };
        String[] G1_itemss = { "A", "K", "gamma1", "_harr" }; // A是m*n的二维G1
        String[] GT_itemss = { "B", "M", "L", "gamma2", "gamma3", "LP", "LPrime" }; // B，M是二维GT
        String[] C_item = { "C", "D" }; // 一行三个元素，分别是G1，G1, GT

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        try (ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            @SuppressWarnings("unchecked")
            HashMap<String, Object> map = (HashMap<String, Object>) objectInputStream.readObject();

            HashMap<String, Object> newMap = new HashMap<>();
            // for (String key : map.keySet())
            map.forEach((key, value) -> {
                // Object value = map.get(key);
                if (Arrays.asList(normal_item).contains(key)) { // int
                    newMap.put(key, value);
                } else if (Arrays.asList(G1_item).contains(key)) { // byte[] to Element
                    newMap.put(key, BytesCompressedToElement((byte[]) value).getImmutable());
                } else if (Arrays.asList(Zr_item).contains(key)) {
                    newMap.put(key, Zr.newElementFromBytes((byte[]) value).getImmutable());
                } else if (Arrays.asList(GT_item).contains(key)) {
                    newMap.put(key, Gt.newElementFromBytes((byte[]) value).getImmutable());
                } else if (Arrays.asList(Zr_items).contains(key)) {// byte[][] to Element[]
                    byte[][] tempValue = (byte[][]) value;
                    Element[] tempElements = new Element[tempValue.length];
                    for (int i = 0; i < tempValue.length; i++) {
                        tempElements[i] = Zr.newElementFromBytes(tempValue[i]).getImmutable();
                    }
                    newMap.put(key, tempElements);
                } else if (Arrays.asList(G1_items).contains(key)) {
                    byte[][] tempValue = (byte[][]) value;
                    Element[] tempElements = new Element[tempValue.length];
                    for (int i = 0; i < tempValue.length; i++) {
                        // tempElements[i] = G1.newElementFromBytes(tempValue[i]).getImmutable();
                        tempElements[i] = BytesCompressedToElement(tempValue[i]).getImmutable();
                    }
                    newMap.put(key, tempElements);
                } else if (Arrays.asList(GT_items).contains(key)) {
                    byte[][] tempValue = (byte[][]) value;
                    Element[] tempElements = new Element[tempValue.length];
                    for (int i = 0; i < tempValue.length; i++) {
                        tempElements[i] = Gt.newElementFromBytes(tempValue[i]).getImmutable();
                    }
                    newMap.put(key, tempElements);
                } else if (Arrays.asList(G1_itemss).contains(key)) {// byte[][][] to Element[][]
                    byte[][][] tempValue = (byte[][][]) value;
                    Element[][] tempElements = new Element[tempValue.length][];
                    for (int i = 0; i < tempValue.length; i++) {
                        tempElements[i] = new Element[tempValue[i].length];
                        for (int j = 0; j < tempValue[i].length; j++) {
                            // tempElements[i][j] = G1.newElementFromBytes(tempValue[i][j]).getImmutable();
                            tempElements[i][j] = BytesCompressedToElement(tempValue[i][j]).getImmutable();
                        }
                    }
                    newMap.put(key, tempElements);
                } else if (Arrays.asList(Zr_itemss).contains(key)) {
                    byte[][][] tempValue = (byte[][][]) value;
                    Element[][] tempElements = new Element[tempValue.length][];
                    for (int i = 0; i < tempValue.length; i++) {
                        tempElements[i] = new Element[tempValue[i].length];
                        for (int j = 0; j < tempValue[i].length; j++) {
                            tempElements[i][j] = Zr.newElementFromBytes(tempValue[i][j]).getImmutable();
                        }
                    }
                    newMap.put(key, tempElements);
                } else if (Arrays.asList(GT_itemss).contains(key)) {
                    byte[][][] tempValue = (byte[][][]) value;
                    Element[][] tempElements = new Element[tempValue.length][];
                    for (int i = 0; i < tempValue.length; i++) {
                        tempElements[i] = new Element[tempValue[i].length];
                        for (int j = 0; j < tempValue[i].length; j++) {
                            tempElements[i][j] = Gt.newElementFromBytes(tempValue[i][j]).getImmutable();
                        }
                    }
                    newMap.put(key, tempElements);
                } else if (Arrays.asList(C_item).contains(key)) {
                    byte[][][] tempValue = (byte[][][]) value;
                    Element[][] tempElements = new Element[tempValue.length][];
                    for (int i = 0; i < tempValue.length; i++) {
                        tempElements[i] = new Element[tempValue[i].length];
                        tempElements[i][0] = G1.newElementFromBytes(tempValue[i][0]).getImmutable();
                        tempElements[i][1] = G1.newElementFromBytes(tempValue[i][1]).getImmutable();
                        tempElements[i][2] = Gt.newElementFromBytes(tempValue[i][2]).getImmutable();
                    }
                    newMap.put(key, tempElements);
                } else {
                    System.out.println("KEY ERROR!");
                }
            });
            return newMap;
        }
    }

    public static HashMap<String, Object> setUP(byte[] params_bytes,
            byte[] SK_bytes) throws ClassNotFoundException, IOException {
        HashMap<String, Object> publicParams = new HashMap<>();
        publicParams = SP.byteArrayToHashMap(params_bytes);
        SK = SP.byteArrayToHashMap(SK_bytes);
        _g = (Element) publicParams.get("_g");
        g1 = (Element) publicParams.get("g1");
        g2 = (Element) publicParams.get("g2");
        h1 = (Element) publicParams.get("h1");
        h2 = (Element) publicParams.get("h2");
        H = (Element) publicParams.get("H");
        W1 = (Element) publicParams.get("W1");
        W2 = (Element) publicParams.get("W2");
        W1Prime = (Element) publicParams.get("W1Prime");
        W2Prime = (Element) publicParams.get("W2Prime");
        Gamma1 = (Element[]) publicParams.get("Gamma1");
        Gamma2 = (Element[]) publicParams.get("Gamma2");
        A = (Element[][]) publicParams.get("A");
        B = (Element[][]) publicParams.get("B");
        C = (Element[][]) publicParams.get("C");
        D = (Element[][]) publicParams.get("D");
        return publicParams;
    }

    public static HashMap<String, Object> setUP(int m, int n) throws IOException {
        SP.m = m;
        SP.n = n;
        serviceM = new Element[m][n];
        Element _h;
        // for (int i = 0; i < m; i++) {
        // for (int j = 0; j < n; j++) {
        // serviceM[i][j] = Gt.newRandomElement();
        // }
        // }
        // 读取service服务矩阵
        Element[][] serviceM = GetServiceM.loadGTMatrixFromFile("ServiceM.dat");

        _g = G1.newRandomElement().getImmutable();
        _h = G1.newRandomElement().getImmutable();
        g1 = G1.newRandomElement().getImmutable();
        g2 = G1.newRandomElement().getImmutable();
        h1 = G1.newRandomElement().getImmutable();
        h2 = G1.newRandomElement().getImmutable();

        Element a1 = Zr.newRandomElement().getImmutable();
        Element a2 = Zr.newRandomElement().getImmutable();
        Element b1 = Zr.newRandomElement().getImmutable();
        Element b2 = Zr.newRandomElement().getImmutable();
        Element x = Zr.newRandomElement().getImmutable();
        Element y = Zr.newRandomElement().getImmutable();

        H = bp.pairing(_g, _h).getImmutable();
        W1 = g1.powZn(a1).getImmutable();
        W2 = g2.powZn(a2).getImmutable();
        W1Prime = h1.powZn(b1).getImmutable();
        W2Prime = h2.powZn(b2).getImmutable();

        Gamma1 = new Element[m];
        Gamma2 = new Element[n];
        for (int i = 0; i < m; i++) {
            Gamma1[i] = g1.powZn((a1.add(Zr.newElement(i))).invert()).getImmutable();
        }
        for (int j = 0; j < n; j++) {
            Gamma2[j] = h1.powZn((b1.add(Zr.newElement(j))).invert()).getImmutable();
        }

        A = new Element[m][n];
        B = new Element[m][n];
        C = new Element[m][3];
        D = new Element[n][3];
        ExecutorService executor = Executors.newFixedThreadPool(16);
        for (int rowi = 0; rowi < m; rowi++) {
            final int i = rowi;
            executor.submit(() -> {
                for (int j = 0; j < n; j++) {
                    Element temp1 = g1.powZn(Zr.newElement(i));
                    Element temp2 = h1.powZn(Zr.newElement(j));
                    Element temp3 = g2.powZn(x.powZn(Zr.newElement(i)));
                    Element temp4 = h2.powZn(y.powZn(Zr.newElement(j)));
                    A[i][j] = temp1.mul(temp2).mul(temp3).mul(temp4).getImmutable();
                    B[i][j] = bp.pairing(A[i][j], _h).mul(serviceM[i][j]).getImmutable();
                }
                Element xi = x.powZn(Zr.newElement(i));
                C[i][0] = g2.powZn(xi).getImmutable();
                C[i][1] = g2.powZn(a2.add(xi).invert()).getImmutable();
                C[i][2] = H.powZn(xi).getImmutable();
            });
        }
        executor.shutdown(); // 关闭线程池
        try {
            executor.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // for (int i = 0; i < m; i++) {
        // Element xi = x.powZn(Zr.newElement(i));
        // C[i][0] = g2.powZn(xi).getImmutable();
        // C[i][1] = g2.powZn(a2.add(xi).invert()).getImmutable();
        // C[i][2] = H.powZn(xi).getImmutable();
        // }
        for (int j = 0; j < n; j++) {
            Element yj = y.powZn(Zr.newElement(j));
            D[j][0] = h2.powZn(yj).getImmutable();
            D[j][1] = h2.powZn(b2.add(yj).invert()).getImmutable();
            D[j][2] = H.powZn(yj).getImmutable();
        }

        SK.put("alpha1", a1);
        SK.put("alpha2", a2);
        SK.put("beta1", b1);
        SK.put("beta2", b2);
        SK.put("x", x);
        SK.put("y", y);
        SK.put("_h", _h);

        byte[] SK_bytes = SP.hashMapToByteArray(SK);
        String SK_file_name = String.valueOf(n) + "_" + String.valueOf(n) + "_" + "SK_bytes.bin";
        Server.writeBytesToFile(SK_bytes, SK_file_name);
        System.out.println(SK_file_name + "finished!");

        HashMap<String, Object> publicParams = new HashMap<>();
        publicParams.put("m", m);
        publicParams.put("n", n);
        publicParams.put("_g", _g);
        publicParams.put("g1", g1);
        publicParams.put("g2", g2);
        publicParams.put("h1", h1);
        publicParams.put("h2", h2);
        publicParams.put("H", H);
        publicParams.put("W1", W1);
        publicParams.put("W2", W2);
        publicParams.put("W1Prime", W1Prime);
        publicParams.put("W2Prime", W2Prime);
        publicParams.put("Gamma1", Gamma1);
        publicParams.put("Gamma2", Gamma2);
        publicParams.put("A", A);
        publicParams.put("B", B);
        publicParams.put("C", C);
        publicParams.put("D", D);
        System.out.println("SetUp Finished!");
        return publicParams;
    }

    public static byte[] sha160(String input) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("sha");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return md.digest(input.getBytes());
    }

    public boolean verProofPIU(HashMap<String, Object> PI) {
        Element E1Prime = (Element) PI.get("E1Prime");
        Element E2Prime = (Element) PI.get("E2Prime");
        Element E1 = (Element) PI.get("E1");
        Element E2 = (Element) PI.get("E2");
        Element MU = (Element) PI.get("MU");
        Element[] c = (Element[]) PI.get("c");
        Element[] z = (Element[]) PI.get("z");
        Element[] Theta = (Element[]) PI.get("Theta");
        Element I1 = (Element) PI.get("I1");
        Element I2 = (Element) PI.get("I2");
        Element I3 = (Element) PI.get("I3");
        Element I4 = (Element) PI.get("I4");
        Element F1 = (Element) PI.get("F1");
        Element F2 = (Element) PI.get("F2");
        Element J1 = (Element) PI.get("J1");
        Element J2 = (Element) PI.get("J2");
        int tmp_k = (int) PI.get("k");
        Element k = Zr.newElement(tmp_k);
        int tmp_l = (int) PI.get("l");
        Element l = Zr.newElement(tmp_l);
        Element[] cPrime = new Element[12];
        // byte[] sha1Bytes;

        ExecutorService executor = Executors.newFixedThreadPool(16);
        executor.submit(() -> {
            byte[] sha1Bytes = sha160(E1.toString() + E1Prime.toString() + MU.toString());
            cPrime[0] = Zr.newElementFromHash(sha1Bytes, 0, sha1Bytes.length).getImmutable();
        });

        executor.submit(() -> {
            byte[] sha1Bytes = sha160(E2.toString() + E2Prime.toString() + MU.toString());
            cPrime[1] = Zr.newElementFromHash(sha1Bytes, 0, sha1Bytes.length).getImmutable();
        });
        executor.submit(() -> {
            byte[] sha1Bytes = sha160(Theta[4].toString() + Theta[5].toString() + MU.toString());
            cPrime[2] = Zr.newElementFromHash(sha1Bytes, 0, sha1Bytes.length).getImmutable();
        });
        executor.submit(() -> {
            byte[] sha1Bytes = sha160(Theta[6].toString() + Theta[7].toString() + MU.toString());
            cPrime[3] = Zr.newElementFromHash(sha1Bytes, 0, sha1Bytes.length).getImmutable();
        });
        executor.submit(() -> {
            byte[] sha1Bytes = sha160(Theta[8].toString() + Theta[14].toString() + MU.toString());
            cPrime[4] = Zr.newElementFromHash(sha1Bytes, 0, sha1Bytes.length).getImmutable();
        });
        executor.submit(() -> {
            byte[] sha1Bytes = sha160(Theta[9].toString() + Theta[15].toString() + MU.toString());
            cPrime[5] = Zr.newElementFromHash(sha1Bytes, 0, sha1Bytes.length).getImmutable();
        });
        executor.submit(() -> {
            byte[] sha1Bytes = sha160(Theta[0].toString() + Theta[1].toString() + MU.toString());
            cPrime[6] = Zr.newElementFromHash(sha1Bytes, 0, sha1Bytes.length).getImmutable();
        });
        executor.submit(() -> {
            byte[] sha1Bytes = sha160(Theta[2].toString() + Theta[3].toString() + MU.toString());
            cPrime[7] = Zr.newElementFromHash(sha1Bytes, 0, sha1Bytes.length).getImmutable();
        });

        executor.submit(() -> {
            byte[] sha1Bytes = sha160(Theta[5].toString() + Theta[10].toString() + MU.toString());
            cPrime[8] = Zr.newElementFromHash(sha1Bytes, 0, sha1Bytes.length).getImmutable();
        });

        executor.submit(() -> {
            byte[] sha1Bytes = sha160(Theta[7].toString() + Theta[11].toString() + MU.toString());
            cPrime[9] = Zr.newElementFromHash(sha1Bytes, 0, sha1Bytes.length).getImmutable();
        });
        executor.submit(() -> {
            byte[] sha1Bytes = sha160(Theta[8].toString() + Theta[12].toString() + MU.toString());
            cPrime[10] = Zr.newElementFromHash(sha1Bytes, 0, sha1Bytes.length).getImmutable();
        });
        executor.submit(() -> {
            byte[] sha1Bytes = sha160(Theta[9].toString() + Theta[13].toString() + MU.toString());
            cPrime[11] = Zr.newElementFromHash(sha1Bytes, 0, sha1Bytes.length).getImmutable();
        });
        // cPrime[11]

        executor.submit(() -> {
            Element value1 = E1Prime;
            Element value2 = _g.powZn(z[0]).mul(g1.powZn(z[1])).mul(E1.powZn(c[0]));
            if (!value1.equals(value2))
                return false;
            return null;
        });

        executor.submit(() -> {
            Element value1 = E2Prime;
            Element value2 = _g.powZn(z[2]).mul(h1.powZn(z[3])).mul(E2.powZn(c[1]));
            if (!value1.equals(value2))
                return false;
            return null;
        });

        executor.submit(() -> {
            Element value1 = Theta[4].mul(Theta[5]);
            Element value2 = bp.pairing(g1, I4).powZn(z[5]).mul(bp.pairing(g1, g1).powZn(z[4]))
                    .mul(bp.pairing(W1.invert(), I1)).powZn(c[2]);
            value2 = bp.pairing(g1, I1).powZn(z[5]).mul(bp.pairing(g1, g1).powZn(z[4]))
                    .mul(bp.pairing(W1.invert(), I1).powZn(c[2]));
            if (!value1.equals(value2))
                return false;
            return null;
        });

        executor.submit(() -> {
            Element value1 = Theta[6].mul(Theta[7]);
            Element value2 = bp.pairing(h1, I2).powZn(z[6]).mul(bp.pairing(h1, h1).powZn(z[7]))
                    .mul(bp.pairing(W1Prime.invert(), I2).powZn(c[3]));
            if (!value1.equals(value2))
                return false;
            return null;
        });

        executor.submit(() -> {
            Element value1 = Theta[8].mul(Theta[14]);
            Element value2 = bp.pairing(g1, g1).powZn(z[8])
                    .mul(bp.pairing(g1, I3).powZn(z[9]))
                    .mul(bp.pairing(I3, W1.invert()).mul(bp.pairing(g1, I3).invert().powZn(l)).powZn(c[4]));
            if (!value1.equals(value2))
                return false;
            return null;
        });

        executor.submit(() -> {
            Element value1 = Theta[9].mul(Theta[15]);
            Element value2 = bp.pairing(h1, h1).powZn(z[10])
                    .mul(bp.pairing(h1, I4).powZn(z[11]))
                    .mul(bp.pairing(I4, W1Prime.invert()).mul(bp.pairing(h1, I4).powZn(k).invert()).powZn(c[5]));
            if (!value1.equals(value2))
                return false;
            return null;
        });

        executor.submit(() -> {
            Element value1 = Theta[0].mul(Theta[1]);
            Element value2 = bp.pairing(_g, F2).powZn(z[12]).mul(bp.pairing(g2, g2).powZn(z[13]))
                    .mul(bp.pairing(F1.mul(W2), F2).powZn(c[6]));
            if (!value1.equals(value2))
                return false;
            return null;
        });

        executor.submit(() -> {
            Element value1 = Theta[2].mul(Theta[3]);
            Element value2 = bp.pairing(_g, J2).powZn(z[14])
                    .mul(bp.pairing(h2, h2).powZn(z[15]))
                    .mul(bp.pairing(J1.mul(W2Prime), J2).powZn(c[7]));
            if (!value1.equals(value2))
                return false;
            return null;
        });

        executor.submit(() -> {
            Element value1 = Theta[5].mul(Theta[10]);
            Element value2 = bp.pairing(g1, g1).powZn(z[16])
                    .mul(bp.pairing(_g, I1).powZn(z[17]))
                    .mul(bp.pairing(E1.mul(W1), I1).powZn(c[8]));
            if (!value1.equals(value2))
                return false;
            return null;
        });

        executor.submit(() -> {
            Element value1 = Theta[7].mul(Theta[11]);
            Element value2 = bp.pairing(h1, h1).powZn(z[18])
                    .mul(bp.pairing(_g, I2).powZn(z[19]))
                    .mul(bp.pairing(E2.mul(W1Prime), I2).powZn(c[9]));
            if (!value1.equals(value2))
                return false;
            return null;
        });

        executor.submit(() -> {
            Element value1 = Theta[8].mul(Theta[12]);
            Element value2 = bp.pairing(g1, g1).powZn(z[20])
                    .mul(bp.pairing(_g, I3).powZn(z[21]))
                    .mul(bp.pairing(E1.mul(g1.powZn(l)).mul(W1), I3).powZn(c[10]));
            if (!value1.equals(value2))
                return false;
            return null;
        });

        executor.submit(() -> {
            Element value1 = Theta[9].mul(Theta[13]);
            Element value2 = bp.pairing(h1, h1).powZn(z[22])
                    .mul(bp.pairing(_g, I4).powZn(z[23]))
                    .mul(bp.pairing(E2.mul(h1.powZn(k)).mul(W1Prime), I4).powZn(c[11]));
            if (!value1.equals(value2))
                return false;
            return null;
        });

        executor.shutdown(); // 关闭线程池
        try {
            executor.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // System.err.println("Theta ok!!");
        for (int i = 0; i < 12; i++) {
            if (!cPrime[i].equals(c[i])) {
                return false;
            }
        }

        return true;
    }

    public HashMap<String, Object> genProofPI1() {
        Element _h = (Element) SK.get("_h");
        Element _hPrime = G1.newRandomElement().getImmutable();
        Element MSP1 = Zr.newRandomElement().getImmutable();
        Element HPrime = bp.pairing(_g, _hPrime).getImmutable();
        byte[] sha1Bytes = sha160(H.toString() + HPrime.toString() + MSP1.toString());
        Element c = Zr.newElementFromHash(sha1Bytes, 0, sha1Bytes.length);
        Element hHat = _hPrime.mul(_h.powZn(c).invert());
        PI1.put("H", H);
        PI1.put("HPrime", HPrime);
        PI1.put("cc", c);
        PI1.put("_hHat", hHat);
        PI1.put("MSP1", MSP1);
        return PI1;
    }

    public HashMap<String, Object> genProofPI2(HashMap<String, Object> OmegaU) {
        int lRow = (int) OmegaU.get("l");
        int kCol = (int) OmegaU.get("k");
        Element E1 = (Element) OmegaU.get("E1");
        Element E2 = (Element) OmegaU.get("E2");
        Element F1 = (Element) OmegaU.get("F1");
        Element J1 = (Element) OmegaU.get("J1");
        Element x = (Element) SK.get("x");
        Element y = (Element) SK.get("y");
        Element _h = (Element) SK.get("_h");
        Element[] w = new Element[lRow];
        Element[] v = new Element[kCol];
        for (int t = 0; t < lRow; t++) {
            w[t] = Zr.newRandomElement().getImmutable();
        }
        for (int t = 0; t < kCol; t++) {
            v[t] = Zr.newRandomElement().getImmutable();
        }
        Element _th = G1.newRandomElement().getImmutable();
        Element M2SP = Zr.newRandomElement().getImmutable();
        Element[][] L = new Element[lRow][kCol];
        Element[][] K = new Element[lRow][kCol];
        Element[][] gamma1 = new Element[lRow][kCol];
        Element[][] gamma2 = new Element[lRow][kCol];
        Element[][] gamma3 = new Element[lRow][kCol];
        Element[][] c1 = new Element[lRow][kCol];
        Element[][] c2 = new Element[lRow][kCol];
        Element[][] z1 = new Element[lRow][kCol];
        Element[][] z2 = new Element[lRow][kCol];
        Element[][] _harr = new Element[lRow][kCol];
        Element[][] LPrime = new Element[lRow][kCol];
        ExecutorService executor = Executors.newFixedThreadPool(16);
        for (int row = 0; row < lRow; row++) {
            for (int col = 0; col < kCol; col++) {
                final int mju = row;
                final int nv = col;
                executor.submit(() -> {
                    {
                        Element valueK;
                        Element valueL;
                        Element valueG1;
                        Element valueG2;
                        Element valueG3;
                        Element valuec1;
                        Element valuec2;
                        Element valuez1;
                        Element valuez2;
                        Element Element_mju;
                        Element Element_nv;
                        byte[] sha1Bytes;
                        Element_mju = Zr.newElement(mju);
                        Element_nv = Zr.newElement(nv);
                        valueK = E1.mul(g1.powZn(Element_mju))
                                .mul(E2)
                                .mul(h1.powZn(Element_nv))
                                .mul(F1.powZn(x.powZn(Element_mju)))
                                .mul(J1.powZn(y.powZn(Element_nv)));
                        valueL = bp.pairing(valueK, _h);
                        valueG1 = F1.powZn(w[mju]).mul(J1.powZn(v[nv])).getImmutable();
                        valueG2 = bp.pairing(C[mju][1], g2).powZn(w[mju].negate()).getImmutable();
                        valueG3 = (bp.pairing(D[nv][1], h2).powZn(v[nv].negate())).getImmutable();
                        sha1Bytes = sha160(
                                valueK.toString() + valueG1.toString() + valueG2.toString() + valueG3.toString()
                                        + M2SP.toString());
                        valuec1 = Zr.newElementFromHash(sha1Bytes, 0, sha1Bytes.length).getImmutable();
                        sha1Bytes = sha160(valueL.toString() + H.toString() + M2SP.toString());
                        valuec2 = Zr.newElementFromHash(sha1Bytes, 0, sha1Bytes.length).getImmutable();
                        valuez1 = (w[mju].sub(valuec1.mul(x.powZn(Element_mju)))).getImmutable();
                        valuez2 = (v[nv].sub(valuec1.mul(y.powZn(Element_nv)))).getImmutable();
                        _harr[mju][nv] = _th.mul(_h.powZn(valuec2.negate())).getImmutable();
                        LPrime[mju][nv] = bp.pairing(valueK, _th).getImmutable();
                        K[mju][nv] = valueK;
                        L[mju][nv] = valueL;
                        gamma1[mju][nv] = valueG1;
                        gamma2[mju][nv] = valueG2;
                        gamma3[mju][nv] = valueG3;
                        c1[mju][nv] = valuec1;
                        c2[mju][nv] = valuec2;
                        z1[mju][nv] = valuez1;
                        z2[mju][nv] = valuez2;
                    }

                });
            }
        }
        executor.shutdown(); // 关闭线程池
        try {
            executor.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Element tildeH = bp.pairing(_g, _th).getImmutable();
        PI2.put("K", K);
        PI2.put("L", L);
        PI2.put("gamma1", gamma1);
        PI2.put("gamma2", gamma2);
        PI2.put("gamma3", gamma3);
        PI2.put("c1", c1);
        PI2.put("c2", c2);
        PI2.put("z1", z1);
        PI2.put("z2", z2);
        PI2.put("_harr", _harr);
        PI2.put("LPrime", LPrime);
        PI2.put("M2SP", M2SP);
        PI2.put("tildeH", tildeH);
        return PI2;
    }

    public static boolean compareHashmap(HashMap<String, Object> mp1, HashMap<String, Object> mp2) {
        String[] intIterm = new String[] { "i", "j", "l", "k" };
        String[] dim1 = new String[] { "Theta", "c", "z", "Gamma1", "Gamma2" };
        String[] dim2 = new String[] { "K", "L", "gamma1", "gamma2", "gamma3", "c1", "c2", "z1", "z2", "_harr",
                "LPrime", "A", "B", "C", "D" };

        if (mp1.size() != mp2.size()) {
            // return false;
        }

        for (HashMap.Entry<String, Object> entry : mp1.entrySet()) {
            String key = entry.getKey();
            if (Arrays.asList(dim1).contains(key)) {
                Element[] v1 = (Element[]) mp1.get(key);
                Element[] v2 = (Element[]) mp2.get(key);
                if (!Arrays.equals(v2, v1)) {
                    return false;
                }
            } else if (Arrays.asList(dim2).contains(key)) {
                Element[][] v1 = (Element[][]) mp1.get(key);
                Element[][] v2 = (Element[][]) mp2.get(key);
                if (!Arrays.deepEquals(v2, v1)) {
                    return false;
                }
            } else if (Arrays.asList(intIterm).contains(key)) {
                int v1 = (int) mp1.get(key);
                int v2 = (int) mp2.get(key);
                if (v1 != v2) {
                    return false;
                }
            } else {
                Element v1 = (Element) mp1.get(key);
                Element v2 = (Element) mp2.get(key);
                if (!v1.isEqual(v2)) {
                    return false;
                }
            }
        }

        return true;
    }

    // 压缩
    public static byte[] compress(byte[] data) {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();

        byte[] buffer = new byte[1024];
        int compressedDataLength;

        // 创建一个输出流来保存压缩的数据
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);

        // 开始压缩
        while (!deflater.finished()) {
            compressedDataLength = deflater.deflate(buffer);
            outputStream.write(buffer, 0, compressedDataLength);
        }

        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outputStream.toByteArray();
    }

    // 解压byte
    public static byte[] decompress(byte[] compressedData) {
        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);

        byte[] buffer = new byte[1024];
        int decompressedDataLength;

        // 创建一个输出流来保存解压缩的数据
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(compressedData.length);

        try {
            // 开始解压
            while (!inflater.finished()) {
                decompressedDataLength = inflater.inflate(buffer);
                outputStream.write(buffer, 0, decompressedDataLength);
            }
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return outputStream.toByteArray();
    }

    // public static byte[] gzipCompress(byte[] data) throws IOException {
    // ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    // try (GZIPOutputStream gzipOut = new GZIPOutputStream(byteOut)) {
    // gzipOut.write(data);
    // }
    // return byteOut.toByteArray();
    // }

    // // GZIP 解压缩方法
    // public static byte[] gzipDecompress(byte[] compressedData) throws IOException
    // {
    // ByteArrayInputStream byteIn = new ByteArrayInputStream(compressedData);
    // GZIPInputStream gzipIn = new GZIPInputStream(byteIn);
    // ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

    // byte[] buffer = new byte[1024];
    // int len;
    // while ((len = gzipIn.read(buffer)) != -1) {
    // byteOut.write(buffer, 0, len);
    // }
    // gzipIn.close();
    // return byteOut.toByteArray();
    // }
    // public static byte[] compress(byte[] data, int compressionLevel) throws
    // ZstdException {
    // if (data == null || data.length == 0) {
    // throw new IllegalArgumentException("输入数据不能为空");
    // }
    // if (compressionLevel < 1 || compressionLevel > 22) {
    // throw new IllegalArgumentException("压缩级别必须在1到22之间");
    // }
    // return Zstd.compress(data, compressionLevel);
    // }

    // public static byte[] decompress(byte[] compressedData) throws ZstdException {
    // if (compressedData == null || compressedData.length == 0) {
    // throw new IllegalArgumentException("压缩数据不能为空");
    // }

    // // 获取解压缩后的大小
    // long decompressedSize = Zstd.decompressedSize(compressedData);
    // if (decompressedSize == 0) {
    // throw new ZstdException(decompressedSize, "无法获取解压后的大小，数据可能已损坏或格式不正确");
    // }

    // byte[] decompressed = new byte[(int) decompressedSize];
    // long result = Zstd.decompress(decompressed, compressedData);
    // if (Zstd.isError(result)) {
    // throw new ZstdException(result, "解压缩失败: " + Zstd.getErrorName(result));
    // }

    // return decompressed;
    // }
    // public static byte[] compress(byte[] data, int compressionLevel) throws
    // IOException {
    // if (data == null || data.length == 0) {
    // throw new IllegalArgumentException("输入数据不能为空");
    // }
    // if (compressionLevel < 1 || compressionLevel > 9) {
    // throw new IllegalArgumentException("压缩级别必须在1到9之间");
    // }

    // ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    // LZMA2Options options = new LZMA2Options();
    // options.setPreset(compressionLevel); // 设置压缩级别

    // try (XZOutputStream xzOut = new XZOutputStream(byteOut, options)) {
    // xzOut.write(data);
    // }

    // return byteOut.toByteArray();
    // }

    // public static byte[] decompress(byte[] compressedData) throws IOException {
    // if (compressedData == null || compressedData.length == 0) {
    // throw new IllegalArgumentException("压缩数据不能为空");
    // }

    // ByteArrayInputStream byteIn = new ByteArrayInputStream(compressedData);
    // ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

    // try (XZInputStream xzIn = new XZInputStream(byteIn)) {
    // byte[] buffer = new byte[1024];
    // int n;
    // while ((n = xzIn.read(buffer)) != -1) {
    // byteOut.write(buffer, 0, n);
    // }
    // }

    // return byteOut.toByteArray();
    // }

    public static void main(String[] args) throws ClassNotFoundException, IOException {
        SP server = new SP();
        int n = 10;
        int m = 10;
        int l = 3;
        int k = 3;
        HashMap<String, Object> params = SP.setUP(m, n);

        byte[] params_bytes = SP.hashMapToByteArray(params); // 假设params是定义好的
        System.out.println("param 压缩后长度: " + params_bytes.length);
        HashMap<String, Object> params1 = SP.byteArrayToHashMap(params_bytes);

        User.readParams(params1);
        User u1 = new User();
        HashMap<String, Object> pi1 = server.genProofPI1();
        byte[] PI_bytes;
        PI_bytes = SP.hashMapToByteArray(pi1);
        System.out.println("PI1 压缩后长度: " + PI_bytes.length);
        HashMap<String, Object> pi_1 = SP.byteArrayToHashMap(PI_bytes);
        boolean pi1_result = u1.verProof1(pi_1);
        System.out.println("pi1_result: " + pi1_result);

        // 生成验证piu
        HashMap<String, Object> piu = u1.genProofPIU(1, 1, l, k);
        PI_bytes = SP.hashMapToByteArray(piu);
        // TODO：因为用 字节流压缩的原因，所以会有些微小变化
        // 如果不用双重压缩的话，
        System.out.println("PIU 压缩后长度: " + PI_bytes.length);
        HashMap<String, Object> piu_1 = SP.byteArrayToHashMap(PI_bytes);
        boolean piu_result = server.verProofPIU(piu_1);
        System.out.println("piu_result:" + piu_result);

        // // 生成验证pi2
        HashMap<String, Object> pi2 = server.genProofPI2(piu);
        PI_bytes = SP.hashMapToByteArray(pi2);
        System.out.println("PI2 压缩前长度: " + PI_bytes.length);
        HashMap<String, Object> pi2_1 = SP.byteArrayToHashMap(PI_bytes);
        boolean pi2_result = u1.verProofPI2(l, k, pi2_1);
        System.out.println("pi2_result: " + pi2_result);
    }
}
