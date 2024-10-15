package com.team.test;


import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;

public class User {
    private Element[] r=new Element[10];
    private Element[] s=new Element[10];
    public HashMap<String,Object> PIU=new HashMap<>();

    public static final Pairing bp= PairingFactory.getPairing("a.properties");
    public static final Field G1=bp.getG1();
    public static final Field Gt=bp.getGT();
    public static final Field Zr=bp.getZr();
    public static Element  _g,g1,g2,h1,h2,H,W1,W2, W1Prime, W2Prime;
    public static Element[] Gamma1; //m个
    public static Element[] Gamma2; //n个
    public static Element[][] A;    //m*n
    public static Element[][] B;    //m*n
    public static Element[][] C;    //m*3
    public static Element[][] D;    //n*3

    public static void readParams(HashMap<String,Object> Params)  {
        _g=(Element)Params.get("_g");
        g1=(Element)Params.get("g1");
        g2=(Element)Params.get("g2");
        h1=(Element)Params.get("h1");
        h2=(Element)Params.get("h2");
        H=(Element)Params.get("H");
        W1=(Element)Params.get("W1");
        W2=(Element)Params.get("W2");
        W1Prime=(Element)Params.get("W1Prime");
        W2Prime=(Element)Params.get("W2Prime");
        Gamma1 =(Element[])Params.get("Gamma1");
        Gamma2 =(Element[])Params.get("Gamma2");
        A=(Element[][])Params.get("A");
        B=(Element[][])Params.get("B");
        C=(Element[][])Params.get("C");
        D=(Element[][])Params.get("D");
    }


    public static byte[] sha160(String input) {
        MessageDigest md;
        try {
            md=MessageDigest.getInstance("sha");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return md.digest(input.getBytes());
    }

    public static byte[] hashMapToByteArray(HashMap<String, Object> map) throws IOException {
        String[] intIterm = new String[] { "i", "j", "l", "k" };
        String[] dim1 = new String[] { "Theta", "c", "z", "Gamma1", "Gamma2" };
        String[] dim2 = new String[] { "K", "L", "gamma1", "gamma2", "gamma3", "c1", "c2", "z1", "z2", "_harr",
                "LPrime", "A", "B", "C", "D" };

        HashMap<String, Object> newMap = new HashMap<>();
        for (String key : map.keySet()) {
            Object value = map.get(key);
            if (Arrays.asList(intIterm).contains(key)) { // int
                newMap.put(key, value);
            } else if (Arrays.asList(dim1).contains(key)) {// Element[] to byte[][]
                Element[] tempValue = (Element[]) value;
                byte[][] tempBytes = new byte[tempValue.length][];
                for (int i = 0; i < tempValue.length; i++) {
                    tempBytes[i] = tempValue[i].toBytes();
                }

                newMap.put(key, tempBytes);
            } else if (Arrays.asList(dim2).contains(key)) {// Element[][] to byte[][][]
                Element[][] tempValue = (Element[][]) value;
                byte[][][] tempBytes = new byte[tempValue.length][][];
                for (int i = 0; i < tempValue.length; i++) {
                    tempBytes[i] = new byte[tempValue[i].length][];
                    for (int j = 0; j < tempValue[i].length; j++) {
                        tempBytes[i][j] = tempValue[i][j].toBytes();
                    }
                }

                newMap.put(key, tempBytes);
            } else { // Element
                newMap.put(key, ((Element) value).toBytes());
            }
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(newMap);
        }

        return byteArrayOutputStream.toByteArray();
    }
    
    public static HashMap<String, Object> byteArrayToHashMap(byte[] bytes) throws IOException, ClassNotFoundException {
        String[] normal_item = { "i", "j", "l", "k" };
        String[] G1_item = { "g1", "g2", "h1", "h2", "_g", "W1", "W2", "W1Prime", "W2Prime", "_hHat", "E1", "E2", "F1",
                "F2", "J1",
                "J2", "I1", "I2", "I3", "I4", "E1Prime", "E2Prime", "_h" };
        String[] Zr_item = { "cc","alpha1", "alpha2", "beta1", "beta2", "x", "y", "c0", "MSP1", "MU", "M2SP" };
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
            for (String key : map.keySet()) {
                Object value = map.get(key);
                if (Arrays.asList(normal_item).contains(key)) { // int
                    newMap.put(key, value);
                } else if (Arrays.asList(G1_item).contains(key)) { // byte[] to Element
                    newMap.put(key, G1.newElementFromBytes((byte[]) value).getImmutable());
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
                        tempElements[i] = G1.newElementFromBytes(tempValue[i]).getImmutable();
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
                            tempElements[i][j] = G1.newElementFromBytes(tempValue[i][j]).getImmutable();
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
            }
            // System.out.println("最后 newMap: " + newMap.get("J1"));
            return newMap;
        }
    }

    public boolean verProof1(HashMap<String, Object> PI){
        // Element H=PI[0];
        // Element HPrime=PI[1];
        // Element c=PI[2];
        // Element _hHat=PI[3];
        // Element MSP1=PI[4];
        Element H = (Element) PI.get("H");
        Element HPrime= (Element) PI.get("HPrime");
        Element c= (Element) PI.get("cc");
        Element _hHat= (Element) PI.get("_hHat");
        Element MSP1= (Element) PI.get("MSP1");
        byte[] sha1Bytes = sha160(H.toString()+HPrime.toString()+MSP1.toString());
        //防止byte太长，需要先哈希一下
        Element tempC = Zr.newElementFromHash(sha1Bytes, 0, sha1Bytes.length);
        Element value=bp.pairing(_g,_hHat).mul(H.powZn(c));
        if (!c.isEqual(tempC))
            System.err.println("c Errot");
        if (!HPrime.isEqual(value))
            System.out.println("HPrime Error");
        return c.isEqual(tempC) && HPrime.isEqual(value);

    }

    public HashMap<String,Object> genProofPIU(int iPoint, int jPoint, int lRow, int kCol){
        for(int t=0;t<10;t++){
            r[t]=Zr.newRandomElement().getImmutable();
            s[t]=Zr.newRandomElement().getImmutable();
        }
        Element _r=Zr.newRandomElement().getImmutable();
        Element _s=Zr.newRandomElement().getImmutable();
        Element MU=Zr.newRandomElement().getImmutable();

        Element E1=_g.powZn(r[0].negate()).mul(g1.powZn(Zr.newElement(iPoint))).getImmutable();
        Element E2=_g.powZn(r[1].negate()).mul(h1.powZn(Zr.newElement(jPoint))).getImmutable();
        Element F1=_g.powZn(r[2]).mul(C[iPoint][0]).getImmutable();
        Element F2=C[iPoint][1].powZn(r[3]).getImmutable();
        Element J1=_g.powZn(r[4]).mul(D[jPoint][0]).getImmutable();
        Element J2=D[jPoint][1].powZn(r[5]).getImmutable();
        Element I1= Gamma1[iPoint].powZn(r[6]).getImmutable();
        Element I2= Gamma2[jPoint].powZn(r[7]).getImmutable();
        Element I3= Gamma1[iPoint+lRow].powZn(r[8]).getImmutable();
        Element I4= Gamma2[jPoint+kCol].powZn(r[9]).getImmutable();

        Element E1Prime=_g.powZn(s[0]).mul(g1.powZn(_r)).getImmutable();
        Element E2Prime=_g.powZn(s[1]).mul(h1.powZn(_s)).getImmutable();

        Element[] theta=new Element[16];
        theta[0]=bp.pairing(_g,F2).powZn(s[2]).getImmutable();
        theta[1]=bp.pairing(g2,g2).powZn(s[3]).getImmutable();
        theta[2]=bp.pairing(_g,J2).powZn(s[4]).getImmutable();
        theta[3]=bp.pairing(h2,h2).powZn(s[5]).getImmutable();
        theta[4]=bp.pairing(g1,I1).powZn(_r).getImmutable();
        theta[5]=bp.pairing(g1,g1).powZn(s[6]).getImmutable();
        theta[6]=bp.pairing(h1,I2).powZn(_s).getImmutable();
        theta[7]=bp.pairing(h1,h1).powZn(s[7]).getImmutable();
        theta[8]=bp.pairing(g1,g1).powZn(s[8]).getImmutable();
        theta[9]=bp.pairing(h1,h1).powZn(s[9]).getImmutable();
        theta[10]=bp.pairing(_g,I1).powZn(s[0]).getImmutable();
        theta[11]=bp.pairing(_g,I2).powZn(s[1]).getImmutable();
        theta[12]=bp.pairing(_g,I3).powZn(s[0]).getImmutable();
        theta[13]=bp.pairing(_g,I4).powZn(s[1]).getImmutable();
        theta[14]=bp.pairing(g1,I3).powZn(_r).getImmutable();
        theta[15]=bp.pairing(h1,I4).powZn(_s).getImmutable();

        Element[] c=new Element[12];
        byte[] sha1Bytes=sha160(E1.toString()+E1Prime.toString()+ MU.toString());
        c[0]=Zr.newElementFromHash(sha1Bytes,0,sha1Bytes.length).getImmutable();

        sha1Bytes=sha160(E2.toString()+E2Prime.toString()+ MU.toString());
        c[1]=Zr.newElementFromHash(sha1Bytes,0,sha1Bytes.length).getImmutable();

        sha1Bytes=sha160(theta[4].toString()+theta[5].toString()+ MU.toString());
        c[2]=Zr.newElementFromHash(sha1Bytes,0,sha1Bytes.length).getImmutable();

        sha1Bytes=sha160(theta[6].toString()+theta[7].toString()+ MU.toString());
        c[3]=Zr.newElementFromHash(sha1Bytes,0,sha1Bytes.length).getImmutable();

        sha1Bytes=sha160(theta[8].toString()+theta[14].toString()+ MU.toString());
        c[4]=Zr.newElementFromHash(sha1Bytes,0,sha1Bytes.length).getImmutable();

        sha1Bytes=sha160(theta[9].toString()+theta[15].toString()+ MU.toString());
        c[5]=Zr.newElementFromHash(sha1Bytes,0,sha1Bytes.length).getImmutable();

        sha1Bytes=sha160(theta[0].toString()+theta[1].toString()+ MU.toString());
        c[6]=Zr.newElementFromHash(sha1Bytes,0,sha1Bytes.length).getImmutable();

        sha1Bytes=sha160(theta[2].toString()+theta[3].toString()+ MU.toString());
        c[7]=Zr.newElementFromHash(sha1Bytes,0,sha1Bytes.length).getImmutable();

        sha1Bytes=sha160(theta[5].toString()+theta[10].toString()+ MU.toString());
        c[8]=Zr.newElementFromHash(sha1Bytes,0,sha1Bytes.length).getImmutable();

        sha1Bytes=sha160(theta[7].toString()+theta[11].toString()+ MU.toString());
        c[9]=Zr.newElementFromHash(sha1Bytes,0,sha1Bytes.length).getImmutable();

        sha1Bytes=sha160(theta[8].toString()+theta[12].toString()+ MU.toString());
        c[10]=Zr.newElementFromHash(sha1Bytes,0,sha1Bytes.length).getImmutable();

        sha1Bytes=sha160(theta[9].toString()+theta[13].toString()+ MU.toString());
        c[11]=Zr.newElementFromHash(sha1Bytes,0,sha1Bytes.length).getImmutable();

        Element[] z=new Element[24];
        z[0]=s[0].add(c[0].mul(r[0])).getImmutable();
        z[1]=_r.sub(c[0].mul(iPoint)).getImmutable();
        z[2]=s[1].add(c[1].mul(r[1])).getImmutable();
        z[3]=_s.sub(c[1].mul(jPoint)).getImmutable();
        z[4]=s[6].add(c[2].mul(r[6])).getImmutable();
        z[5]=_r.sub(c[2].mul(iPoint)).getImmutable();
        z[6]=_s.sub(c[3].mul(jPoint)).getImmutable();
        z[7]=s[7].add(c[3].mul(r[7])).getImmutable();
        z[8]=s[8].add(c[4].mul(r[8])).getImmutable();
        z[9]=_r.sub(c[4].mul(iPoint)).getImmutable();
        z[10]=s[9].add(c[5].mul(r[9])).getImmutable();
        z[11]=_s.sub(c[5].mul(jPoint)).getImmutable();
        z[12]=s[2].sub(c[6].mul(r[2])).getImmutable();
        z[13]=s[3].sub(c[6].mul(r[3])).getImmutable();
        z[14]=s[4].sub(c[7].mul(r[4])).getImmutable();
        z[15]=s[5].sub(c[7].mul(r[5])).getImmutable();
        z[16]=s[6].sub(c[8].mul(r[6])).getImmutable();
        z[17]=s[0].add(c[8].mul(r[0])).getImmutable();
        z[18]=s[7].sub(c[9].mul(r[7])).getImmutable();
        z[19]=s[1].add(c[9].mul(r[1])).getImmutable();
        z[20]=s[8].sub(c[10].mul(r[8])).getImmutable();
        z[21]=s[0].add(c[10].mul(r[0])).getImmutable();
        z[22]=s[9].sub(c[11].mul(r[9])).getImmutable();
        z[23]=s[1].add(c[11].mul(r[1])).getImmutable();

        PIU.put("l",lRow);
        PIU.put("k",kCol);
        PIU.put("E1",E1);
        PIU.put("E2",E2);
        PIU.put("F1",F1);
        PIU.put("F2",F2);
        PIU.put("J1",J1);
        PIU.put("J2",J2);
        PIU.put("I1",I1);
        PIU.put("I2",I2);
        PIU.put("I3",I3);
        PIU.put("I4",I4);
        PIU.put("E1Prime",E1Prime);
        PIU.put("E2Prime",E2Prime);
        PIU.put("Theta",theta);
        PIU.put("c",c);
        PIU.put("z",z);
        PIU.put("MU",MU);
        return PIU;
    }

    public boolean verProofPI2(int lRow,int kCol,HashMap<String,Object> PI2){
        Element[][] K= (Element[][]) PI2.get("K");
        Element[][] L= (Element[][]) PI2.get("L");
        Element[][] gamma1= (Element[][]) PI2.get("gamma1");
        Element[][] gamma2= (Element[][]) PI2.get("gamma2");
        Element[][] gamma3= (Element[][]) PI2.get("gamma3");
        Element[][] c1= (Element[][]) PI2.get("c1");
        Element[][] c2= (Element[][]) PI2.get("c2");
        Element[][] z1= (Element[][]) PI2.get("z1");
        Element[][] z2= (Element[][]) PI2.get("z2");
        Element[][] _harr= (Element[][]) PI2.get("_harr");
        Element tildeH= (Element) PI2.get("tildeH");
        Element[][] LPrime= (Element[][]) PI2.get("LPrime");
        Element msgSP2= (Element) PI2.get("M2SP");

        Element F1= (Element) PIU.get("F1");
        Element J1= (Element) PIU.get("J1");
        Element E1= (Element) PIU.get("E1");
        Element E2= (Element) PIU.get("E2");
        for (int mju=0;mju<lRow;mju++){
            for (int nu = 0; nu < kCol; nu++) {
                // System.out.println("mju: " + mju);
                // System.err.println("nu: " + nu);
                byte[] sha1Bytes=sha160(K[mju][nu].toString()+gamma1[mju][nu].toString()+ gamma2[mju][nu].toString()+gamma3[mju][nu].toString()+msgSP2.toString());
                Element value=Zr.newElementFromHash(sha1Bytes,0,sha1Bytes.length).getImmutable();
                if (!value.isEqual(c1[mju][nu])) {
                    return false;
                }
                // System.out.println("c1");
                sha1Bytes=sha160(L[mju][nu].toString()+H.toString()+msgSP2.toString());
                value=Zr.newElementFromHash(sha1Bytes,0,sha1Bytes.length).getImmutable();
                if (!value.isEqual(c2[mju][nu])) {
                    return false;
                }
                // System.out.println("c2");
                Element t1=F1.powZn(z1[mju][nu]).mul(J1.powZn(z2[mju][nu])).getImmutable();
                Element t2=K[mju][nu].div(E1.mul(g1.powZn(Zr.newElement(mju))).mul(E2).mul(h1.powZn(Zr.newElement(nu)))).getImmutable();
                value=t1.mul(t2.powZn(c1[mju][nu]));
                if (!value.isEqual(gamma1[mju][nu])) {
                    return false;
                }
                // System.out.println("gamma1");
                
                t1=bp.pairing(C[mju][1],g2).powZn(z1[mju][nu].negate()).getImmutable();
                t2=(bp.pairing(C[mju][1],W2).div(bp.pairing(g2,g2))).powZn(c1[mju][nu]).getImmutable();
                value=t1.mul(t2);
                if (!value.isEqual(gamma2[mju][nu])) {
                    return false;
                }
                // System.out.println("gamma2");
                t1=bp.pairing(D[nu][1],h2).powZn(z2[mju][nu].negate()).getImmutable();
                t2=(bp.pairing(D[nu][1],W2Prime).div(bp.pairing(h2,h2))).powZn(c1[mju][nu]).getImmutable();
                value = t1.mul(t2);
                if (!value.isEqual(gamma3[mju][nu])) {
                    return false;
                }
                // System.out.println("gamma3");
                value=bp.pairing(K[mju][nu],_harr[mju][nu]).mul(L[mju][nu].powZn(c2[mju][nu]));
                if(!value.isEqual(LPrime[mju][nu])){
                    return false;
                }
                // System.out.println("LPrime");
                value=bp.pairing(_g,_harr[mju][nu]).mul(H.powZn(c2[mju][nu]));
                if (!value.isEqual(tildeH)) {
                    return false;
                }
                // System.out.println("tildeH");
            }
        }
        return true;
    }

    public Element[][] computeM(int iPoint,int jPoint,int lRow,int kCol,HashMap<String,Object> PI2){
        Element[][] L= (Element[][]) PI2.get("L");
        Element[][] M=new Element[lRow][kCol];
        for(int mju=0;mju<lRow;mju++){
            for(int nu=0;nu<kCol;nu++){
                Element cd=C[mju][2].powZn(r[2]).mul(D[nu][2].powZn(r[4])).getImmutable();
                Element P=L[mju][nu].div(H.powZn((r[0].add(r[1])).negate()).mul(cd)).getImmutable();
                M[mju][nu]=B[iPoint+mju][jPoint+nu].div(P);
            }
        }
        return M;
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

    public static void main(String[] args) {
        byte[] sha1Bytes=sha160("ly");
        Element value=G1.newElementFromHash(sha1Bytes,0,sha1Bytes.length).getImmutable();
        System.out.println(value);
        System.out.println(bytesToHex(value.toBytes()));
    }
}
