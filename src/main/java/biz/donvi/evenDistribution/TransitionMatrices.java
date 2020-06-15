package biz.donvi.evenDistribution;


import static java.lang.Math.*;

final class TransitionMatrices {

    static final double[][] ROTATION_0 = {
            {1, 0},
            {0, 1}};
    static final double[][] ROTATION_90 = {
            {0, -1},
            {1, 0}};
    static final double[][] ROTATION_180 = {
            {-1, 0},
            {0, -1}};
    static final double[][] ROTATION_270 = {
            {0, 1},
            {-1, 0}};

    static final double[][][] ROTATIONS_0_90_180_270 = {
            ROTATION_0,
            ROTATION_90,
            ROTATION_180,
            ROTATION_270};

    static double[][] getSquish(double amount) {
        return new double[][]{
                {1, cos(amount * PI / 2d)},
                {0, sin(amount * PI / 2d)}
        };
    }

    /**
     * Multiplies a matrix and vector together. Useful for transformations of vectors.
     *
     * @param matrix Matrix to start with
     * @param vector Vector to multiply
     * @return The resulting vector
     */
    public static double[] multiplyMatrixVector(double[][] matrix, double[] vector) {
        return new double[]{
                matrix[0][0] * vector[0] + matrix[0][1] * vector[1],
                matrix[1][0] * vector[0] + matrix[1][1] * vector[1]
        };
    }
}
