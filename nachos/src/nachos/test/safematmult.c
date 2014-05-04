/*
 * safematmult.c - based on matmult.c, but uses only the exit syscall, and
 * returns 0 on success, 1 on failure.
 */

#include "syscall.h"

/* Try varying this value.  Is your implementation still correct? */
#define DIM 20

int A[DIM][DIM];
int B[DIM][DIM];
int C[DIM][DIM];

int main() {
  int i, j, k;

  /* Initialize the matrices */
  for (i = 0; i < DIM; i++) {
    for (j = 0; j < DIM; j++) {
      A[i][j] = i;
      B[i][j] = j;
      C[i][j] = 0;
    }
  }

  /* Then perform the multiplication */
  for (i = 0; i < DIM; i++) {
    for (j = 0; j < DIM; j++) {
      for (k = 0; k < DIM; k++) {
        C[i][j] += A[i][k] * B[k][j];
      }
    }
  }


  /*
   * The bottom row of A is all DIM-1, the right column of B is all DIM-1, so
   * the bottom-right cell of C should be DIM * (DIM-1) * (DIM-1)
   */
  return C[DIM - 1][DIM - 1] == DIM * (DIM - 1) * (DIM - 1);
}
