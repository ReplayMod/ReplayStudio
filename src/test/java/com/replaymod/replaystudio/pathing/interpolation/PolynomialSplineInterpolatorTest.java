/*
 * Copyright (c) 2021
 *
 * This file is part of ReplayStudio.
 *
 * ReplayStudio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ReplayStudio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReplayStudio.  If not, see <https://www.gnu.org/licenses/>.
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