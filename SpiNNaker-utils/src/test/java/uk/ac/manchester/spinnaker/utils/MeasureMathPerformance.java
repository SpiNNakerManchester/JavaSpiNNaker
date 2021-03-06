/*
 * Copyright (c) 2019 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.utils;

import java.util.Arrays;
import java.util.function.IntBinaryOperator;

public class MeasureMathPerformance {
	static final long ITER = 100000000;

	public static int ceildiv1(int numerator, int denominator) {
		return (int) Math.ceil((float) numerator / (float) denominator);
	}

	public static int ceildiv2(int numerator, int denominator) {
		return (numerator / denominator)
				+ (numerator % denominator != 0 ? 1 : 0);
	}

	public static int ceildiv3(int numerator, int denominator) {
		return (denominator - 1 + numerator) / denominator;
	}

	private static int doNothing(int a, int b) {
		return a / b;
	}

	public static long test0(int a, int b, long iter) {
		long total = 0;
		for (long i = 0; i < iter; i++) {
			total += doNothing(a, b);
		}
		long total2 = 0;
		long start = System.nanoTime();
		for (long i = 0; i < iter; i++) {
			total2 += doNothing(a, b);
		}
		long end = System.nanoTime();
		if (total != total2)
			throw new RuntimeException(total + ", " + total2);
		return end - start;
	}

	public static long test1(int a, int b, long iter) {
		long total = 0;
		for (long i = 0; i < iter; i++) {
			total += ceildiv1(a, b);
		}
		long total2 = 0;
		long start = System.nanoTime();
		for (long i = 0; i < iter; i++) {
			total2 += ceildiv1(a, b);
		}
		long end = System.nanoTime();
		if (total != total2)
			throw new RuntimeException(total + ", " + total2);
		return end - start;
	}

	public static long test2(int a, int b, long iter) {
		long total = 0;
		for (long i = 0; i < iter; i++) {
			total += ceildiv2(a, b);
		}
		long total2 = 0;
		long start = System.nanoTime();
		for (long i = 0; i < iter; i++) {
			total2 += ceildiv2(a, b);
		}
		long end = System.nanoTime();
		if (total != total2)
			throw new RuntimeException(total + ", " + total2);
		return end - start;
	}

	public static long test3(int a, int b, long iter) {
		long total = 0;
		for (long i = 0; i < iter; i++) {
			total += ceildiv3(a, b);
		}
		long total2 = 0;
		long start = System.nanoTime();
		for (long i = 0; i < iter; i++) {
			total2 += ceildiv3(a, b);
		}
		long end = System.nanoTime();
		if (total != total2)
			throw new RuntimeException(total + ", " + total2);
		return end - start;
	}

	public static long test4(IntBinaryOperator f, int a, int b, long iter) {
		long total = 0;
		for (long i = 0; i < iter; i++) {
			total += f.applyAsInt(a, b);
		}
		long total2 = 0;
		long start = System.nanoTime();
		for (long i = 0; i < iter; i++) {
			total2 += f.applyAsInt(a, b);
		}
		long end = System.nanoTime();
		if (total != total2)
			throw new RuntimeException(total + ", " + total2);
		return end - start;
	}

	private static void runtest(IntBinaryOperator op) {
		int a = 456;
		int b = 123;
		for (long i = 0; i < ITER; i++) {
			test4(op, a, b, 1);
		}
		System.out.println(test4(op, a, b, ITER) / (double) ITER);
	}

	public static void main(String... strings) {
		int a = 456;
		int b = 123;

		System.out.println("test #0: overhead estimation");
		for (long i = 0; i < ITER; i++) {
			test0(a, b, 1);
		}
		System.out.println(test0(a, b, ITER) / (double) ITER);

		System.out.println("test #1: Math.ceil");
		for (long i = 0; i < ITER; i++) {
			test1(a, b, 1);
		}
		System.out.println(test1(a, b, ITER) / (double) ITER);

		System.out.println("test #2: divmod");
		for (long i = 0; i < ITER; i++) {
			test2(a, b, 1);
		}
		System.out.println(test2(a, b, ITER) / (double) ITER);

		System.out.println("test #3: add-div");
		for (long i = 0; i < ITER; i++) {
			test3(a, b, 1);
		}
		System.out.println(test3(a, b, ITER) / (double) ITER);

		IntBinaryOperator[] ops = {
			MeasureMathPerformance::doNothing,
			MeasureMathPerformance::ceildiv1,
			MeasureMathPerformance::ceildiv2,
			MeasureMathPerformance::ceildiv3
		};
		System.out.println("test #4.0: overhead estimation");
		runtest(ops[0]);

		System.out.println("test #4.1: Math.ceil");
		runtest(ops[1]);

		System.out.println("test #4.2: divmod");
		runtest(ops[2]);

		System.out.println("test #4.3: add-div");
		runtest(ops[3]);
		System.out.println(
				"this is just to force retention of references; ignore please!"
						+ Arrays.toString(ops));
	}
}
