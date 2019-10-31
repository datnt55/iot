package vmio.com.blemultipleconnect.Utilities;

public class MathUtils {

    public static double[][] multipleMatrix(double[][] firstMatrix ,double[][] secondMatrix  ){
        int r1 = firstMatrix.length , c1 = firstMatrix[0].length;
        int r2 = secondMatrix.length, c2 =  secondMatrix[0].length;
        if (c1 != r2 )
            return null;
        double[][] product = new double[r1][c2];
        for(int i = 0; i < r1; i++) {
            for (int j = 0; j < c2; j++) {
                for (int k = 0; k < c1; k++) {
                    product[i][j] += firstMatrix[i][k] * secondMatrix[k][j];
                }
            }
        }
        return  product;
    }
}
