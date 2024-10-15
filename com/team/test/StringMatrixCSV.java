package com.team.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class StringMatrixCSV {
    /*
     * 生成明文的服务表
     */

    // 生成 m * n 的字符串矩阵
    public static String[][] generateMatrix(int m, int n, String[] elements) {
        Random random = new Random();
        String[][] matrix = new String[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = elements[random.nextInt(elements.length)];
            }
        }
        return matrix;
    }

    // 将矩阵存储为 CSV 文件
    public static void saveMatrixToCSV(String[][] matrix, String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (String[] row : matrix) {
                writer.write(String.join(",", row)); // 以逗号分隔每一行
                writer.newLine(); // 换行
            }
            System.out.println("Matrix saved to CSV file: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 从 CSV 文件中读取矩阵
    public static String[][] loadMatrixFromCSV(String filename) {
        String[][] matrix = null;
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int rows = 0;

            // 先计算行数
            while ((line = reader.readLine()) != null) {
                rows++;
            }

            matrix = new String[rows][];
            reader.close(); // 关闭以重新读取

            // 读取每一行
            try (BufferedReader reader2 = new BufferedReader(new FileReader(filename))) {
                int rowIndex = 0;
                while ((line = reader2.readLine()) != null) {
                    matrix[rowIndex++] = line.split(","); // 以逗号分隔每一行
                }
            }
            System.out.println("Matrix loaded from CSV file: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return matrix;
    }

    // 打印矩阵
    public static void printMatrix(String[][] matrix) {
        for (String[] row : matrix) {
            for (String element : row) {
                System.out.print(element + " ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        String[][] matrix;

        String[] elements = { "Bank", "Restaurant", "Hospital", "School", "Library" };
        int m = 100;
        int n = 100;
        matrix = StringMatrixCSV.generateMatrix(m, n, elements);
        StringMatrixCSV.saveMatrixToCSV(matrix,"ServiceM.csv");
        //明文服务点地图
    
        matrix = StringMatrixCSV.loadMatrixFromCSV("ServiceM.csv");
        StringMatrixCSV.printMatrix(matrix);
    }
}
