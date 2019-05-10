package uk.ac.manchester.spinnaker.utils;

public class MeasureMathPerformance {
	static final long ITER = 100000000;

	public static int ceildiv2(int numerator, int denominator) {
		return (numerator / denominator)
				+ (numerator % denominator != 0 ? 1 : 0);
	}

	private static int doNothing(int a, int b) {
		return 0;
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
			total += MathUtils.ceildiv(a, b);
		}
		long total2 = 0;
		long start = System.nanoTime();
		for (long i = 0; i < iter; i++) {
			total2 += MathUtils.ceildiv(a, b);
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

	public static void main(String... strings) {
		int a = 456;
		int b = 123;
		System.out.println("test #0");
		for (long i = 0; i < ITER; i++) {
			test0(a, b, 1);
		}
		System.out.println(test0(a, b, ITER) / (double) ITER);
		System.out.println("test #1");
		for (long i = 0; i < ITER; i++) {
			test1(a, b, 1);
		}
		System.out.println(test1(a, b, ITER) / (double) ITER);
		System.out.println("test #2");
		for (long i = 0; i < ITER; i++) {
			test2(a, b, 1);
		}
		System.out.println(test2(a, b, ITER) / (double) ITER);
	}
}
