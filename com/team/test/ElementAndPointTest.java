package com.team.test;

import it.unisa.dia.gas.jpbc.Point;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

public class ElementAndPointTest {
    //Element 变成压缩比特 只能压缩G1，G2元素
    public static byte[] ElementtoBytesCompressed(Element element) {
        Point point = (Point) element.duplicate(); // 创建新的Point对象
        byte[] compressed_bytes = point.toBytesCompressed();
        return compressed_bytes;
    }
    
    // 压缩比特变成Element (和elemnt同一类型)
    public static void BytesCompressedToElement(Element element, byte[] compressed_bytes) {
        Point re_point = (Point) element;
        re_point.setFromBytesCompressed(compressed_bytes);
        element = (Element) re_point;
    }
    public static void main(String[] args) {
        // 初始化JPBC
        Pairing bp = PairingFactory.getPairing("a.properties");
        Field G1 = bp.getG1();
        Field Gt = bp.getGT();
        Field Zr = bp.getZr();

        // 1. 创建一个Element
        Element gtelement = Gt.newRandomElement(); // 随机生成一个元素
        Element g1element = G1.newRandomElement();
        Element zrelement = Zr.newRandomElement();
        // System.out.println("Original Element: " + element);

        byte[] gtBytes = gtelement.toBytes();
        byte[] g1Bytes = g1element.toBytes();
        byte[] zrBytes = zrelement.toBytes();
        System.out.println("一个GT元素的字节数: " + gtBytes.length);
        System.out.println("一个G1元素的字节数: " + g1Bytes.length);
        System.out.println("一个zr元素的字节数: " + zrBytes.length);

        // Element re_Element = Gt.newElement(); 
        // ElementAndPointTest.BytesCompressedToElement(re_Element, elementBytes);
        // System.out.println("re_element: " + re_Element);
    }
}
