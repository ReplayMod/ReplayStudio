/*
 * This file is part of ReplayStudio, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 johni0702 <https://github.com/johni0702>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.replaymod.replaystudio.pathing.interpolation;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class PolynomialSplineInterpolatorTest {

    @Test
    public void testSolveMatrix() throws Exception {
        double[][] matrix;
        PolynomialSplineInterpolator.solveMatrix(matrix = new double[][]{
                {1, 0, 0, 1},
                {0, 1, 0, 2},
                {0, 0, 1, 3},
        });
        assertEquals(solvedMatrix(1, 2, 3), matrix);
        PolynomialSplineInterpolator.solveMatrix(matrix = new double[][]{
                {0, 0, 1, 3},
                {0, 1, 0, 2},
                {1, 0, 0, 1},
        });
        assertEquals(solvedMatrix(1, 2, 3), matrix);
        PolynomialSplineInterpolator.solveMatrix(matrix = new double[][]{
                {1, 0, 0, 1},
                {0, 2, 0, 4},
                {0, 0, 3, 9},
        });
        assertEquals(solvedMatrix(1, 2, 3), matrix);
        PolynomialSplineInterpolator.solveMatrix(matrix = new double[][]{
                {3, 3,  4, 1},
                {3, 5,  9, 2},
                {5, 9, 17, 4},
        });
        assertEquals(solvedMatrix(1, -2, 1), matrix);
    }

    private double[][] solvedMatrix(int...results) {
        double[][] matrix = new double[results.length][results.length + 1];
        for (int i = 0; i < results.length; i++) {
            matrix[i][i] = 1;
            matrix[i][results.length] = results[i];
        }
        return matrix;
    }

    private void assertEquals(double[][] expected, double[][] actual) {
        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(expected[i], actual[i], 1.0E-10);
        }
    }

    @Test
    public void testDerivative() throws Exception {
        assertArrayEquals(new double[]{}, new PolynomialSplineInterpolator.Polynomial(
                new double[]{}).derivative().coefficients, Double.MIN_VALUE);
        assertArrayEquals(new double[]{}, new PolynomialSplineInterpolator.Polynomial(
                new double[]{42}).derivative().coefficients, Double.MIN_VALUE);
        assertArrayEquals(new double[]{3, 2, 1}, new PolynomialSplineInterpolator.Polynomial(
                new double[]{1, 1, 1, 1}).derivative().coefficients, Double.MIN_VALUE);
        assertArrayEquals(new double[]{15, 8, 3}, new PolynomialSplineInterpolator.Polynomial(
                new double[]{5, 4, 3, 2}).derivative().coefficients, Double.MIN_VALUE);
        assertArrayEquals(new double[]{0, 0, 0}, new PolynomialSplineInterpolator.Polynomial(
                new double[]{0, 0, 0, 0}).derivative().coefficients, Double.MIN_VALUE);
        assertArrayEquals(new double[]{0, 0, 0}, new PolynomialSplineInterpolator.Polynomial(
                new double[]{0, 0, 0, 1}).derivative().coefficients, Double.MIN_VALUE);
    }
}