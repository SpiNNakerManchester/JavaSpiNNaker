package uk.ac.manchester.spinnaker.messages.model;

enum ARMRegisters {
	r0, r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, sp, lr, pc, apsr;
	int get(int[] registers) {
		return registers[ordinal()];
	}
}